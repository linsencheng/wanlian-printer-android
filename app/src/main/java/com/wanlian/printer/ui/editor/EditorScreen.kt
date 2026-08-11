package com.wanlian.printer.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wanlian.printer.MainUiState
import com.wanlian.printer.model.ConnectionStatus
import com.wanlian.printer.model.CoupletSide
import com.wanlian.printer.model.CoupletTemplate
import com.wanlian.printer.model.DocumentMode
import com.wanlian.printer.model.PrintGate
import com.wanlian.printer.model.PrintSettings
import com.wanlian.printer.model.TemplateNameRules
import com.wanlian.printer.ui.components.CompactNumberControl
import com.wanlian.printer.ui.components.CompactPreviewControls
import com.wanlian.printer.ui.components.CoupletPreview
import com.wanlian.printer.ui.components.ChoiceChips
import com.wanlian.printer.ui.components.PreviewTransformState
import com.wanlian.printer.ui.components.SwitchRow
import com.wanlian.printer.ui.components.rememberPreviewTransformState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private enum class EditorPanelSnap(val fraction: Float) {
    MINIMIZED(0.13f),
    COMPACT(0.34f),
    EXPANDED(0.60f),
    ;

    val mode: EditorPanelMode
        get() = when (this) {
            MINIMIZED -> EditorPanelMode.MINIMIZED
            COMPACT -> EditorPanelMode.COMPACT
            EXPANDED -> EditorPanelMode.EXPANDED
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    state: MainUiState,
    onSettingsChange: ((PrintSettings) -> PrintSettings) -> Unit,
    onOpenDevices: () -> Unit,
    onOpenSettings: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSaveTemplate: (String) -> Unit,
    onUpdateCurrentTemplate: () -> Unit,
    onLoadTemplate: (CoupletTemplate) -> Unit,
    onRenameTemplate: (CoupletTemplate, String) -> Unit,
    onDeleteTemplate: (CoupletTemplate) -> Unit,
    onPrint: () -> Unit,
    onEnablePairMode: () -> Unit,
    onKeepPairSideAsSingle: (CoupletSide) -> Unit,
    onSelectCoupletSide: (CoupletSide) -> Unit,
    onAlignPairFooters: () -> Unit,
    onRetryPairPrint: () -> Unit,
    onSkipFailedPairSide: () -> Unit,
    onCancelPairPrint: () -> Unit,
) {
    var showPrintConfirmation by remember { mutableStateOf(false) }
    var showDisconnectedDialog by remember { mutableStateOf(false) }
    var showConnectingDialog by remember { mutableStateOf(false) }
    var showPaperSizeDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showSaveChoiceDialog by remember { mutableStateOf(false) }
    var saveDialogIsCopy by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }
    var showModeDialog by remember { mutableStateOf(false) }
    var showKeepSideDialog by remember { mutableStateOf(false) }
    var focusPreview by rememberSaveable { mutableStateOf(false) }
    var selectedTool by rememberSaveable { mutableStateOf(EditorTool.TEXT) }
    var panelSnap by rememberSaveable { mutableStateOf(EditorPanelSnap.COMPACT) }
    var lastOpenPanelSnap by rememberSaveable { mutableStateOf(EditorPanelSnap.COMPACT) }
    var panelFraction by rememberSaveable { mutableFloatStateOf(EditorPanelSnap.COMPACT.fraction) }
    var templateName by remember { mutableStateOf("") }
    val previewTransformState = rememberPreviewTransformState()

    fun selectPanelSnap(target: EditorPanelSnap) {
        panelSnap = target
        panelFraction = target.fraction
        if (target != EditorPanelSnap.MINIMIZED) {
            lastOpenPanelSnap = target
        }
    }

    fun snapToNearestPanel(currentFraction: Float) {
        selectPanelSnap(
            EditorPanelSnap.entries.minBy { snap -> abs(snap.fraction - currentFraction) },
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val twoPane = maxWidth >= 700.dp
        Scaffold(
            topBar = {
                if (focusPreview) {
                    PreviewFocusTopBar(state, previewTransformState)
                } else {
                    EditorTopBar(
                        state = state,
                        onTemplates = { showTemplates = true },
                        onDevice = onOpenDevices,
                        onUndo = onUndo,
                        onRedo = onRedo,
                        previewTransformState = previewTransformState,
                        onFullscreen = { focusPreview = true },
                        onPaperSize = { showPaperSizeDialog = true },
                        onDocumentMode = { showModeDialog = true },
                    )
                }
            },
            bottomBar = {
                if (!focusPreview) {
                    EditorBottomBar(
                        state = state,
                        onSave = {
                            if (state.activeTemplateId == null) {
                                templateName = defaultTemplateName(state.documentMode)
                                saveDialogIsCopy = false
                                showSaveDialog = true
                            } else {
                                showSaveChoiceDialog = true
                            }
                        },
                        onPrint = {
                            when (PrintGate.resolve(state.connectionStatus, state.isPrinting)) {
                                PrintGate.ALLOWED -> showPrintConfirmation = true
                                PrintGate.BLOCKED_CONNECTING -> showConnectingDialog = true
                                PrintGate.BLOCKED_DISCONNECTED -> showDisconnectedDialog = true
                                PrintGate.BLOCKED_PRINTING -> Unit
                            }
                        },
                        onSettings = onOpenSettings,
                    )
                }
            },
        ) { contentPadding ->
            if (twoPane && !focusPreview) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(contentPadding).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CoupletPreview(
                        rendered = state.preview,
                        isRendering = state.isRendering,
                        onFullscreen = { focusPreview = true },
                        transformState = previewTransformState,
                        showControls = false,
                        documentMode = state.documentMode,
                        pairRendered = state.pairPreview,
                        selectedSide = state.pairDocument?.selectedSide ?: CoupletSide.LEFT,
                        onSideSelected = onSelectCoupletSide,
                        onPersonBlockMove = { deltaXMm, deltaYMm ->
                            onSettingsChange { current ->
                                current.copy(
                                    personBlock = current.personBlock.movedBy(deltaXMm, deltaYMm),
                                )
                            }
                        },
                        modifier = Modifier.weight(0.65f).fillMaxSize(),
                    )
                    EditorToolPanel(
                        state = state,
                        onSettingsChange = onSettingsChange,
                        onAlignPairFooters = onAlignPairFooters,
                        modifier = Modifier.weight(0.35f).fillMaxSize(),
                    )
                }
            } else {
                PhoneEditorWorkspace(
                    state = state,
                    selectedTool = selectedTool,
                    onToolSelected = { tool ->
                        selectedTool = tool
                        if (tool == EditorTool.FOOTER) {
                            selectPanelSnap(EditorPanelSnap.EXPANDED)
                        }
                    },
                    panelSnap = panelSnap,
                    panelFraction = panelFraction,
                    onPanelFractionChange = { panelFraction = it },
                    onPanelDragStopped = ::snapToNearestPanel,
                    onTogglePreviewSpace = {
                        if (panelSnap == EditorPanelSnap.MINIMIZED) {
                            selectPanelSnap(lastOpenPanelSnap)
                        } else {
                            selectPanelSnap(EditorPanelSnap.MINIMIZED)
                        }
                    },
                    focusPreview = focusPreview,
                    onFocusPreview = { focusPreview = true },
                    onContinueEditing = { focusPreview = false },
                    onSettingsChange = onSettingsChange,
                    onSelectCoupletSide = onSelectCoupletSide,
                    onAlignPairFooters = onAlignPairFooters,
                    previewTransformState = previewTransformState,
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                )
            }
        }
    }

    if (showPrintConfirmation) {
        ModalBottomSheet(onDismissRequest = { showPrintConfirmation = false }) {
            PrintConfirmation(
                state = state,
                onCancel = { showPrintConfirmation = false },
                onStart = {
                    showPrintConfirmation = false
                    onPrint()
                },
            )
        }
    }
    if (showModeDialog) {
        AlertDialog(
            onDismissRequest = { showModeDialog = false },
            title = { Text("挽联模式") },
            text = {
                ChoiceChips(
                    values = DocumentMode.entries,
                    selected = state.documentMode,
                    label = { it.label },
                    onSelected = { mode ->
                        showModeDialog = false
                        when {
                            mode == state.documentMode -> Unit
                            mode == DocumentMode.PAIR -> onEnablePairMode()
                            else -> showKeepSideDialog = true
                        }
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { showModeDialog = false }) { Text("关闭") }
            },
        )
    }
    if (showKeepSideDialog) {
        AlertDialog(
            onDismissRequest = { showKeepSideDialog = false },
            title = { Text("切换为单联") },
            text = { Text("左右联内容可能不同，请选择要保留的一条。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showKeepSideDialog = false
                        onKeepPairSideAsSingle(CoupletSide.LEFT)
                    },
                ) { Text("保留左联") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showKeepSideDialog = false }) { Text("取消") }
                    TextButton(
                        onClick = {
                            showKeepSideDialog = false
                            onKeepPairSideAsSingle(CoupletSide.RIGHT)
                        },
                    ) { Text("保留右联") }
                }
            },
        )
    }
    state.pairPrintFailure?.let { failure ->
        AlertDialog(
            onDismissRequest = onCancelPairPrint,
            title = { Text("${failure.side.label}打印中断") },
            text = { Text(failure.reason) },
            confirmButton = {
                Button(onClick = onRetryPairPrint) { Text("重试${failure.side.label}") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onCancelPairPrint) { Text("取消") }
                    TextButton(onClick = onSkipFailedPairSide) { Text("跳过") }
                }
            },
        )
    }
    if (showDisconnectedDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectedDialog = false },
            title = { Text("打印机未连接") },
            text = { Text("请先连接打印设备后再开始打印。") },
            confirmButton = {
                Button(
                    onClick = {
                        showDisconnectedDialog = false
                        onOpenDevices()
                    },
                ) { Text("连接打印机") }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectedDialog = false }) { Text("取消") }
            },
        )
    }
    if (showConnectingDialog) {
        AlertDialog(
            onDismissRequest = { showConnectingDialog = false },
            title = { Text("正在连接打印机") },
            text = { Text("正在连接打印机，请稍候。连接完成前不会启动打印任务。") },
            confirmButton = {
                TextButton(onClick = { showConnectingDialog = false }) { Text("知道了") }
            },
        )
    }
    if (showPaperSizeDialog) {
        PaperSizeDialog(
            settings = state.settings,
            renderedLengthMm = state.preview?.paperLengthMm,
            onSettingsChange = onSettingsChange,
            onDismiss = { showPaperSizeDialog = false },
        )
    }
    if (showTemplates) {
        ModalBottomSheet(onDismissRequest = { showTemplates = false }) {
            TemplateList(
                templates = state.templates,
                activeTemplateId = state.activeTemplateId,
                onLoad = {
                    onLoadTemplate(it)
                    showTemplates = false
                },
                onRename = onRenameTemplate,
                onDelete = onDeleteTemplate,
            )
        }
    }
    if (showSaveDialog) {
        TemplateNameDialog(
            title = if (saveDialogIsCopy) "另存为新模板" else "保存模板",
            value = templateName,
            onValueChange = { templateName = TemplateNameRules.limit(it) },
            onDismiss = { showSaveDialog = false },
            onSave = {
                onSaveTemplate(TemplateNameRules.normalize(templateName))
                showSaveDialog = false
            },
        )
    }
    if (showSaveChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showSaveChoiceDialog = false },
            title = { Text("保存模板") },
            text = {
                Text(
                    "当前模板：${state.activeTemplateName ?: "未命名模板"}\n\n" +
                        "更新会保留模板 ID，并用当前编辑内容覆盖该模板。",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateCurrentTemplate()
                        showSaveChoiceDialog = false
                    },
                ) { Text("更新当前模板") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { showSaveChoiceDialog = false }) { Text("取消") }
                    TextButton(
                        onClick = {
                            templateName = defaultTemplateName(state.documentMode)
                            saveDialogIsCopy = true
                            showSaveChoiceDialog = false
                            showSaveDialog = true
                        },
                    ) { Text("另存为") }
                }
            },
        )
    }
}

@Composable
private fun PhoneEditorWorkspace(
    state: MainUiState,
    selectedTool: EditorTool,
    onToolSelected: (EditorTool) -> Unit,
    panelSnap: EditorPanelSnap,
    panelFraction: Float,
    onPanelFractionChange: (Float) -> Unit,
    onPanelDragStopped: (Float) -> Unit,
    onTogglePreviewSpace: () -> Unit,
    focusPreview: Boolean,
    onFocusPreview: () -> Unit,
    onContinueEditing: () -> Unit,
    onSettingsChange: ((PrintSettings) -> PrintSettings) -> Unit,
    onSelectCoupletSide: (CoupletSide) -> Unit,
    onAlignPairFooters: () -> Unit,
    previewTransformState: PreviewTransformState,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val textScrollState = rememberScrollState()
        val borderScrollState = rememberScrollState()
        val layoutScrollState = rememberScrollState()
        val moreScrollState = rememberScrollState()
        val selectedScrollState = when (selectedTool) {
            EditorTool.TEXT -> textScrollState
            EditorTool.BORDER -> borderScrollState
            EditorTool.LAYOUT -> layoutScrollState
            EditorTool.FOOTER -> moreScrollState
        }
        val density = LocalDensity.current
        val workspaceHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        Column(Modifier.fillMaxSize()) {
            CoupletPreview(
                rendered = state.preview,
                isRendering = state.isRendering,
                onFullscreen = if (focusPreview) null else onFocusPreview,
                transformState = previewTransformState,
                showControls = false,
                documentMode = state.documentMode,
                pairRendered = state.pairPreview,
                selectedSide = state.pairDocument?.selectedSide ?: CoupletSide.LEFT,
                onSideSelected = onSelectCoupletSide,
                onPersonBlockMove = { deltaXMm, deltaYMm ->
                    onSettingsChange { current ->
                        current.copy(
                            personBlock = current.personBlock.movedBy(deltaXMm, deltaYMm),
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (focusPreview) 1f else 1f - panelFraction),
            )
            if (!focusPreview) {
                EmbeddedEditorPanel(
                    selectedTool = selectedTool,
                    onToolSelected = onToolSelected,
                    state = state,
                    onSettingsChange = onSettingsChange,
                    onAlignPairFooters = onAlignPairFooters,
                    scrollState = selectedScrollState,
                    panelMode = panelSnap.mode,
                    onTogglePreviewSpace = onTogglePreviewSpace,
                    panelFraction = panelFraction,
                    workspaceHeightPx = workspaceHeightPx,
                    onPanelFractionChange = onPanelFractionChange,
                    onPanelDragStopped = onPanelDragStopped,
                    modifier = Modifier.fillMaxWidth().weight(panelFraction),
                )
            }
        }

        if (focusPreview) {
            Button(
                onClick = onContinueEditing,
                modifier = Modifier.align(Alignment.BottomCenter).padding(18.dp),
            ) {
                Text("继续编辑")
            }
        }
    }
}

@Composable
private fun PreviewFocusTopBar(
    state: MainUiState,
    previewTransformState: PreviewTransformState,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 3.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(34.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "专注预览",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    state.preview?.let {
                        "${format(it.paperWidthMm)} × ${format(it.paperLengthMm)} mm"
                    } ?: "计算尺寸中",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            CompactPreviewControls(
                transformState = previewTransformState,
                modifier = Modifier.align(Alignment.CenterHorizontally).height(40.dp),
            )
        }
    }
}

@Composable
private fun EditorTopBar(
    state: MainUiState,
    onTemplates: () -> Unit,
    onDevice: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    previewTransformState: PreviewTransformState,
    onFullscreen: () -> Unit,
    onPaperSize: () -> Unit,
    onDocumentMode: () -> Unit,
) {
    Surface(
        shadowElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                HeaderCapsule(
                    text = "模板",
                    onClick = onTemplates,
                    modifier = Modifier.width(54.dp).height(44.dp),
                )
                val selectedSide = state.pairDocument?.selectedSide
                HeaderCapsule(
                    text = if (state.documentMode == DocumentMode.PAIR) {
                        "双联·${selectedSide?.label ?: "左联"}⌄"
                    } else {
                        "单联⌄"
                    },
                    onClick = onDocumentMode,
                    modifier = Modifier
                        .width(if (state.documentMode == DocumentMode.PAIR) 88.dp else 66.dp)
                        .height(44.dp),
                    emphasized = true,
                )
                val statusColor = when (state.connectionStatus) {
                    ConnectionStatus.CONNECTED -> ColorConnected
                    ConnectionStatus.CONNECTING, ConnectionStatus.DISCONNECTING -> ColorConnecting
                    ConnectionStatus.DISCONNECTED, ConnectionStatus.ERROR -> ColorDisconnected
                }
                val statusText = when (state.connectionStatus) {
                    ConnectionStatus.CONNECTED -> state.currentDevice?.displayName ?: "已连接"
                    ConnectionStatus.CONNECTING -> "正在连接"
                    ConnectionStatus.DISCONNECTING -> "正在断开"
                    ConnectionStatus.DISCONNECTED, ConnectionStatus.ERROR -> {
                        state.lastDevice?.displayName?.let { "$it 未连接" } ?: "打印机未连接"
                    }
                }
                Surface(
                    modifier = Modifier.width(108.dp).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        IconButton(
                            onClick = onUndo,
                            enabled = state.canUndo,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        ) { Text("↶", fontSize = 20.sp) }
                        IconButton(
                            onClick = onRedo,
                            enabled = state.canRedo,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        ) { Text("↷", fontSize = 20.sp) }
                    }
                }
                HeaderCapsule(
                    text = "● $statusText",
                    onClick = onDevice,
                    modifier = Modifier.weight(1f).widthIn(max = 78.dp).height(44.dp),
                    contentColor = statusColor,
                    compactText = true,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(42.dp).padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val rendered = state.preview
                HeaderCapsule(
                    text = if (rendered == null) "尺寸…" else
                        "${format(rendered.paperWidthMm)}×${format(rendered.paperLengthMm)} mm ›",
                    onClick = onPaperSize,
                    modifier = Modifier.weight(1f).height(40.dp),
                )
                Surface(
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    CompactPreviewControls(
                        transformState = previewTransformState,
                        onFullscreen = onFullscreen,
                        modifier = Modifier.height(40.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderCapsule(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    emphasized: Boolean = false,
    compactText: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (emphasized) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = contentColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = if (compactText) {
                    MaterialTheme.typography.labelMedium
                } else {
                    MaterialTheme.typography.labelLarge
                },
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = if (compactText) 3.dp else 4.dp),
            )
        }
    }
}

@Composable
private fun EditorBottomBar(
    state: MainUiState,
    onSave: () -> Unit,
    onPrint: () -> Unit,
    onSettings: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            if (state.isPrinting) {
                state.printingSide?.let { side ->
                    Text(
                        "正在打印：${side.label}  ${if (side == CoupletSide.LEFT) "1 / 2" else "2 / 2"}",
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                    )
                }
                LinearProgressIndicator(
                    progress = { state.printProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(onClick = onSave, modifier = Modifier.weight(1f)) {
                    Text("保存")
                }
                Button(
                    onClick = onPrint,
                    enabled = !state.isPrinting,
                    modifier = Modifier.weight(1.5f),
                ) { Text(if (state.isPrinting) "发送中" else "打印") }
                OutlinedButton(onClick = onSettings, modifier = Modifier.weight(1f)) {
                    Text("设置")
                }
            }
        }
    }
}

@Composable
private fun PrintConfirmation(state: MainUiState, onCancel: () -> Unit, onStart: () -> Unit) {
    val rendered = state.preview
    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (state.documentMode == DocumentMode.PAIR) "打印双联" else "打印预览",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        InfoRow("设备", state.currentDevice?.displayName ?: "未连接")
        if (state.documentMode == DocumentMode.PAIR) {
            val pair = state.pairPreview
            InfoRow(
                "左联",
                pair?.left?.let { "${format(it.paperWidthMm)} × ${format(it.paperLengthMm)} mm" } ?: "计算中",
            )
            InfoRow(
                "右联",
                pair?.right?.let { "${format(it.paperWidthMm)} × ${format(it.paperLengthMm)} mm" } ?: "计算中",
            )
        } else {
            InfoRow(
                "尺寸",
                rendered?.let { "${format(it.paperWidthMm)} × ${format(it.paperLengthMm)} mm" } ?: "计算中",
            )
        }
        InfoRow("纸宽", "${format(state.settings.paperWidthMm)} mm")
        InfoRow("纸长", rendered?.let { "${format(it.paperLengthMm)} mm" } ?: "自动")
        InfoRow("打印浓度", state.settings.density.toString())
        InfoRow("打印速度", "${state.settings.speedInchesPerSecond} ips")
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("取消") }
            Button(
                onClick = onStart,
                enabled = state.connectionStatus == ConnectionStatus.CONNECTED,
                modifier = Modifier.weight(1f),
            ) { Text("开始打印") }
        }
    }
}

@Composable
private fun PaperSizeDialog(
    settings: PrintSettings,
    renderedLengthMm: Float?,
    onSettingsChange: ((PrintSettings) -> PrintSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("纸张尺寸") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CompactNumberControl(
                    title = "纸张宽度",
                    value = settings.paperWidthMm,
                    unit = "mm",
                    valueRange = 30f..110f,
                    step = 1f,
                    onValueChange = { value -> onSettingsChange { it.copy(paperWidthMm = value) } },
                )
                SwitchRow(
                    title = "自动长度",
                    subtitle = renderedLengthMm?.let { "实际打印长度 ${format(it)} mm" },
                    checked = settings.autoPaperLength,
                    onCheckedChange = { enabled -> onSettingsChange { it.copy(autoPaperLength = enabled) } },
                )
                if (!settings.autoPaperLength) {
                    CompactNumberControl(
                        title = "纸张长度",
                        value = settings.paperLengthMm,
                        unit = "mm",
                        valueRange = 50f..1500f,
                        step = 5f,
                        onValueChange = { value -> onSettingsChange { it.copy(paperLengthMm = value) } },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        },
    )
}

@Composable
private fun TemplateList(
    templates: List<CoupletTemplate>,
    activeTemplateId: String?,
    onLoad: (CoupletTemplate) -> Unit,
    onRename: (CoupletTemplate, String) -> Unit,
    onDelete: (CoupletTemplate) -> Unit,
) {
    var renameTarget by remember { mutableStateOf<CoupletTemplate?>(null) }
    var renameName by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<CoupletTemplate?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("本地模板", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (templates.isEmpty()) {
            item { Text("尚未保存模板", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(templates, key = { it.id }) { template ->
            val isActive = template.id == activeTemplateId
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            template.name,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${template.documentMode.label} · ${format(template.settings.paperWidthMm)} mm · " +
                                template.settings.border.style.label,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (isActive) {
                            Text(
                                "当前模板",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Box {
                        var menuExpanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuExpanded = true }) { Text("⋮") }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("重命名") },
                                onClick = {
                                    menuExpanded = false
                                    renameTarget = template
                                    renameName = template.name
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("删除") },
                                onClick = {
                                    menuExpanded = false
                                    deleteTarget = template
                                },
                            )
                        }
                    }
                    Button(onClick = { onLoad(template) }) { Text("载入") }
                }
            }
        }
    }
    renameTarget?.let { target ->
        TemplateNameDialog(
            title = "重命名模板",
            value = renameName,
            onValueChange = { renameName = TemplateNameRules.limit(it) },
            onDismiss = { renameTarget = null },
            onSave = {
                onRename(target, TemplateNameRules.normalize(renameName))
                renameTarget = null
            },
        )
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除模板？") },
            text = { Text("将删除“${target.name}”。删除后无法恢复。") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(target)
                        deleteTarget = null
                    },
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun TemplateNameDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val normalized = TemplateNameRules.normalize(value)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text("模板名称") },
                supportingText = {
                    Text(
                        if (normalized.isEmpty()) {
                            "请输入模板名称"
                        } else {
                            "最多 ${TemplateNameRules.MAX_CODE_POINTS} 个字符"
                        },
                    )
                },
                isError = normalized.isEmpty(),
                singleLine = true,
            )
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = TemplateNameRules.isValid(value),
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private val ColorConnected = Color(0xFF2E7D32)
private val ColorConnecting = Color(0xFFEF6C00)
private val ColorDisconnected = Color(0xFFC62828)

private fun format(value: Float): String = String.format(Locale.CHINA, "%.1f", value)

private fun defaultTemplateName(documentMode: DocumentMode): String =
    "${documentMode.label} ${SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())}"
