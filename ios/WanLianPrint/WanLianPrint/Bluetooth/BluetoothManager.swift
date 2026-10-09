import Combine
import CoreBluetooth
import Foundation

final class BluetoothManager: NSObject, ObservableObject {
    let logger: AppLogger

    @Published private(set) var centralStateText = "UNKNOWN"
    @Published private(set) var connectionState: PrinterConnectionState = .disconnected
    @Published private(set) var discoveredDevices: [DiscoveredBLEDevice] = []
    @Published private(set) var connectedName = "未连接"
    @Published private(set) var connectedPeripheralUUID = "N/A"
    @Published private(set) var connectedRSSI: Int?
    @Published private(set) var services: [BLEServiceDiagnostic] = []
    @Published private(set) var selectedWriteServiceUUID = "N/A"
    @Published private(set) var selectedWriteCharacteristicUUID = "N/A"
    @Published private(set) var selectedWriteMode: BLEWriteMode?
    @Published private(set) var maximumWriteValueLength = 0
    @Published private(set) var sendProgress = BLESendProgress()
    @Published private(set) var lastError: String?

    private var centralManager: CBCentralManager!
    private var activePeripheral: CBPeripheral?
    private var selectedWriteCharacteristic: CBCharacteristic?
    private let writeQueue: BLEWriteQueue

    override init() {
        let logger = AppLogger()
        self.logger = logger
        self.writeQueue = BLEWriteQueue(logger: logger)
        super.init()
        centralManager = CBCentralManager(delegate: self, queue: nil)
    }

    var isConnected: Bool {
        connectionState == .connected && activePeripheral?.state == .connected
    }

    var canPrint: Bool {
        isConnected && selectedWriteCharacteristic != nil && selectedWriteMode != nil && !sendProgress.isSending
    }

    func startScan() {
        guard centralManager.state == .poweredOn else {
            let message = "蓝牙不可扫描：CBCentralManager=\(centralManager.state.diagnosticName)"
            lastError = message
            logger.log(message)
            return
        }
        discoveredDevices.removeAll()
        connectionState = .scanning
        lastError = nil
        logger.log("开始扫描所有附近 BLE Peripheral（不按名称过滤）")
        centralManager.scanForPeripherals(
            withServices: nil,
            options: [CBCentralManagerScanOptionAllowDuplicatesKey: false]
        )
    }

    func stopScan() {
        guard centralManager.isScanning else { return }
        centralManager.stopScan()
        if connectionState == .scanning { connectionState = .disconnected }
        logger.log("停止 BLE 扫描")
    }

    func connect(_ device: DiscoveredBLEDevice) {
        stopScan()
        if let activePeripheral, activePeripheral.identifier != device.id {
            centralManager.cancelPeripheralConnection(activePeripheral)
        }
        resetConnectionDetails()
        activePeripheral = device.peripheral
        connectedName = device.name
        connectedPeripheralUUID = device.id.uuidString
        connectedRSSI = device.rssi
        device.peripheral.delegate = self
        connectionState = .connecting
        lastError = nil
        logger.log("开始连接: name=\(device.name), UUID=\(device.id.uuidString), RSSI=\(device.rssi)")
        centralManager.connect(device.peripheral, options: nil)
    }

    func disconnect() {
        guard let activePeripheral else { return }
        connectionState = .disconnecting
        writeQueue.cancel(reason: "用户断开设备")
        logger.log("请求断开: \(activePeripheral.identifier.uuidString)")
        centralManager.cancelPeripheralConnection(activePeripheral)
    }

    func send(payload: Data, label: String) {
        guard let peripheral = activePeripheral,
              peripheral.state == .connected,
              let characteristic = selectedWriteCharacteristic,
              let mode = selectedWriteMode else {
            let message = "无法开始打印：尚未连接或没有 writable characteristic"
            lastError = message
            logger.log(message)
            return
        }
        sendProgress = BLESendProgress(
            isSending: true,
            label: label,
            sentBytes: 0,
            totalBytes: payload.count,
            sentChunks: 0,
            totalChunks: 0,
            resultMessage: "发送中"
        )
        lastError = nil
        logger.log("打印任务开始: \(label), payload 总大小=\(payload.count) bytes")
        writeQueue.enqueue(
            payload: payload,
            peripheral: peripheral,
            characteristic: characteristic,
            mode: mode,
            progress: { [weak self] sentBytes, totalBytes, sentChunks, totalChunks in
                self?.sendProgress = BLESendProgress(
                    isSending: true,
                    label: label,
                    sentBytes: sentBytes,
                    totalBytes: totalBytes,
                    sentChunks: sentChunks,
                    totalChunks: totalChunks,
                    resultMessage: "发送中"
                )
            },
            completion: { [weak self] result in
                guard let self else { return }
                switch result {
                case .success:
                    self.sendProgress.isSending = false
                    self.sendProgress.resultMessage = "发送完成"
                    self.logger.log("打印任务完成: \(label)")
                case .failure(let error):
                    self.sendProgress.isSending = false
                    self.sendProgress.resultMessage = "发送失败"
                    self.lastError = error.localizedDescription
                    self.logger.log("打印任务中断: \(label), error=\(error.localizedDescription)")
                }
            }
        )
    }

    private func resetConnectionDetails() {
        services = []
        selectedWriteServiceUUID = "N/A"
        selectedWriteCharacteristicUUID = "N/A"
        selectedWriteMode = nil
        selectedWriteCharacteristic = nil
        maximumWriteValueLength = 0
        sendProgress = BLESendProgress()
    }

    private func considerWriteCharacteristic(_ characteristic: CBCharacteristic) {
        let properties = characteristic.properties
        let candidateMode: BLEWriteMode?
        if properties.contains(.writeWithoutResponse) {
            candidateMode = .withoutResponse
        } else if properties.contains(.write) {
            candidateMode = .withResponse
        } else {
            candidateMode = nil
        }
        guard let candidateMode else { return }

        let shouldReplace = selectedWriteCharacteristic == nil ||
            (selectedWriteMode == .withResponse && candidateMode == .withoutResponse)
        guard shouldReplace, let peripheral = activePeripheral else { return }

        selectedWriteCharacteristic = characteristic
        selectedWriteMode = candidateMode
        selectedWriteServiceUUID = characteristic.service?.uuid.uuidString ?? "N/A"
        selectedWriteCharacteristicUUID = characteristic.uuid.uuidString
        maximumWriteValueLength = peripheral.maximumWriteValueLength(
            for: candidateMode.characteristicWriteType
        )
        logger.log("最终选中的 write characteristic")
        logger.log("Selected Write Service: \(selectedWriteServiceUUID)")
        logger.log("Selected Write Characteristic: \(selectedWriteCharacteristicUUID)")
        logger.log("Write Mode: \(candidateMode.rawValue)")
        logger.log("maximumWriteValueLength: \(maximumWriteValueLength) bytes")
        refreshSelectionMarkers()
    }

    private func refreshSelectionMarkers() {
        services = services.map { service in
            BLEServiceDiagnostic(
                id: service.id,
                uuid: service.uuid,
                characteristics: service.characteristics.map { characteristic in
                    BLECharacteristicDiagnostic(
                        id: characteristic.id,
                        uuid: characteristic.uuid,
                        properties: characteristic.properties,
                        isSelectedWriteCharacteristic: service.uuid == selectedWriteServiceUUID &&
                            characteristic.uuid == selectedWriteCharacteristicUUID
                    )
                },
                isSelectedWriteService: service.uuid == selectedWriteServiceUUID
            )
        }
    }
}

extension BluetoothManager: CBCentralManagerDelegate {
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        centralStateText = central.state.diagnosticName
        logger.log("CBCentralManager state: \(central.state.diagnosticName)")
        if central.state != .poweredOn {
            stopScan()
            if central.state == .poweredOff {
                lastError = "系统蓝牙未开启"
            } else if central.state == .unauthorized {
                lastError = "蓝牙权限未授权"
            }
        }
    }

    func centralManager(
        _ central: CBCentralManager,
        didDiscover peripheral: CBPeripheral,
        advertisementData: [String: Any],
        rssi RSSI: NSNumber
    ) {
        let advertisedName = advertisementData[CBAdvertisementDataLocalNameKey] as? String
        let name = advertisedName ?? peripheral.name ?? "未命名设备"
        let device = DiscoveredBLEDevice(peripheral: peripheral, name: name, rssi: RSSI.intValue)
        if let index = discoveredDevices.firstIndex(where: { $0.id == device.id }) {
            discoveredDevices[index] = device
        } else {
            discoveredDevices.append(device)
            discoveredDevices.sort { $0.rssi > $1.rssi }
        }
        if activePeripheral?.identifier == peripheral.identifier {
            connectedRSSI = RSSI.intValue
        }
        logger.log("发现设备: name=\(name), UUID=\(peripheral.identifier.uuidString), RSSI=\(RSSI)")
    }

    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        connectionState = .connected
        connectedName = peripheral.name ?? connectedName
        connectedPeripheralUUID = peripheral.identifier.uuidString
        logger.log("连接成功: name=\(connectedName), UUID=\(connectedPeripheralUUID)")
        peripheral.delegate = self
        logger.log("Service discovery: discoverServices(nil)")
        peripheral.discoverServices(nil)
    }

    func centralManager(
        _ central: CBCentralManager,
        didFailToConnect peripheral: CBPeripheral,
        error: Error?
    ) {
        connectionState = .failed
        let reason = error?.localizedDescription ?? "unknown"
        lastError = reason
        logger.log("连接失败: UUID=\(peripheral.identifier.uuidString), error=\(reason)")
    }

    func centralManager(
        _ central: CBCentralManager,
        didDisconnectPeripheral peripheral: CBPeripheral,
        error: Error?
    ) {
        writeQueue.cancel(reason: error?.localizedDescription ?? "设备断开")
        connectionState = .disconnected
        let reason = error?.localizedDescription ?? "normal"
        logger.log("设备断开: UUID=\(peripheral.identifier.uuidString), reason=\(reason)")
        resetConnectionDetails()
        activePeripheral = nil
    }
}

extension BluetoothManager: CBPeripheralDelegate {
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        if let error {
            lastError = error.localizedDescription
            logger.log("Service discovery error: \(error.localizedDescription)")
            return
        }
        let discoveredServices = peripheral.services ?? []
        services = discoveredServices.map { service in
            logger.log("Service discovery: UUID=\(service.uuid.uuidString)")
            return BLEServiceDiagnostic(
                id: service.uuid.uuidString,
                uuid: service.uuid.uuidString,
                characteristics: [],
                isSelectedWriteService: false
            )
        }
        for service in discoveredServices {
            logger.log("Characteristic discovery start: service=\(service.uuid.uuidString)")
            peripheral.discoverCharacteristics(nil, for: service)
        }
    }

    func peripheral(
        _ peripheral: CBPeripheral,
        didDiscoverCharacteristicsFor service: CBService,
        error: Error?
    ) {
        if let error {
            lastError = error.localizedDescription
            logger.log("Characteristic discovery error: service=\(service.uuid.uuidString), error=\(error.localizedDescription)")
            return
        }
        let characteristics = service.characteristics ?? []
        let diagnostics = characteristics.map { characteristic in
            let properties = characteristic.properties.diagnosticNames
            logger.log("Characteristic discovery: service=\(service.uuid.uuidString), UUID=\(characteristic.uuid.uuidString), properties=\(properties.joined(separator: ", "))")
            considerWriteCharacteristic(characteristic)
            return BLECharacteristicDiagnostic(
                id: "\(service.uuid.uuidString)/\(characteristic.uuid.uuidString)",
                uuid: characteristic.uuid.uuidString,
                properties: properties,
                isSelectedWriteCharacteristic: false
            )
        }
        if let index = services.firstIndex(where: { $0.uuid == service.uuid.uuidString }) {
            services[index].characteristics = diagnostics
        }
        refreshSelectionMarkers()
    }

    func peripheral(
        _ peripheral: CBPeripheral,
        didWriteValueFor characteristic: CBCharacteristic,
        error: Error?
    ) {
        writeQueue.didWriteValue(for: characteristic, error: error)
    }

    func peripheralIsReady(toSendWriteWithoutResponse peripheral: CBPeripheral) {
        writeQueue.peripheralIsReady(peripheral)
    }
}
