import Foundation
import SwiftUI

struct TestPrintScreen: View {
    @EnvironmentObject private var bluetooth: BluetoothManager

    var body: some View {
        NavigationStack {
            Form {
                Section("BLE 写入通道") {
                    infoRow("当前设备", bluetooth.connectedName)
                    infoRow("状态", bluetooth.connectionState.rawValue)
                    infoRow("Service", bluetooth.selectedWriteServiceUUID)
                    infoRow("Characteristic", bluetooth.selectedWriteCharacteristicUUID)
                    infoRow("Write Mode", bluetooth.selectedWriteMode?.rawValue ?? "N/A")
                    infoRow("Max Write", "\(bluetooth.maximumWriteValueLength) bytes")
                }

                Section("小尺寸 P0 测试") {
                    testButton("TSPL 基础测试", systemImage: "arrow.down.to.line") {
                        bluetooth.send(payload: TSPLPrinter.basicTest(), label: "TSPL 基础测试")
                    }
                    testButton("打印矩形", systemImage: "rectangle.fill") {
                        bluetooth.send(payload: TSPLPrinter.rectangleTest(), label: "打印矩形")
                    }
                    testButton("打印 TEST", systemImage: "textformat") {
                        bluetooth.send(payload: TSPLPrinter.englishTest(), label: "打印 TEST")
                    }
                    testButton("打印 测试", systemImage: "character.book.closed") {
                        bluetooth.send(payload: TSPLPrinter.chineseTest(), label: "打印 测试")
                    }
                    Text("位图测试尺寸为 60 × 45 mm；中文由 iOS CoreGraphics/UIFont 渲染，不使用打印机中文字库。")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                Section("发送进度") {
                    ProgressView(value: bluetooth.sendProgress.fraction)
                    HStack {
                        Text(byteProgress)
                        Spacer()
                        Text(chunkProgress)
                    }
                    .font(.caption.monospacedDigit())
                    Text(bluetooth.sendProgress.resultMessage.isEmpty ? "等待任务" : bluetooth.sendProgress.resultMessage)
                        .foregroundStyle(
                            bluetooth.sendProgress.resultMessage == "发送失败" ? Color.red : Color.secondary
                        )
                }

                if let error = bluetooth.lastError {
                    Section("错误") {
                        Text(error).foregroundStyle(.red)
                    }
                }
            }
            .navigationTitle("测试打印")
        }
    }

    private var byteProgress: String {
        let progress = bluetooth.sendProgress
        return "\(formatBytes(progress.sentBytes)) / \(formatBytes(progress.totalBytes))"
    }

    private var chunkProgress: String {
        let progress = bluetooth.sendProgress
        return "\(progress.sentChunks) / \(progress.totalChunks) chunks"
    }

    private func infoRow(_ label: String, _ value: String) -> some View {
        LabeledContent(label) {
            Text(value)
                .font(.caption.monospaced())
                .multilineTextAlignment(.trailing)
                .textSelection(.enabled)
        }
    }

    private func testButton(
        _ title: String,
        systemImage: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Label(title, systemImage: systemImage)
                .frame(maxWidth: .infinity)
        }
        .disabled(!bluetooth.canPrint)
    }

    private func formatBytes(_ value: Int) -> String {
        if value >= 1_024 {
            return String(format: "%.1f KB", Double(value) / 1_024)
        }
        return "\(value) B"
    }
}
