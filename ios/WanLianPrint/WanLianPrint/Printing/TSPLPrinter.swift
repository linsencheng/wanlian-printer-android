import Foundation

enum TSPLPrinter {
    static let dpi: Double = 203
    static let dotsPerMillimeter = dpi / 25.4

    static func millimetersToDots(_ millimeters: Double) -> Int {
        max(1, Int((millimeters * dotsPerMillimeter).rounded()))
    }

    static func basicTest() -> Data {
        ascii(
            "SIZE 60.0 mm,20.0 mm\r\n" +
            "GAP 0 mm,0 mm\r\n" +
            "DIRECTION 1,0\r\n" +
            "CLS\r\n" +
            "PRINT 1,1\r\n"
        )
    }

    static func rectangleTest() -> Data {
        let widthMm = 60.0
        let heightMm = 45.0
        let mask = PrintMaskRenderer.rectangle(
            widthDots: millimetersToDots(widthMm),
            heightDots: millimetersToDots(heightMm)
        )
        return bitmapJob(mask: mask, widthMm: widthMm, heightMm: heightMm)
    }

    static func englishTest() -> Data {
        let widthMm = 60.0
        let heightMm = 45.0
        let mask = PrintMaskRenderer.centeredText(
            "TEST",
            widthDots: millimetersToDots(widthMm),
            heightDots: millimetersToDots(heightMm),
            fontSizeDots: 96
        )
        return bitmapJob(mask: mask, widthMm: widthMm, heightMm: heightMm)
    }

    static func chineseTest() -> Data {
        let widthMm = 60.0
        let heightMm = 45.0
        let mask = PrintMaskRenderer.centeredText(
            "测试",
            widthDots: millimetersToDots(widthMm),
            heightDots: millimetersToDots(heightMm),
            fontSizeDots: 112
        )
        return bitmapJob(mask: mask, widthMm: widthMm, heightMm: heightMm)
    }

    private static func bitmapJob(mask: PrintMask, widthMm: Double, heightMm: Double) -> Data {
        let packed = mask.packedBytesForXPTT426B()
        var data = ascii(
            "SIZE \(format(widthMm)) mm,\(format(heightMm)) mm\r\n" +
            "GAP 0 mm,0 mm\r\n" +
            "DIRECTION 1,0\r\n" +
            "DENSITY 10\r\n" +
            "SPEED 3.0\r\n" +
            "CLS\r\n" +
            "BITMAP 0,0,\(mask.bytesPerRow),\(mask.height),0,"
        )
        data.append(packed)
        data.append(ascii("\r\nPRINT 1,1\r\n"))
        return data
    }

    private static func ascii(_ value: String) -> Data {
        value.data(using: .ascii) ?? Data()
    }

    private static func format(_ value: Double) -> String {
        String(format: "%.1f", locale: Locale(identifier: "en_US_POSIX"), value)
    }
}
