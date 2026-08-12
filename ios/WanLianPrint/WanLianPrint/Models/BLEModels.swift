import CoreBluetooth
import Foundation

enum PrinterConnectionState: String {
    case disconnected = "未连接"
    case scanning = "扫描中"
    case connecting = "连接中"
    case connected = "已连接"
    case disconnecting = "断开中"
    case failed = "连接失败"
}

enum BLEWriteMode: String {
    case withoutResponse = "Without Response"
    case withResponse = "With Response"

    var characteristicWriteType: CBCharacteristicWriteType {
        switch self {
        case .withoutResponse: return .withoutResponse
        case .withResponse: return .withResponse
        }
    }
}

struct DiscoveredBLEDevice: Identifiable {
    let peripheral: CBPeripheral
    var name: String
    var rssi: Int

    var id: UUID { peripheral.identifier }
}

struct BLECharacteristicDiagnostic: Identifiable {
    let id: String
    let uuid: String
    let properties: [String]
    let isSelectedWriteCharacteristic: Bool
}

struct BLEServiceDiagnostic: Identifiable {
    let id: String
    let uuid: String
    var characteristics: [BLECharacteristicDiagnostic]
    var isSelectedWriteService: Bool
}

struct BLESendProgress {
    var isSending = false
    var label = ""
    var sentBytes = 0
    var totalBytes = 0
    var sentChunks = 0
    var totalChunks = 0
    var resultMessage = ""

    var fraction: Double {
        guard totalBytes > 0 else { return 0 }
        return min(1, Double(sentBytes) / Double(totalBytes))
    }
}

extension CBCharacteristicProperties {
    var diagnosticNames: [String] {
        var names: [String] = []
        if contains(.read) { names.append("READ") }
        if contains(.write) { names.append("WRITE") }
        if contains(.writeWithoutResponse) { names.append("WRITE WITHOUT RESPONSE") }
        if contains(.notify) { names.append("NOTIFY") }
        if contains(.indicate) { names.append("INDICATE") }
        return names.isEmpty ? ["NONE"] : names
    }
}

extension CBManagerState {
    var diagnosticName: String {
        switch self {
        case .unknown: return "UNKNOWN"
        case .resetting: return "RESETTING"
        case .unsupported: return "UNSUPPORTED"
        case .unauthorized: return "UNAUTHORIZED"
        case .poweredOff: return "POWERED OFF"
        case .poweredOn: return "POWERED ON"
        @unknown default: return "FUTURE STATE"
        }
    }
}
