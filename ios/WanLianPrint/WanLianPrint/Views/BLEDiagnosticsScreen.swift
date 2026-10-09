import SwiftUI
import UniformTypeIdentifiers
import UIKit

struct BLEDiagnosticsScreen: View {
    @EnvironmentObject private var bluetooth: BluetoothManager
    @EnvironmentObject private var logger: AppLogger
    @State private var isExporting = false
    @State private var exportDocument = LogFileDocument(text: "")

    var body: some View {
        NavigationStack {
            List {
                Section("当前连接") {
                    diagnosticRow("设备名称", bluetooth.connectedName)
                    diagnosticRow("Peripheral UUID", bluetooth.connectedPeripheralUUID)
                    diagnosticRow("RSSI", bluetooth.connectedRSSI.map(String.init) ?? "N/A")
                    diagnosticRow("连接状态", bluetooth.connectionState.rawValue)
                    diagnosticRow("Selected Write Service", bluetooth.selectedWriteServiceUUID)
                    diagnosticRow("Selected Write Characteristic", bluetooth.selectedWriteCharacteristicUUID)
                    diagnosticRow("Write Mode", bluetooth.selectedWriteMode?.rawValue ?? "N/A")
                    diagnosticRow("maximumWriteValueLength", "\(bluetooth.maximumWriteValueLength) bytes")
                }

                Section("发现的 Services / Characteristics") {
                    if bluetooth.services.isEmpty {
                        Text("连接设备后自动 discoverServices(nil) 和 discoverCharacteristics(nil)。")
                            .foregroundStyle(.secondary)
                    }
                    ForEach(bluetooth.services) { service in
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text("Service \(service.uuid)")
                                    .font(.subheadline.monospaced().weight(.semibold))
                                    .textSelection(.enabled)
                                if service.isSelectedWriteService {
                                    Text("SELECTED WRITE SERVICE")
                                        .font(.caption2.weight(.bold))
                                        .foregroundStyle(.green)
                                }
                            }
                            ForEach(service.characteristics) { characteristic in
                                VStack(alignment: .leading, spacing: 4) {
                                    HStack {
                                        Text(characteristic.uuid)
                                            .font(.caption.monospaced())
                                            .textSelection(.enabled)
                                        if characteristic.isSelectedWriteCharacteristic {
                                            Image(systemName: "checkmark.circle.fill")
                                                .foregroundStyle(.green)
                                        }
                                    }
                                    Text(characteristic.properties.joined(separator: " · "))
                                        .font(.caption2)
                                        .foregroundStyle(.secondary)
                                }
                                .padding(.leading, 12)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                }

                Section {
                    HStack {
                        Button("复制日志") {
                            UIPasteboard.general.string = logger.exportedText
                        }
                        Spacer()
                        Button("清空日志", role: .destructive) {
                            logger.clear()
                        }
                        Spacer()
                        Button("导出日志") {
                            exportDocument = LogFileDocument(text: logger.exportedText)
                            isExporting = true
                        }
                    }

                    ScrollViewReader { proxy in
                        ScrollView {
                            LazyVStack(alignment: .leading, spacing: 3) {
                                ForEach(logger.entries) { entry in
                                    Text(logger.formatted(entry))
                                        .font(.caption2.monospaced())
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        .textSelection(.enabled)
                                        .id(entry.id)
                                }
                            }
                        }
                        .frame(minHeight: 260)
                        .onChange(of: logger.entries.count) { _ in
                            if let last = logger.entries.last {
                                proxy.scrollTo(last.id, anchor: .bottom)
                            }
                        }
                    }
                } header: {
                    Text("应用内日志（最近 \(logger.entries.count) / 1000 条）")
                }
            }
            .navigationTitle("BLE Diagnostics")
            .fileExporter(
                isPresented: $isExporting,
                document: exportDocument,
                contentType: .plainText,
                defaultFilename: "WanLianPrint-BLE.log"
            ) { result in
                switch result {
                case .success(let url): logger.log("日志已导出: \(url.lastPathComponent)")
                case .failure(let error): logger.log("日志导出失败: \(error.localizedDescription)")
                }
            }
        }
    }

    private func diagnosticRow(_ label: String, _ value: String) -> some View {
        LabeledContent(label) {
            Text(value)
                .font(.caption.monospaced())
                .multilineTextAlignment(.trailing)
                .textSelection(.enabled)
        }
    }
}

struct LogFileDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.plainText] }
    var text: String

    init(text: String) {
        self.text = text
    }

    init(configuration: ReadConfiguration) throws {
        guard let data = configuration.file.regularFileContents,
              let text = String(data: data, encoding: .utf8) else {
            throw CocoaError(.fileReadCorruptFile)
        }
        self.text = text
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: Data(text.utf8))
    }
}
