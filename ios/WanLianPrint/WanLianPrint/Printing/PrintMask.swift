import CoreGraphics
import Foundation
import UIKit

/** Semantic mask: 0 = NOT_PRINT, 1 = PRINT. */
struct PrintMask {
    let width: Int
    let height: Int
    private let printPixels: [UInt8]

    init(width: Int, height: Int, printPixels: [UInt8]) {
        precondition(printPixels.count == width * height)
        self.width = width
        self.height = height
        self.printPixels = printPixels
    }

    var bytesPerRow: Int { (width + 7) / 8 }

    func shouldPrintPixel(x: Int, y: Int) -> Bool {
        printPixels[y * width + x] != 0
    }

    /**
     * Packs logical PRINT pixels MSB-first and then applies the XP-TT426B wire polarity
     * proven by the Android implementation: on this BITMAP path wire 0 heats, wire 1 does not.
     */
    func packedBytesForXPTT426B() -> Data {
        var output = Data(capacity: bytesPerRow * height)
        for y in 0..<height {
            for byteIndex in 0..<bytesPerRow {
                var logicalPrintBits: UInt8 = 0
                for bit in 0..<8 {
                    let x = byteIndex * 8 + bit
                    if x < width && shouldPrintPixel(x: x, y: y) {
                        logicalPrintBits |= UInt8(0x80 >> bit)
                    }
                }
                output.append(logicalPrintBits ^ 0xFF)
            }
        }
        return output
    }
}

enum PrintMaskRenderer {
    static func rectangle(widthDots: Int, heightDots: Int) -> PrintMask {
        render(widthDots: widthDots, heightDots: heightDots) {
            UIColor.white.setFill()
            let margin = CGFloat(min(widthDots, heightDots)) * 0.20
            UIBezierPath(
                rect: CGRect(
                    x: margin,
                    y: margin,
                    width: CGFloat(widthDots) - margin * 2,
                    height: CGFloat(heightDots) - margin * 2
                )
            ).fill()
        }
    }

    static func centeredText(
        _ text: String,
        widthDots: Int,
        heightDots: Int,
        fontSizeDots: CGFloat
    ) -> PrintMask {
        render(widthDots: widthDots, heightDots: heightDots) {
            let attributes: [NSAttributedString.Key: Any] = [
                .font: UIFont.systemFont(ofSize: fontSizeDots, weight: .bold),
                .foregroundColor: UIColor.white,
            ]
            let string = text as NSString
            let size = string.size(withAttributes: attributes)
            string.draw(
                at: CGPoint(
                    x: (CGFloat(widthDots) - size.width) / 2,
                    y: (CGFloat(heightDots) - size.height) / 2
                ),
                withAttributes: attributes
            )
        }
    }

    private static func render(
        widthDots: Int,
        heightDots: Int,
        drawing: () -> Void
    ) -> PrintMask {
        let width = max(1, widthDots)
        let height = max(1, heightDots)
        let bytesPerPixel = 4
        let bytesPerRow = width * bytesPerPixel
        var rgba = [UInt8](repeating: 0, count: bytesPerRow * height)
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        let bitmapInfo = CGBitmapInfo.byteOrder32Big.rawValue |
            CGImageAlphaInfo.premultipliedLast.rawValue

        rgba.withUnsafeMutableBytes { rawBuffer in
            guard let baseAddress = rawBuffer.baseAddress,
                  let context = CGContext(
                    data: baseAddress,
                    width: width,
                    height: height,
                    bitsPerComponent: 8,
                    bytesPerRow: bytesPerRow,
                    space: colorSpace,
                    bitmapInfo: bitmapInfo
                  ) else { return }
            context.setFillColor(UIColor.black.cgColor)
            context.fill(CGRect(x: 0, y: 0, width: width, height: height))
            context.translateBy(x: 0, y: CGFloat(height))
            context.scaleBy(x: 1, y: -1)
            UIGraphicsPushContext(context)
            drawing()
            UIGraphicsPopContext()
        }

        var semanticPixels = [UInt8](repeating: 0, count: width * height)
        for outputY in 0..<height {
            // CGBitmapContext row zero is bottom-up after using UIKit's top-left transform.
            let sourceY = height - 1 - outputY
            for x in 0..<width {
                let source = sourceY * bytesPerRow + x * bytesPerPixel
                let red = Int(rgba[source])
                let green = Int(rgba[source + 1])
                let blue = Int(rgba[source + 2])
                let luminance = (red * 299 + green * 587 + blue * 114) / 1_000
                semanticPixels[outputY * width + x] = luminance >= 128 ? 1 : 0
            }
        }
        return PrintMask(width: width, height: height, printPixels: semanticPixels)
    }
}
