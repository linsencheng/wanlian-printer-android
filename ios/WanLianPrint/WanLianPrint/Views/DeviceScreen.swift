import SwiftUI

struct DeviceScreen: View {
    @EnvironmentObject private var bluetooth: BluetoothManager

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                statusHeader
                if let error = bluetooth.lastError {
                    Text(error)
                        .font(.footnote)
                        .foregroundStyle(.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal)
                        .padding(.bottom, 6)
                }
                List(bluetooth.discoveredDevices) { device in
                    deviceRow(device)
                }
                .overlay {
                    if bluetooth.discoveredDevices.isEmpty {
                        VStack(spacing: 10) {
                            Image(systemName: "antenna.radiowaves.left.and.right")
                                .font(.largeTitle)
                            Text("尚未发现 BLE 设备").font(.headline)
                            Text("点击右上角“扫描”，不会按设备名称过滤。")
                                .font(.caption)
                        }
                        .foregroundStyle(.secondary)
                    }
                }
            }
            .navigationTitle("BLE 打印机")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(bluetooth.connectionState == .scanning ? "停止" : "扫描") {
                        if bluetooth.connectionState == .scanning {
                            bluetooth.stopScan()
                        } else {
                            bluetooth.startScan()
                        }
                    }
                }
            }
        }
    }

    private var statusHeader: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Label(bluetooth.connectionState.rawValue, systemImage: statusIcon)
                    .foregroundStyle(bluetooth.isConnected ? Color.green : Color.secondary)
                Spacer()
                Text("CoreBluetooth: \(bluetooth.centralStateText)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Text("当前设备：\(bluetooth.connectedName)")
                .font(.subheadline.weight(.semibold))
            Text(bluetooth.connectedPeripheralUUID)
                .font(.caption.monospaced())
                .foregroundStyle(.secondary)
        }
        .padding()
        .background(Color(uiColor: .secondarySystemBackground))
    }

    private func deviceRow(_ device: DiscoveredBLEDevice) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .firstTextBaseline) {
                Text(device.name)
                    .font(.headline)
                Spacer()
                Text("RSSI \(device.rssi)")
                    .font(.caption.monospacedDigit())
                    .foregroundStyle(.secondary)
            }
            Text(device.id.uuidString)
                .font(.caption.monospaced())
                .foregroundStyle(.secondary)
                .textSelection(.enabled)
            Button {
                if bluetooth.connectedPeripheralUUID == device.id.uuidString,
                   bluetooth.connectionState == .connected {
                    bluetooth.disconnect()
                } else {
                    bluetooth.connect(device)
                }
            } label: {
                Text(buttonTitle(for: device))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .disabled(bluetooth.connectionState == .connecting &&
                bluetooth.connectedPeripheralUUID != device.id.uuidString)
        }
        .padding(.vertical, 4)
    }

    private var statusIcon: String {
        bluetooth.isConnected ? "checkmark.circle.fill" : "circle"
    }

    private func buttonTitle(for device: DiscoveredBLEDevice) -> String {
        guard bluetooth.connectedPeripheralUUID == device.id.uuidString else { return "连接" }
        switch bluetooth.connectionState {
        case .connecting: return "连接中…"
        case .connected: return "断开"
        case .disconnecting: return "断开中…"
        default: return "重新连接"
        }
    }
}
