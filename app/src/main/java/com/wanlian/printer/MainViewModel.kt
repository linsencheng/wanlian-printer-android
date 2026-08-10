package com.wanlian.printer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wanlian.printer.bluetooth.BluetoothManager
import com.wanlian.printer.model.ConnectionStatus
import com.wanlian.printer.model.CoupletTemplate
import com.wanlian.printer.model.PrintSettings
import com.wanlian.printer.model.PrinterDevice
import com.wanlian.printer.printing.BitmapRenderer
import com.wanlian.printer.printing.RenderedBitmap
import com.wanlian.printer.printing.TsplPrinter
import com.wanlian.printer.storage.TemplateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MainUiState(
    val settings: PrintSettings = PrintSettings(),
    val devices: List<PrinterDevice> = emptyList(),
    val isScanning: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val currentDevice: PrinterDevice? = null,
    val lastDevice: PrinterDevice? = null,
    val autoReconnect: Boolean = true,
    val templates: List<CoupletTemplate> = emptyList(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val preview: RenderedBitmap? = null,
    val testPreview: RenderedBitmap? = null,
    val polarityTestPreview: RenderedBitmap? = null,
    val isRendering: Boolean = false,
    val isPrinting: Boolean = false,
    val printProgress: Float = 0f,
    val message: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val bluetoothManager = BluetoothManager(application)
    private val bitmapRenderer = BitmapRenderer()
    private val tsplPrinter = TsplPrinter(bluetoothManager)
    private val repository = TemplateRepository(application)
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    private var previewJob: Job? = null
    private var testPreviewJob: Job? = null
    private val settingsHistory = mutableListOf(PrintSettings())
    private var historyIndex = 0
    private var autoReconnectAttempted = false

    val isBluetoothAvailable: Boolean get() = bluetoothManager.isBluetoothAvailable
    val isBluetoothEnabled: Boolean get() = bluetoothManager.isBluetoothEnabled

    init {
        observeBluetooth()
        observeLocalData()
        schedulePreview(immediate = true)
        renderTestPreviews()
    }

    fun updateSettings(transform: (PrintSettings) -> PrintSettings) {
        val updated = transform(_uiState.value.settings)
        if (updated == _uiState.value.settings) return
        if (historyIndex < settingsHistory.lastIndex) {
            settingsHistory.subList(historyIndex + 1, settingsHistory.size).clear()
        }
        settingsHistory += updated
        if (settingsHistory.size > MAX_HISTORY) settingsHistory.removeAt(0) else historyIndex++
        applySettings(updated)
    }

    fun undo() {
        if (historyIndex <= 0) return
        historyIndex--
        applySettings(settingsHistory[historyIndex])
    }

    fun redo() {
        if (historyIndex >= settingsHistory.lastIndex) return
        historyIndex++
        applySettings(settingsHistory[historyIndex])
    }

    fun startScan() = bluetoothManager.startScan()

    fun stopScan() = bluetoothManager.stopScan()

    fun connect(device: PrinterDevice) {
        if (_uiState.value.isPrinting) return
        viewModelScope.launch {
            bluetoothManager.connect(device)
            if (bluetoothManager.connectionStatus.value == ConnectionStatus.CONNECTED) {
                repository.saveLastDevice(device)
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch { bluetoothManager.disconnect() }
    }

    fun reconnect() {
        val candidate = _uiState.value.currentDevice ?: _uiState.value.lastDevice
        if (candidate == null) {
            reportMessage("没有上次使用的打印机")
        } else {
            connect(candidate)
        }
    }

    fun setAutoReconnect(enabled: Boolean) {
        _uiState.update { it.copy(autoReconnect = enabled) }
        viewModelScope.launch { repository.setAutoReconnect(enabled) }
    }

    fun saveTemplate(name: String) {
        viewModelScope.launch {
            repository.saveTemplate(name, _uiState.value.settings)
            reportMessage("模板已保存")
        }
    }

    fun loadTemplate(template: CoupletTemplate) {
        val updated = template.settings
        if (historyIndex < settingsHistory.lastIndex) {
            settingsHistory.subList(historyIndex + 1, settingsHistory.size).clear()
        }
        settingsHistory += updated
        historyIndex = settingsHistory.lastIndex
        applySettings(updated)
        reportMessage("已载入模板：${template.name}")
    }

    fun deleteTemplate(template: CoupletTemplate) {
        viewModelScope.launch {
            repository.deleteTemplate(template.id)
            reportMessage("模板已删除")
        }
    }

    fun printCouplet() = printRendered(
        successMessage = "打印数据已发送",
        errorMessage = "打印失败",
    ) { settings -> bitmapRenderer.renderCouplet(settings) }

    fun printTestPage() = printRendered(
        successMessage = "测试页已发送",
        errorMessage = "测试打印失败",
    ) { settings -> bitmapRenderer.renderTestPage(settings) }

    fun printPolarityTest() = printRendered(
        successMessage = "白字极性测试已发送",
        errorMessage = "极性测试打印失败",
    ) { settings -> bitmapRenderer.renderPolarityTest(settings) }

    fun refreshPreview() = schedulePreview(immediate = true)

    fun reportMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    fun consumeMessage() {
        bluetoothManager.clearError()
        _uiState.update { it.copy(message = null) }
    }

    private fun observeBluetooth() {
        viewModelScope.launch {
            bluetoothManager.devices.collect { value ->
                _uiState.update { it.copy(devices = value) }
            }
        }
        viewModelScope.launch {
            bluetoothManager.isScanning.collect { value ->
                _uiState.update { it.copy(isScanning = value) }
            }
        }
        viewModelScope.launch {
            bluetoothManager.connectionStatus.collect { value ->
                _uiState.update { it.copy(connectionStatus = value) }
            }
        }
        viewModelScope.launch {
            bluetoothManager.currentDevice.collect { value ->
                _uiState.update { it.copy(currentDevice = value) }
            }
        }
        viewModelScope.launch {
            bluetoothManager.lastError.collect { value ->
                if (value != null) _uiState.update { it.copy(message = value) }
            }
        }
    }

    private fun observeLocalData() {
        viewModelScope.launch {
            repository.templates.collect { templates ->
                _uiState.update { it.copy(templates = templates) }
            }
        }
        viewModelScope.launch {
            repository.devicePreferences.collect { preferences ->
                _uiState.update {
                    it.copy(
                        lastDevice = preferences.lastDevice,
                        autoReconnect = preferences.autoReconnect,
                    )
                }
                if (
                    !autoReconnectAttempted &&
                    preferences.autoReconnect &&
                    preferences.lastDevice != null &&
                    bluetoothManager.isBluetoothEnabled
                ) {
                    autoReconnectAttempted = true
                    connect(preferences.lastDevice)
                }
            }
        }
    }

    private fun applySettings(settings: PrintSettings) {
        _uiState.update {
            it.copy(
                settings = settings,
                canUndo = historyIndex > 0,
                canRedo = historyIndex < settingsHistory.lastIndex,
            )
        }
        schedulePreview()
        renderTestPreviews()
    }

    private fun printRendered(
        successMessage: String,
        errorMessage: String,
        renderer: (PrintSettings) -> RenderedBitmap,
    ) {
        if (_uiState.value.isPrinting) return
        if (_uiState.value.connectionStatus != ConnectionStatus.CONNECTED) {
            reportMessage("请先连接打印机")
            return
        }
        val settings = _uiState.value.settings
        viewModelScope.launch {
            _uiState.update { it.copy(isPrinting = true, printProgress = 0f) }
            try {
                val rendered = withContext(Dispatchers.Default) { renderer(settings) }
                tsplPrinter.print(rendered, settings) { progress ->
                    _uiState.update { it.copy(printProgress = progress) }
                }
                reportMessage(successMessage)
            } catch (error: Throwable) {
                reportMessage(error.message ?: errorMessage)
            } finally {
                _uiState.update { it.copy(isPrinting = false) }
            }
        }
    }

    private fun schedulePreview(immediate: Boolean = false) {
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            if (!immediate) delay(100)
            _uiState.update { it.copy(isRendering = true) }
            val settings = _uiState.value.settings
            try {
                val rendered = withContext(Dispatchers.Default) {
                    bitmapRenderer.renderCouplet(settings)
                }
                _uiState.update { it.copy(preview = rendered) }
            } catch (error: Throwable) {
                reportMessage("生成预览失败：${error.message ?: "内存不足"}")
            } finally {
                _uiState.update { it.copy(isRendering = false) }
            }
        }
    }

    private fun renderTestPreviews() {
        testPreviewJob?.cancel()
        val settings = _uiState.value.settings
        testPreviewJob = viewModelScope.launch {
            delay(80)
            val test = withContext(Dispatchers.Default) { bitmapRenderer.renderTestPage(settings) }
            val polarity = withContext(Dispatchers.Default) { bitmapRenderer.renderPolarityTest(settings) }
            _uiState.update { it.copy(testPreview = test, polarityTestPreview = polarity) }
        }
    }

    override fun onCleared() {
        bluetoothManager.close()
        super.onCleared()
    }

    companion object {
        private const val MAX_HISTORY = 60
    }
}
