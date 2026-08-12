package com.wanlian.printer

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wanlian.printer.bluetooth.BluetoothManager
import com.wanlian.printer.model.ConnectionStatus
import com.wanlian.printer.model.ClosingTextBlockRules
import com.wanlian.printer.model.CoupletPairDocument
import com.wanlian.printer.model.CoupletSide
import com.wanlian.printer.model.CoupletTemplate
import com.wanlian.printer.model.DocumentMode
import com.wanlian.printer.model.PairPrintPlan
import com.wanlian.printer.model.PairPrintTimingRules
import com.wanlian.printer.model.PairFooterAlignmentRules
import com.wanlian.printer.model.PrintSettings
import com.wanlian.printer.model.PrintUnits
import com.wanlian.printer.model.PrintGate
import com.wanlian.printer.model.TemplateNameRules
import com.wanlian.printer.model.PrinterConnectionInfo
import com.wanlian.printer.model.PrinterDevice
import com.wanlian.printer.printing.BitmapRenderer
import com.wanlian.printer.printing.FontRepository
import com.wanlian.printer.printing.RenderedBitmap
import com.wanlian.printer.printing.RenderedCoupletPair
import com.wanlian.printer.printing.TsplPrinter
import com.wanlian.printer.printing.TsplPrintDiagnostics
import com.wanlian.printer.printing.TsplPrintStage
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
import kotlin.math.roundToInt

data class PairPrintFailure(
    val side: CoupletSide,
    val reason: String,
    val completedSides: Set<CoupletSide> = emptySet(),
)

enum class PairPrintUiStage {
    SENDING_LEFT,
    WAITING_FOR_LEFT,
    SENDING_RIGHT,
    WAITING_FOR_RIGHT,
}

private data class EditorHistoryState(
    val settings: PrintSettings,
    val documentMode: DocumentMode,
    val pairDocument: CoupletPairDocument?,
)

private data class ClosingBlockPairAlignment(
    val requestedOffsetDots: Float,
    val clampedOffsetDots: Float,
    val characterOffsetDots: List<Float>,
)

data class MainUiState(
    val settings: PrintSettings = PrintSettings(),
    val documentMode: DocumentMode = DocumentMode.SINGLE,
    val pairDocument: CoupletPairDocument? = null,
    val devices: List<PrinterDevice> = emptyList(),
    val isScanning: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val currentDevice: PrinterDevice? = null,
    val connectionInfo: PrinterConnectionInfo? = null,
    val lastDevice: PrinterDevice? = null,
    val autoReconnect: Boolean = true,
    val templates: List<CoupletTemplate> = emptyList(),
    val activeTemplateId: String? = null,
    val activeTemplateName: String? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val preview: RenderedBitmap? = null,
    val pairPreview: RenderedCoupletPair? = null,
    val testPreview: RenderedBitmap? = null,
    val polarityTestPreview: RenderedBitmap? = null,
    val isRendering: Boolean = false,
    val isPrinting: Boolean = false,
    val printProgress: Float = 0f,
    val printingSide: CoupletSide? = null,
    val pairPrintStage: PairPrintUiStage? = null,
    val pairPrintFailure: PairPrintFailure? = null,
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
    private val settingsHistory = mutableListOf(
        EditorHistoryState(
            settings = PrintSettings(),
            documentMode = DocumentMode.SINGLE,
            pairDocument = null,
        ),
    )
    private var historyIndex = 0
    private var autoReconnectAttempted = false

    val isBluetoothAvailable: Boolean get() = bluetoothManager.isBluetoothAvailable
    val isBluetoothEnabled: Boolean get() = bluetoothManager.isBluetoothEnabled

    init {
        FontRepository.initialize(application)
        observeBluetooth()
        observeLocalData()
        schedulePreview(immediate = true)
        renderTestPreviews()
    }

    fun updateSettings(transform: (PrintSettings) -> PrintSettings) {
        val state = _uiState.value
        val updated = transform(state.settings)
        if (updated == state.settings) return
        val updatedPair = if (state.documentMode == DocumentMode.PAIR) {
            state.pairDocument?.replaceSelected(updated)
        } else {
            null
        }
        val next = EditorHistoryState(
            settings = updatedPair?.selectedSettings ?: updated,
            documentMode = state.documentMode,
            pairDocument = updatedPair ?: state.pairDocument,
        )
        pushHistory(next)
        applyHistoryState(next)
    }

    fun undo() {
        if (historyIndex <= 0) return
        historyIndex--
        applyHistoryState(settingsHistory[historyIndex])
    }

    fun redo() {
        if (historyIndex >= settingsHistory.lastIndex) return
        historyIndex++
        applyHistoryState(settingsHistory[historyIndex])
    }

    fun startScan() = bluetoothManager.startScan()

    fun stopScan() = bluetoothManager.stopScan()

    fun connect(device: PrinterDevice) {
        if (_uiState.value.isPrinting) return
        viewModelScope.launch {
            bluetoothManager.connect(device)
            if (bluetoothManager.connectionStatus.value == ConnectionStatus.CONNECTED) {
                repository.saveLastDevice(bluetoothManager.currentDevice.value ?: device)
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

    fun enablePairMode() {
        val state = _uiState.value
        if (state.documentMode == DocumentMode.PAIR) return
        val pair = CoupletPairDocument.fromSingle(state.settings)
        resetHistory(
            EditorHistoryState(
                settings = pair.left,
                documentMode = DocumentMode.PAIR,
                pairDocument = pair,
            ),
        )
        _uiState.update {
            it.copy(
                documentMode = DocumentMode.PAIR,
                pairDocument = pair,
                settings = pair.left,
                pairPreview = null,
            )
        }
        schedulePreview(immediate = true)
    }

    fun selectCoupletSide(side: CoupletSide) {
        val state = _uiState.value
        val pair = state.pairDocument?.select(side) ?: return
        val selectedPreview = when (side) {
            CoupletSide.LEFT -> state.pairPreview?.left
            CoupletSide.RIGHT -> state.pairPreview?.right
        }
        resetHistory(
            EditorHistoryState(
                settings = pair.selectedSettings,
                documentMode = state.documentMode,
                pairDocument = pair,
            ),
        )
        _uiState.update {
            it.copy(
                pairDocument = pair,
                settings = pair.selectedSettings,
                preview = selectedPreview ?: it.preview,
            )
        }
    }

    fun alignPairFootersToSelected() {
        val state = _uiState.value
        val pair = state.pairDocument ?: return
        if (state.documentMode != DocumentMode.PAIR) return
        if (!pair.left.footerLabel.enabled || !pair.right.footerLabel.enabled) {
            reportMessage("请先启用左右联的落款标签")
            return
        }
        val aligned = PairFooterAlignmentRules.alignOtherToSelected(pair)
        if (aligned == pair) return
        val next = EditorHistoryState(
            settings = aligned.selectedSettings,
            documentMode = DocumentMode.PAIR,
            pairDocument = aligned,
        )
        pushHistory(next)
        applyHistoryState(next)
        reportMessage("已以${aligned.selectedSide.label}为基准对齐左右页尾")
    }

    fun alignPairClosingBlocks() {
        val state = _uiState.value
        val pair = state.pairDocument ?: return
        if (state.documentMode != DocumentMode.PAIR) return

        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                val pairLength = bitmapRenderer.resolvePairPaperLengthMm(pair)
                val rightBounds = bitmapRenderer.measureClosingTextBlockBounds(pair.right, pairLength)
                val leftBase = pair.left.copy(
                    closingTextBlock = pair.left.closingTextBlock.copy(
                        offsetYMm = 0f,
                        characterOffsetDots = emptyList(),
                    ),
                )
                val leftBaseBounds = bitmapRenderer.measureClosingTextBlockBounds(leftBase, pairLength)
                val characterOffsetDots = if (leftBaseBounds == null || rightBounds == null) {
                    null
                } else {
                    ClosingTextBlockRules.characterOffsetsForAlignment(
                        movingZeroOffsetCenters = leftBaseBounds.characterBounds.map {
                            it.zeroOffsetCenterYDots
                        },
                        anchorCenters = rightBounds.characterBounds.map { it.centerYDots },
                    )
                }
                if (rightBounds == null || characterOffsetDots == null) {
                    null
                } else {
                    val adjustedLeft = leftBase.copy(
                        closingTextBlock = leftBase.closingTextBlock.copy(
                            characterOffsetDots = characterOffsetDots,
                        ),
                    )
                    val adjustedLeftBounds = bitmapRenderer.measureClosingTextBlockBounds(
                        adjustedLeft,
                        pairLength,
                    ) ?: return@withContext null
                    val requestedOffsetDots = rightBounds.characterBounds.first().centerYDots -
                        adjustedLeftBounds.alignmentGeometry.zeroOffsetFirstCharacterCenterYDots
                    val clampedOffsetDots = ClosingTextBlockRules.alignmentOffsetDots(
                        geometry = adjustedLeftBounds.alignmentGeometry,
                        anchorFirstCharacterCenterYDots = rightBounds.characterBounds.first().centerYDots,
                    )
                    pair to ClosingBlockPairAlignment(
                        requestedOffsetDots = requestedOffsetDots,
                        clampedOffsetDots = clampedOffsetDots,
                        characterOffsetDots = characterOffsetDots,
                    )
                }
            }
            if (result == null) {
                reportMessage("未找到左右联可对齐的尾字块")
                return@launch
            }
            val (snapshotPair, alignment) = result
            if (
                _uiState.value.documentMode != DocumentMode.PAIR ||
                _uiState.value.pairDocument != snapshotPair
            ) {
                return@launch
            }
            val alignedOffsetYMm = PrintUnits.dotsToMm(alignment.clampedOffsetDots.roundToInt())
            val aligned = snapshotPair.copy(
                left = snapshotPair.left.copy(
                    closingTextBlock = snapshotPair.left.closingTextBlock.copy(
                        offsetYMm = ClosingTextBlockRules.clampOffsetYMm(alignedOffsetYMm),
                        characterOffsetDots = alignment.characterOffsetDots,
                    ),
                ),
            )
            if (aligned == snapshotPair) {
                reportMessage("左右联尾字已经对齐")
                return@launch
            }
            val next = EditorHistoryState(
                settings = aligned.selectedSettings,
                documentMode = DocumentMode.PAIR,
                pairDocument = aligned,
            )
            pushHistory(next)
            applyHistoryState(next)
            if (alignment.requestedOffsetDots != alignment.clampedOffsetDots) {
                reportMessage("已对齐到左联尾字可用范围内的最近位置")
            } else {
                reportMessage("已以右联为基准对齐左右联尾字")
            }
        }
    }

    fun keepPairSideAsSingle(side: CoupletSide) {
        val state = _uiState.value
        val pair = state.pairDocument ?: return
        val keptSettings = if (side == CoupletSide.LEFT) pair.left else pair.right
        val keptPreview = if (side == CoupletSide.LEFT) state.pairPreview?.left else state.pairPreview?.right
        resetHistory(
            EditorHistoryState(
                settings = keptSettings,
                documentMode = DocumentMode.SINGLE,
                pairDocument = null,
            ),
        )
        _uiState.update {
            it.copy(
                documentMode = DocumentMode.SINGLE,
                pairDocument = null,
                settings = keptSettings,
                preview = keptPreview,
                pairPreview = null,
            )
        }
        schedulePreview(immediate = true)
    }

    fun saveTemplate(name: String) {
        val normalizedName = TemplateNameRules.normalize(name)
        if (normalizedName.isEmpty()) {
            reportMessage("请输入模板名称")
            return
        }
        viewModelScope.launch {
            val state = _uiState.value
            val templateId = repository.saveTemplate(
                name = normalizedName,
                settings = state.settings,
                documentMode = state.documentMode,
                pairDocument = state.pairDocument,
            )
            if (templateId != null) {
                _uiState.update {
                    it.copy(activeTemplateId = templateId, activeTemplateName = normalizedName)
                }
                reportMessage("模板已保存：$normalizedName")
            }
        }
    }

    fun updateCurrentTemplate() {
        val state = _uiState.value
        val templateId = state.activeTemplateId
        val templateName = state.activeTemplateName
        if (templateId == null || templateName == null) {
            reportMessage("当前没有已打开的模板，请另存为新模板")
            return
        }
        viewModelScope.launch {
            repository.saveTemplate(
                id = templateId,
                name = templateName,
                settings = state.settings,
                documentMode = state.documentMode,
                pairDocument = state.pairDocument,
            )
            reportMessage("已更新模板：$templateName")
        }
    }

    fun renameTemplate(template: CoupletTemplate, name: String) {
        val normalizedName = TemplateNameRules.normalize(name)
        if (normalizedName.isEmpty()) {
            reportMessage("请输入模板名称")
            return
        }
        viewModelScope.launch {
            if (repository.renameTemplate(template.id, normalizedName)) {
                _uiState.update { state ->
                    state.copy(
                        templates = state.templates.map { current ->
                            if (current.id == template.id) current.copy(name = normalizedName) else current
                        },
                        activeTemplateName = if (state.activeTemplateId == template.id) {
                            normalizedName
                        } else {
                            state.activeTemplateName
                        },
                    )
                }
                reportMessage("模板已重命名：$normalizedName")
            }
        }
    }

    fun loadTemplate(template: CoupletTemplate) {
        var missingFont = false
        val sourcePair = template.pairDocument?.takeIf { template.documentMode == DocumentMode.PAIR }
        val pair = sourcePair?.let { document ->
            val (left, leftMissing) = normalizeFontReferences(document.left)
            val (right, rightMissing) = normalizeFontReferences(document.right)
            missingFont = leftMissing || rightMissing
            document.copy(left = left, right = right)
        }
        val normalizedSingle = normalizeFontReferences(template.settings).also {
            missingFont = missingFont || it.second
        }.first
        val updated = pair?.selectedSettings ?: normalizedSingle
        val documentMode = if (pair == null) DocumentMode.SINGLE else DocumentMode.PAIR
        resetHistory(
            EditorHistoryState(
                settings = updated,
                documentMode = documentMode,
                pairDocument = pair,
            ),
        )
        _uiState.update {
            it.copy(
                documentMode = documentMode,
                pairDocument = pair,
                settings = updated,
                pairPreview = null,
                activeTemplateId = template.id,
                activeTemplateName = template.name,
            )
        }
        schedulePreview(immediate = true)
        renderTestPreviews()
        reportMessage(
            if (missingFont) {
                "原字体已不存在，已使用默认字体。"
            } else {
                "已载入模板：${template.name}"
            },
        )
    }

    fun deleteTemplate(template: CoupletTemplate) {
        viewModelScope.launch {
            repository.deleteTemplate(template.id)
            _uiState.update { state ->
                if (state.activeTemplateId == template.id) {
                    state.copy(activeTemplateId = null, activeTemplateName = null)
                } else {
                    state
                }
            }
            reportMessage("模板已删除")
        }
    }

    fun printCouplet() {
        if (_uiState.value.documentMode == DocumentMode.PAIR) {
            printPairFrom(startIndex = 0)
        } else {
            printRendered(
                successMessage = "打印数据已发送",
                errorMessage = "打印失败",
            ) { settings -> bitmapRenderer.renderCouplet(settings) }
        }
    }

    fun retryPairPrint() {
        val failedSide = _uiState.value.pairPrintFailure?.side ?: return
        _uiState.update { it.copy(pairPrintFailure = null) }
        printPairFrom(if (failedSide == CoupletSide.LEFT) 0 else 1)
    }

    fun printRightOnly() = printPairFrom(startIndex = 1)

    fun skipFailedPairSide() {
        val failedSide = _uiState.value.pairPrintFailure?.side ?: return
        _uiState.update { it.copy(pairPrintFailure = null) }
        if (failedSide == CoupletSide.LEFT) {
            printPairFrom(startIndex = 1)
        } else {
            reportMessage("已取消右联重试；双联打印未完成")
        }
    }

    fun cancelPairPrint() {
        _uiState.update {
            it.copy(
                pairPrintFailure = null,
                printingSide = null,
                pairPrintStage = null,
                printProgress = 0f,
            )
        }
        reportMessage("已取消双联打印")
    }

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
            bluetoothManager.connectionInfo.collect { value ->
                _uiState.update { it.copy(connectionInfo = value) }
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
                _uiState.update { state ->
                    val active = state.activeTemplateId?.let { id ->
                        templates.firstOrNull { it.id == id }
                    }
                    val nextActiveId = if (state.activeTemplateId != null && active == null) {
                        null
                    } else {
                        state.activeTemplateId
                    }
                    state.copy(
                        templates = templates,
                        activeTemplateId = nextActiveId,
                        activeTemplateName = active?.name ?: if (nextActiveId == null) {
                            null
                        } else {
                            state.activeTemplateName
                        },
                    )
                }
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

    private fun pushHistory(next: EditorHistoryState) {
        if (historyIndex < settingsHistory.lastIndex) {
            settingsHistory.subList(historyIndex + 1, settingsHistory.size).clear()
        }
        settingsHistory += next
        if (settingsHistory.size > MAX_HISTORY) {
            settingsHistory.removeAt(0)
        }
        historyIndex = settingsHistory.lastIndex
    }

    private fun applyHistoryState(history: EditorHistoryState) {
        _uiState.update {
            it.copy(
                settings = history.settings,
                documentMode = history.documentMode,
                pairDocument = history.pairDocument,
                canUndo = historyIndex > 0,
                canRedo = historyIndex < settingsHistory.lastIndex,
            )
        }
        schedulePreview()
        renderTestPreviews()
    }

    private fun printPairFrom(startIndex: Int) {
        when (PrintGate.resolve(_uiState.value.connectionStatus, _uiState.value.isPrinting)) {
            PrintGate.ALLOWED -> Unit
            PrintGate.BLOCKED_CONNECTING -> {
                reportMessage("正在连接打印机，请稍候")
                return
            }
            PrintGate.BLOCKED_DISCONNECTED -> {
                reportMessage("请先连接打印设备后再开始打印")
                return
            }
            PrintGate.BLOCKED_PRINTING -> return
        }
        val pair = _uiState.value.pairDocument ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isPrinting = true,
                    printProgress = startIndex / 2f,
                    pairPrintFailure = null,
                    pairPrintStage = if (startIndex == 1) {
                        PairPrintUiStage.SENDING_RIGHT
                    } else {
                        PairPrintUiStage.SENDING_LEFT
                    },
                )
            }
            var activeSide: CoupletSide? = null
            val completedSides = linkedSetOf<CoupletSide>()
            try {
                val pairLength = withContext(Dispatchers.Default) {
                    bitmapRenderer.resolvePairPaperLengthMm(pair)
                }
                val jobs = PairPrintPlan.jobs(pair)
                for (index in startIndex until jobs.size) {
                    val job = jobs[index]
                    activeSide = job.side
                    if (job.side == CoupletSide.RIGHT) {
                        logPairPrint("RIGHT（上联）2/2：检查 Bluetooth connection")
                        check(bluetoothManager.isConnected) { "RIGHT 发送前蓝牙连接已断开" }
                    }
                    _uiState.update {
                        it.copy(
                            printingSide = job.side,
                            pairPrintStage = if (job.side == CoupletSide.LEFT) {
                                PairPrintUiStage.SENDING_LEFT
                            } else {
                                PairPrintUiStage.SENDING_RIGHT
                            },
                            printProgress = index / jobs.size.toFloat(),
                        )
                    }
                    val rendered = withContext(Dispatchers.Default) {
                        bitmapRenderer.renderCouplet(job.settings, forcedPaperLengthMm = pairLength)
                    }
                    logPairPrint("${job.side.diagnosticLabel(index)} Bitmap 生成完成：" +
                        "${rendered.printMask.width}x${rendered.printMask.height}, " +
                        "SIZE=${rendered.paperWidthMm}x${rendered.paperLengthMm}mm")
                    try {
                        tsplPrinter.print(
                            rendered = rendered,
                            settings = job.settings,
                            onProgress = { sideProgress ->
                                _uiState.update {
                                    it.copy(printProgress = (index + sideProgress) / jobs.size.toFloat())
                                }
                            },
                            onStage = { stage, diagnostics ->
                                logTsplStage(job.side, index, stage, diagnostics)
                            },
                        )
                        completedSides += job.side
                    } finally {
                        rendered.previewBitmap.recycle()
                        rendered.printMask.recycle()
                    }
                    val shouldWaitForPhysicalCompletion =
                        job.side == CoupletSide.RIGHT || index < jobs.lastIndex
                    if (shouldWaitForPhysicalCompletion) {
                        val waitMs = PairPrintTimingRules.interJobWaitMs(
                            paperLengthMm = pairLength,
                            speedInchesPerSecond = job.settings.speedInchesPerSecond,
                        )
                        logPairPrint("等待 ${job.side.name} 实际打印完成：${waitMs}ms")
                        _uiState.update {
                            it.copy(
                                printingSide = job.side,
                                pairPrintStage = if (job.side == CoupletSide.LEFT) {
                                    PairPrintUiStage.WAITING_FOR_LEFT
                                } else {
                                    PairPrintUiStage.WAITING_FOR_RIGHT
                                },
                                printProgress = if (job.side == CoupletSide.LEFT) 0.5f else 0.99f,
                            )
                        }
                        delay(waitMs)
                    }
                }
                reportMessage(
                    if (startIndex == 1) {
                        "RIGHT（上联）单独打印命令已发送"
                    } else {
                        "双联打印完成"
                    },
                )
            } catch (error: Throwable) {
                val failedSide = activeSide ?: if (startIndex == 0) CoupletSide.LEFT else CoupletSide.RIGHT
                Log.e(PAIR_PRINT_LOG_TAG, "${failedSide.name} print failed", error)
                _uiState.update {
                    it.copy(
                        pairPrintFailure = PairPrintFailure(
                            side = failedSide,
                            reason = error.message ?: "打印中断",
                            completedSides = completedSides.toSet(),
                        ),
                    )
                }
                reportMessage(
                    if (failedSide == CoupletSide.RIGHT && CoupletSide.LEFT in completedSides) {
                        "左联打印完成，右联打印失败"
                    } else {
                        "${failedSide.label}打印失败"
                    },
                )
            } finally {
                _uiState.update {
                    it.copy(isPrinting = false, printingSide = null, pairPrintStage = null)
                }
            }
        }
    }

    private fun logTsplStage(
        side: CoupletSide,
        index: Int,
        stage: TsplPrintStage,
        diagnostics: TsplPrintDiagnostics,
    ) {
        val prefix = side.diagnosticLabel(index)
        val detail = when (stage) {
            TsplPrintStage.BITMAP_SEND_STARTED ->
                "BITMAP 开始发送：SIZE=${diagnostics.paperWidthMm}x${diagnostics.paperLengthMm}mm, " +
                    "bitmap=${diagnostics.bitmapWidthDots}x${diagnostics.bitmapHeightDots}, " +
                    "widthBytes=${diagnostics.widthBytes}, dataBytes=${diagnostics.bitmapDataBytes}"
            TsplPrintStage.BITMAP_SEND_COMPLETED ->
                "BITMAP 发送完成：dataBytes=${diagnostics.bitmapDataBytes}"
            TsplPrintStage.PRINT_COMMAND_SENT -> "PRINT 命令发送完成"
        }
        logPairPrint("$prefix $detail")
    }

    private fun CoupletSide.diagnosticLabel(index: Int): String = when (this) {
        CoupletSide.LEFT -> "LEFT（下联）${index + 1}/2："
        CoupletSide.RIGHT -> "RIGHT（上联）${index + 1}/2："
    }

    private fun logPairPrint(message: String) {
        Log.i(PAIR_PRINT_LOG_TAG, message)
    }

    private fun printRendered(
        successMessage: String,
        errorMessage: String,
        renderer: (PrintSettings) -> RenderedBitmap,
    ) {
        when (PrintGate.resolve(_uiState.value.connectionStatus, _uiState.value.isPrinting)) {
            PrintGate.ALLOWED -> Unit
            PrintGate.BLOCKED_CONNECTING -> {
                reportMessage("正在连接打印机，请稍候")
                return
            }
            PrintGate.BLOCKED_DISCONNECTED -> {
                reportMessage("请先连接打印设备后再开始打印")
                return
            }
            PrintGate.BLOCKED_PRINTING -> return
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
            val snapshot = _uiState.value
            try {
                if (snapshot.documentMode == DocumentMode.PAIR && snapshot.pairDocument != null) {
                    val renderedPair = withContext(Dispatchers.Default) {
                        bitmapRenderer.renderCoupletPair(snapshot.pairDocument)
                    }
                    _uiState.update { current ->
                        val selectedPreview = if (
                            current.pairDocument?.selectedSide == CoupletSide.RIGHT
                        ) {
                            renderedPair.right
                        } else {
                            renderedPair.left
                        }
                        current.copy(pairPreview = renderedPair, preview = selectedPreview)
                    }
                } else {
                    val rendered = withContext(Dispatchers.Default) {
                        bitmapRenderer.renderCouplet(snapshot.settings)
                    }
                    _uiState.update { it.copy(preview = rendered, pairPreview = null) }
                }
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

    private fun resetHistory(history: EditorHistoryState) {
        settingsHistory.clear()
        settingsHistory += history
        historyIndex = 0
        _uiState.update { it.copy(canUndo = false, canRedo = false) }
    }

    private fun normalizeFontReferences(settings: PrintSettings): Pair<PrintSettings, Boolean> {
        var missing = false
        fun availableOrDefault(id: String): String {
            if (FontRepository.hasFont(id)) return id
            missing = true
            return FontRepository.defaultFont.id
        }
        val footer = settings.footerLabel.copy(
            fontId = availableOrDefault(settings.footerLabel.fontId),
        )
        val personBlock = settings.personBlock.copy(
            fontId = availableOrDefault(settings.personBlock.fontId),
        )
        return settings.copy(
            fontId = availableOrDefault(settings.fontId),
            personBlock = personBlock,
            footerLabel = footer,
        ) to missing
    }

    override fun onCleared() {
        bluetoothManager.close()
        super.onCleared()
    }

    companion object {
        private const val PAIR_PRINT_LOG_TAG = "PairPrint"
        private const val MAX_HISTORY = 60
    }
}
