import CoreBluetooth
import Foundation

enum BLEWriteQueueError: LocalizedError {
    case busy
    case invalidChunkSize
    case disconnected(String)
    case writeFailed(String)

    var errorDescription: String? {
        switch self {
        case .busy: return "已有 BLE 发送任务正在执行"
        case .invalidChunkSize: return "设备返回的 maximumWriteValueLength 无效"
        case .disconnected(let reason): return "BLE 连接已断开：\(reason)"
        case .writeFailed(let reason): return "BLE 写入失败：\(reason)"
        }
    }
}

/**
 * Serial BLE writer with CoreBluetooth back-pressure support.
 *
 * Without-response writes stop immediately when canSendWriteWithoutResponse becomes false
 * and resume only from peripheralIsReady(toSendWriteWithoutResponse:). With-response writes
 * send exactly one chunk and wait for didWriteValueFor before advancing.
 */
final class BLEWriteQueue {
    typealias ProgressHandler = (_ sentBytes: Int, _ totalBytes: Int, _ sentChunks: Int, _ totalChunks: Int) -> Void
    typealias CompletionHandler = (Result<Void, Error>) -> Void

    private let logger: AppLogger
    private weak var peripheral: CBPeripheral?
    private var characteristic: CBCharacteristic?
    private var mode: BLEWriteMode = .withoutResponse
    private var payload = Data()
    private var offset = 0
    private var chunkSize = 0
    private var totalChunks = 0
    private var pendingResponseBytes = 0
    private var waitingForResponse = false
    private var progressHandler: ProgressHandler?
    private var completionHandler: CompletionHandler?

    private(set) var isSending = false

    init(logger: AppLogger) {
        self.logger = logger
    }

    func enqueue(
        payload: Data,
        peripheral: CBPeripheral,
        characteristic: CBCharacteristic,
        mode: BLEWriteMode,
        progress: @escaping ProgressHandler,
        completion: @escaping CompletionHandler
    ) {
        guard !isSending else {
            completion(.failure(BLEWriteQueueError.busy))
            return
        }
        let maximum = peripheral.maximumWriteValueLength(for: mode.characteristicWriteType)
        guard maximum > 0 else {
            completion(.failure(BLEWriteQueueError.invalidChunkSize))
            return
        }

        self.peripheral = peripheral
        self.characteristic = characteristic
        self.mode = mode
        self.payload = payload
        self.offset = 0
        self.chunkSize = maximum
        self.totalChunks = max(1, Int(ceil(Double(payload.count) / Double(maximum))))
        self.pendingResponseBytes = 0
        self.waitingForResponse = false
        self.progressHandler = progress
        self.completionHandler = completion
        self.isSending = true

        logger.log("BLE write queue start: payload=\(payload.count) bytes, chunk=\(maximum), chunks=\(totalChunks), mode=\(mode.rawValue)")
        progress(0, payload.count, 0, totalChunks)
        pump()
    }

    func peripheralIsReady(_ peripheral: CBPeripheral) {
        guard isSending,
              mode == .withoutResponse,
              let queuedPeripheral = self.peripheral,
              queuedPeripheral === peripheral else { return }
        logger.log("peripheralIsReady: resume write-without-response queue")
        pump()
    }

    func didWriteValue(
        for characteristic: CBCharacteristic,
        error: Error?
    ) {
        guard isSending,
              mode == .withResponse,
              let queuedCharacteristic = self.characteristic,
              queuedCharacteristic === characteristic else { return }
        if let error {
            logger.log("write error: \(error.localizedDescription)")
            finish(.failure(BLEWriteQueueError.writeFailed(error.localizedDescription)))
            return
        }
        guard waitingForResponse else { return }
        offset += pendingResponseBytes
        pendingResponseBytes = 0
        waitingForResponse = false
        reportProgress()
        pump()
    }

    func cancel(reason: String) {
        guard isSending else { return }
        logger.log("BLE write queue interrupted: \(reason)")
        finish(.failure(BLEWriteQueueError.disconnected(reason)))
    }

    private func pump() {
        guard isSending, let peripheral, let characteristic else {
            finish(.failure(BLEWriteQueueError.disconnected("peripheral or characteristic unavailable")))
            return
        }
        guard offset < payload.count else {
            finish(.success(()))
            return
        }

        switch mode {
        case .withoutResponse:
            while offset < payload.count && peripheral.canSendWriteWithoutResponse {
                let end = min(offset + chunkSize, payload.count)
                let chunk = payload.subdata(in: offset..<end)
                peripheral.writeValue(chunk, for: characteristic, type: .withoutResponse)
                offset = end
                reportProgress()
            }
            if offset >= payload.count {
                finish(.success(()))
            } else {
                logger.log("BLE buffer full at \(offset)/\(payload.count) bytes; waiting for peripheralIsReady")
            }

        case .withResponse:
            guard !waitingForResponse else { return }
            let end = min(offset + chunkSize, payload.count)
            let chunk = payload.subdata(in: offset..<end)
            pendingResponseBytes = chunk.count
            waitingForResponse = true
            let chunkIndex = offset / chunkSize + 1
            logger.log("write chunk \(chunkIndex)/\(totalChunks), bytes=\(chunk.count), awaiting response")
            peripheral.writeValue(chunk, for: characteristic, type: .withResponse)
        }
    }

    private func reportProgress() {
        let completedChunks = min(totalChunks, Int(ceil(Double(offset) / Double(chunkSize))))
        logger.log("chunk \(completedChunks)/\(totalChunks), sent=\(offset)/\(payload.count) bytes")
        progressHandler?(offset, payload.count, completedChunks, totalChunks)
    }

    private func finish(_ result: Result<Void, Error>) {
        guard isSending else { return }
        let completion = completionHandler
        switch result {
        case .success:
            logger.log("BLE write queue complete: \(payload.count) bytes")
        case .failure(let error):
            logger.log("BLE write queue failed: \(error.localizedDescription)")
        }
        isSending = false
        peripheral = nil
        characteristic = nil
        payload = Data()
        offset = 0
        pendingResponseBytes = 0
        waitingForResponse = false
        progressHandler = nil
        completionHandler = nil
        completion?(result)
    }
}
