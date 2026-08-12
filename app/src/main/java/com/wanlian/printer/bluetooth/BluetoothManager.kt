package com.wanlian.printer.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.wanlian.printer.BuildConfig
import com.wanlian.printer.model.BluetoothTransport
import com.wanlian.printer.model.ConnectionStatus
import com.wanlian.printer.model.PrinterConnectionInfo
import com.wanlian.printer.model.PrinterDevice
import com.wanlian.printer.storage.DiagnosticLogRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.util.UUID

/** Bluetooth Classic SPP + BLE transport manager. */
@SuppressLint("MissingPermission")
class BluetoothManager(
    context: Context,
    private val diagnostics: DiagnosticLogRepository? = null,
) {
    private val appContext = context.applicationContext
    private val androidBluetoothManager =
        appContext.getSystemService(AndroidBluetoothManager::class.java)
    private val adapter: BluetoothAdapter? get() = androidBluetoothManager?.adapter
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectionMutex = Mutex()
    private val writeMutex = Mutex()

    private val _devices = MutableStateFlow<List<PrinterDevice>>(emptyList())
    val devices: StateFlow<List<PrinterDevice>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _currentDevice = MutableStateFlow<PrinterDevice?>(null)
    val currentDevice: StateFlow<PrinterDevice?> = _currentDevice.asStateFlow()

    private val _connectionInfo = MutableStateFlow<PrinterConnectionInfo?>(null)
    val connectionInfo: StateFlow<PrinterConnectionInfo?> = _connectionInfo.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    val isConnected: Boolean get() = _connectionStatus.value == ConnectionStatus.CONNECTED
    val isBluetoothAvailable: Boolean get() = adapter != null
    val isBluetoothEnabled: Boolean get() = adapter?.isEnabled == true

    private var scanJob: Job? = null
    private var classicSocket: BluetoothSocket? = null
    private var bleEndpoint: BleEndpoint? = null
    private var pendingBleConnection: CompletableDeferred<BleEndpoint>? = null
    private var pendingBleWrite: CompletableDeferred<Int>? = null
    private var negotiatedMtu = DEFAULT_BLE_MTU
    private var intentionalDisconnect = false

    fun clearError() {
        _lastError.value = null
    }

    fun reportError(message: String) {
        diagnostics?.append("APP_ERROR", message)
        _lastError.value = message
    }

    fun startScan() {
        if (!isBluetoothAvailable) {
            reportError("此设备不支持蓝牙")
            return
        }
        if (!isBluetoothEnabled) {
            reportError("请先开启蓝牙")
            return
        }
        if (!hasScanPermission()) {
            reportError("缺少附近设备权限，无法搜索打印机")
            return
        }

        stopScan()
        _devices.value = emptyList()
        _isScanning.value = true

        adapter?.bondedDevices.orEmpty().forEach { device ->
            mergeDevice(
                name = safeName(device),
                address = device.address,
                transport = BluetoothTransport.CLASSIC,
                bonded = true,
            )
        }

        val classicStarted = adapter?.startDiscovery() == true
        val scanner = adapter?.bluetoothLeScanner
        val bleStarted = try {
            scanner?.startScan(bleScanCallback)
            scanner != null
        } catch (error: Exception) {
            false
        }
        if (!classicStarted && !bleStarted) {
            _isScanning.value = false
            reportError("无法启动蓝牙搜索，请关闭后重新开启蓝牙")
            return
        }

        scanJob = scope.launch {
            delay(SCAN_DURATION_MS)
            stopScan()
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        if (hasScanPermission()) {
            runCatching { adapter?.cancelDiscovery() }
            runCatching { adapter?.bluetoothLeScanner?.stopScan(bleScanCallback) }
        }
        _isScanning.value = false
    }

    suspend fun connect(printer: PrinterDevice) = connectionMutex.withLock {
        diagnostics?.append(
            "BT_CONNECT",
            "开始连接 device=${printer.displayName}, address=${maskAddress(printer.address)}, " +
                "candidates=${printer.transportLabel}",
        )
        if (!hasConnectPermission()) {
            reportError("缺少蓝牙连接权限")
            return@withLock
        }
        stopScan()
        disconnectActiveConnection(updateState = false)
        intentionalDisconnect = false
        _currentDevice.value = printer
        _connectionStatus.value = ConnectionStatus.CONNECTING
        clearError()

        var lastFailure: Throwable? = null
        if (BluetoothTransport.CLASSIC in printer.transports) {
            try {
                classicSocket = connectClassic(printer)
                markConnected(
                    printer = printer,
                    info = PrinterConnectionInfo(transport = BluetoothTransport.CLASSIC),
                )
                return@withLock
            } catch (error: Throwable) {
                diagnostics?.append("BT_CONNECT", "SPP 连接失败", error)
                lastFailure = error
                classicSocket = null
            }
        }
        if (BluetoothTransport.BLE in printer.transports) {
            try {
                bleEndpoint = connectBle(printer)
                val characteristic = checkNotNull(bleEndpoint).characteristic
                val properties = characteristic.properties
                markConnected(
                    printer = printer,
                    info = PrinterConnectionInfo(
                        transport = BluetoothTransport.BLE,
                        serviceUuid = characteristic.service?.uuid?.toString(),
                        characteristicUuid = characteristic.uuid.toString(),
                        supportsWrite = properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0,
                        supportsWriteWithoutResponse = properties and
                            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0,
                    ),
                )
                return@withLock
            } catch (error: Throwable) {
                diagnostics?.append("BT_CONNECT", "BLE 连接失败", error)
                lastFailure = error
                bleEndpoint = null
            }
        }

        _connectionStatus.value = ConnectionStatus.ERROR
        diagnostics?.append("BT_CONNECT", "所有连接通道均失败", lastFailure)
        reportError("连接 ${printer.displayName} 失败：${lastFailure?.message ?: "没有可用的打印通道"}")
        disconnectActiveConnection(updateState = false)
    }

    suspend fun reconnect() {
        val printer = _currentDevice.value
        if (printer == null) {
            reportError("没有可重新连接的打印机")
        } else {
            connect(printer)
        }
    }

    suspend fun disconnect() = connectionMutex.withLock {
        intentionalDisconnect = true
        _connectionStatus.value = ConnectionStatus.DISCONNECTING
        disconnectActiveConnection(updateState = true)
    }

    suspend fun write(bytes: ByteArray) = writeMutex.withLock {
        check(isConnected) { "打印机连接已断开" }
        try {
            val socket = classicSocket
            val endpoint = bleEndpoint
            when {
                socket != null -> withContext(Dispatchers.IO) {
                    socket.outputStream.write(bytes)
                    socket.outputStream.flush()
                }
                endpoint != null -> writeBle(endpoint, bytes)
                else -> error("没有可用的蓝牙输出通道")
            }
        } catch (error: Throwable) {
            val transport = _connectionInfo.value?.transport?.connectionLabel ?: "unknown"
            diagnostics?.append(
                "BT_WRITE_FAILED",
                "transport=$transport, requestedBytes=${bytes.size}, connected=$isConnected",
                error,
            )
            val reason = error.message ?: error::class.java.simpleName
            handleConnectionLost("发送数据失败：$reason")
            throw IOException("蓝牙发送 ${bytes.size} 字节失败：$reason", error)
        }
    }

    fun close() {
        stopScan()
        intentionalDisconnect = true
        disconnectActiveConnection(updateState = false)
        runCatching { appContext.unregisterReceiver(receiver) }
        scope.coroutineContext[Job]?.cancel()
    }

    private suspend fun connectClassic(printer: PrinterDevice): BluetoothSocket =
        withContext(Dispatchers.IO) {
            val remote = adapter?.getRemoteDevice(printer.address)
                ?: error("找不到蓝牙适配器")
            adapter?.cancelDiscovery()

            var firstError: Throwable? = null
            val secureSocket = remote.createRfcommSocketToServiceRecord(SPP_UUID)
            try {
                secureSocket.connect()
                return@withContext secureSocket
            } catch (error: Throwable) {
                firstError = error
                runCatching { secureSocket.close() }
            }

            val insecureSocket = remote.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
            try {
                insecureSocket.connect()
                insecureSocket
            } catch (error: Throwable) {
                runCatching { insecureSocket.close() }
                throw IOException(error.message ?: firstError?.message ?: "SPP 连接失败", error)
            }
        }

    private suspend fun connectBle(printer: PrinterDevice): BleEndpoint {
        val remote = adapter?.getRemoteDevice(printer.address)
            ?: error("找不到蓝牙适配器")
        negotiatedMtu = DEFAULT_BLE_MTU
        val deferred = CompletableDeferred<BleEndpoint>()
        pendingBleConnection = deferred
        val gatt = remote.connectGatt(
            appContext,
            false,
            gattCallback,
            BluetoothDevice.TRANSPORT_LE,
        ) ?: error("无法创建 BLE GATT 连接")

        return try {
            withTimeout(CONNECTION_TIMEOUT_MS) { deferred.await() }
        } catch (error: Throwable) {
            runCatching { gatt.disconnect() }
            runCatching { gatt.close() }
            throw error
        } finally {
            pendingBleConnection = null
        }
    }

    private suspend fun writeBle(endpoint: BleEndpoint, bytes: ByteArray) {
        val payloadSize = (endpoint.mtu - 3).coerceIn(20, 244)
        var offset = 0
        while (offset < bytes.size) {
            val count = minOf(payloadSize, bytes.size - offset)
            val payload = bytes.copyOfRange(offset, offset + count)
            val withResponse = endpoint.writeType == BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            val writeDeferred = if (withResponse) CompletableDeferred<Int>() else null
            pendingBleWrite = writeDeferred
            val accepted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                endpoint.gatt.writeCharacteristic(
                    endpoint.characteristic,
                    payload,
                    endpoint.writeType,
                ) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                endpoint.characteristic.value = payload
                @Suppress("DEPRECATION")
                endpoint.characteristic.writeType = endpoint.writeType
                @Suppress("DEPRECATION")
                endpoint.gatt.writeCharacteristic(endpoint.characteristic)
            }
            check(accepted) { "BLE 写入请求被拒绝" }
            if (writeDeferred != null) {
                val status = withTimeout(BLE_WRITE_TIMEOUT_MS) { writeDeferred.await() }
                check(status == BluetoothGatt.GATT_SUCCESS) { "BLE 写入失败，状态码 $status" }
            } else {
                delay(BLE_NO_RESPONSE_DELAY_MS)
            }
            pendingBleWrite = null
            offset += count
        }
    }

    private fun disconnectActiveConnection(updateState: Boolean) {
        pendingBleConnection?.cancel()
        pendingBleConnection = null
        pendingBleWrite?.cancel()
        pendingBleWrite = null
        runCatching { classicSocket?.close() }
        classicSocket = null
        bleEndpoint?.let { endpoint ->
            runCatching { endpoint.gatt.disconnect() }
            runCatching { endpoint.gatt.close() }
        }
        bleEndpoint = null
        _connectionInfo.value = null
        if (updateState) _connectionStatus.value = ConnectionStatus.DISCONNECTED
    }

    private fun markConnected(printer: PrinterDevice, info: PrinterConnectionInfo) {
        _currentDevice.value = printer.copy(transports = setOf(info.transport))
        _connectionInfo.value = info
        _connectionStatus.value = ConnectionStatus.CONNECTED
        diagnostics?.append(
            "BT_CONNECTED",
            "device=${printer.displayName}, address=${maskAddress(printer.address)}, " +
                "transport=${info.transport.connectionLabel}, service=${info.serviceUuid ?: "N/A"}, " +
                "characteristic=${info.characteristicUuid ?: "N/A"}, " +
                "properties=${info.characteristicPropertiesLabel}",
        )
        if (BuildConfig.DEBUG) {
            Log.i(
                LOG_TAG,
                buildString {
                    appendLine("Printer connected")
                    appendLine("Name: ${printer.displayName}")
                    appendLine("Address: ${printer.address}")
                    appendLine("Transport: ${info.transport.connectionLabel}")
                    appendLine("Service UUID: ${info.serviceUuid ?: "N/A"}")
                    appendLine("Characteristic UUID: ${info.characteristicUuid ?: "N/A"}")
                    append("Characteristic properties: ${info.characteristicPropertiesLabel}")
                },
            )
        }
    }

    private fun handleConnectionLost(message: String) {
        diagnostics?.append(
            "BT_DISCONNECTED",
            "reason=$message, intentional=$intentionalDisconnect, " +
                "transport=${_connectionInfo.value?.transport?.connectionLabel ?: "unknown"}",
        )
        disconnectActiveConnection(updateState = false)
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        if (!intentionalDisconnect) reportError(message)
    }

    private fun mergeDevice(
        name: String,
        address: String,
        transport: BluetoothTransport,
        bonded: Boolean = false,
        rssi: Int? = null,
    ) {
        _devices.update { oldList ->
            val existing = oldList.firstOrNull {
                it.address == address && it.transports == setOf(transport)
            }
            val merged = if (existing == null) {
                PrinterDevice(name, address, setOf(transport), bonded, rssi)
            } else {
                existing.copy(
                    name = name.ifBlank { existing.name },
                    bonded = existing.bonded || bonded,
                    rssi = rssi ?: existing.rssi,
                )
            }
            (
                oldList.filterNot {
                    it.address == address && it.transports == setOf(transport)
                } + merged
            ).sortedWith(
                compareByDescending<PrinterDevice> { it.bonded }
                    .thenBy { it.displayName }
                    .thenBy { it.transports.singleOrNull()?.name.orEmpty() },
            )
        }
    }

    private fun findWritableCharacteristic(services: List<BluetoothGattService>): BluetoothGattCharacteristic? =
        services.asSequence()
            .flatMap { it.characteristics.asSequence() }
            .filter { characteristic ->
                val properties = characteristic.properties
                properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0 ||
                    properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0
            }
            .sortedByDescending { characteristic ->
                characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0
            }
            .firstOrNull()

    private fun safeName(device: BluetoothDevice): String =
        runCatching { device.name.orEmpty() }.getOrDefault("")

    private fun hasScanPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) ==
            PackageManager.PERMISSION_GRANTED && hasConnectPermission()
    } else {
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun hasConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    private val receiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = intent.bluetoothDeviceExtra() ?: return
                        val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                            .takeUnless { it == Short.MIN_VALUE }?.toInt()
                        mergeDevice(
                            name = safeName(device),
                            address = device.address,
                            transport = BluetoothTransport.CLASSIC,
                            bonded = device.bondState == BluetoothDevice.BOND_BONDED,
                            rssi = rssi,
                        )
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        val disconnected = intent.bluetoothDeviceExtra()
                        if (
                            disconnected != null &&
                            disconnected.address == _currentDevice.value?.address &&
                            isConnected
                        ) {
                            diagnostics?.append(
                                "BT_CALLBACK",
                                "收到 ACTION_ACL_DISCONNECTED, device=${safeName(disconnected)}, " +
                                    "address=${maskAddress(disconnected.address)}",
                            )
                            handleConnectionLost("打印机连接已断开")
                        }
                    }
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        if (state == BluetoothAdapter.STATE_OFF) {
                            diagnostics?.append("BT_CALLBACK", "手机蓝牙状态变为 STATE_OFF")
                            stopScan()
                            if (isConnected) handleConnectionLost("蓝牙已关闭，打印机连接断开")
                        }
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            appContext.registerReceiver(receiver, filter)
        }
    }

    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val advertisedName = result.scanRecord?.deviceName.orEmpty()
            mergeDevice(
                name = advertisedName.ifBlank { safeName(device) },
                address = device.address,
                transport = BluetoothTransport.BLE,
                bonded = device.bondState == BluetoothDevice.BOND_BONDED,
                rssi = result.rssi,
            )
        }

        override fun onScanFailed(errorCode: Int) {
            reportError("BLE 搜索失败，错误码 $errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            diagnostics?.append(
                "BLE_CALLBACK",
                "onConnectionStateChange status=$status, newState=$newState, " +
                    "active=${bleEndpoint?.gatt == gatt}",
            )
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    val requested = runCatching { gatt.requestMtu(PREFERRED_BLE_MTU) }.getOrDefault(false)
                    if (!requested) gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    pendingBleConnection?.completeExceptionally(
                        IOException("BLE 已断开，状态码 $status"),
                    )
                    if (bleEndpoint?.gatt == gatt) {
                        runCatching { gatt.close() }
                        bleEndpoint = null
                        handleConnectionLost("BLE 打印机连接已断开")
                    }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            diagnostics?.append("BLE_CALLBACK", "onMtuChanged mtu=$mtu, status=$status")
            negotiatedMtu = if (status == BluetoothGatt.GATT_SUCCESS) mtu else DEFAULT_BLE_MTU
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            diagnostics?.append("BLE_CALLBACK", "onServicesDiscovered status=$status")
            if (status != BluetoothGatt.GATT_SUCCESS) {
                pendingBleConnection?.completeExceptionally(IOException("读取 BLE 服务失败：$status"))
                return
            }
            val characteristic = findWritableCharacteristic(gatt.services)
            if (characteristic == null) {
                pendingBleConnection?.completeExceptionally(IOException("设备没有可写 BLE 特征"))
                return
            }
            val writeType = if (
                characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0
            ) {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            }
            pendingBleConnection?.complete(
                BleEndpoint(gatt, characteristic, negotiatedMtu, writeType),
            )
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                diagnostics?.append("BLE_WRITE_FAILED", "onCharacteristicWrite status=$status")
            }
            pendingBleWrite?.complete(status)
        }
    }

    private fun Intent.bluetoothDeviceExtra(): BluetoothDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

    private fun maskAddress(address: String): String =
        address.takeLast(8).padStart(address.length, '*')

    private data class BleEndpoint(
        val gatt: BluetoothGatt,
        val characteristic: BluetoothGattCharacteristic,
        val mtu: Int,
        val writeType: Int,
    )

    companion object {
        private const val LOG_TAG = "BluetoothManager"
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val SCAN_DURATION_MS = 12_000L
        private const val CONNECTION_TIMEOUT_MS = 15_000L
        private const val BLE_WRITE_TIMEOUT_MS = 5_000L
        private const val BLE_NO_RESPONSE_DELAY_MS = 12L
        private const val DEFAULT_BLE_MTU = 23
        private const val PREFERRED_BLE_MTU = 247
    }
}
