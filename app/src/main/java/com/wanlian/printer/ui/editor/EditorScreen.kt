package com.wanlian.printer.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wanlian.printer.MainUiState
import com.wanlian.printer.model.ConnectionStatus
import com.wanlian.printer.model.CoupletTemplate
import com.wanlian.printer.model.PrintSettings
import com.wanlian.printer.ui.components.CoupletPreview
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    onLoadTemplate: (CoupletTemplate) -> Unit,
    onDeleteTemplate: (CoupletTemplate) -> Unit,
    onPrint: () -> Unit,
    onPrintTest: () -> Unit,
    onPrintPolarityTest: () -> Unit,
) {
    var showPrintConfirmation by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }
    var showFullScreen by remember { mutableStateOf(false) }
    var activeTool by remember { mutableStateOf<EditorTool?>(null) }
    var templateName by remember { mutableStateOf("") }

    if (showFullScreen) {
        FullScreenPreviewScreen(
            rendered = state.preview,
            isRendering = state.isRendering,
            onBack = { showFullScreen = false },
            onPrint = {
                showFullScreen = false
                showPrintConfirmation = true
            },
        )
        return
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val twoPane = maxWidth >= 700.dp
        Scaffold(
            topBar = {
                EditorTopBar(
                    state = state,
                    onTemplates = { showTemplates = true },
                    onDevice = onOpenDevices,
                    onUndo = onUndo,
                    onRedo = onRedo,
                )
            },
            bottomBar = {
                EditorBottomBar(
                    state = state,
                    showToolBar = !twoPane,
                    onTool = { activeTool = it },
                    onSave = {
                        templateName = defaultTemplateName()
                        showSaveDialog = true
                    },
                    onPrint = { showPrintConfirmation = true },
                    onSettings = onOpenSettings,
                )
            },
        ) { contentPadding ->
            if (twoPane) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(contentPadding).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CoupletPreview(
                        rendered = state.preview,
                        isRendering = state.isRendering,
                        onFullscreen = { showFullScreen = true },
                        modifier = Modifier.weight(0.65f).fillMaxSize(),
                    )
                    EditorToolPanel(
                        settings = state.settings,
                        rendered = state.preview,
                        onSettingsChange = onSettingsChange,
                        modifier = Modifier.weight(0.35f).fillMaxSize(),
                    )
                }
            } else {
                CoupletPreview(
                    rendered = state.preview,
                    isRendering = state.isRendering,
                    onFullscreen = { showFullScreen = true },
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                )
            }
        }
    }

    activeTool?.let { tool ->
        ModalBottomSheet(onDismissRequest = { activeTool = null }) {
            EditorSettingsSheet(
                tool = tool,
                state = state,
                onSettingsChange = onSettingsChange,
                onOpenSettings = {
                    activeTool = null
                    onOpenSettings()
                },
                onPrintTest = onPrintTest,
                onPrintPolarityTest = onPrintPolarityTest,
                onDone = { activeTool = null },
            )
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
    if (showTemplates) {
        ModalBottomSheet(onDismissRequest = { showTemplates = false }) {
            TemplateList(
                templates = state.templates,
                onLoad = {
                    onLoadTemplate(it)
                    showTemplates = false
                },
                onDelete = onDeleteTemplate,
            )
        }
    }
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("保存当前模板") },
            text = {
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("模板名称") },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveTemplate(templateName)
                        showSaveDialog = false
                    },
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun EditorTopBar(
    state: MainUiState,
    onTemplates: () -> Unit,
    onDevice: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
) {
    Surface(shadowElevation = 3.dp) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onTemplates) { Text("模板") }
                Text(
                    "单联",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                val connected = state.connectionStatus == ConnectionStatus.CONNECTED
                TextButton(
                    onClick = onDevice,
                    modifier = Modifier.widthIn(max = 150.dp),
                ) {
                    Text(
                        "${if (connected) "●" else "○"} " +
                            (state.currentDevice?.displayName ?: state.lastDevice?.displayName ?: "设备"),
                        color = if (connected) ColorConnected else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(80.dp))
                val rendered = state.preview
                Text(
                    if (rendered == null) "计算尺寸中" else
                        "${format(rendered.paperWidthMm)} × ${format(rendered.paperLengthMm)} mm",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onUndo,
                    enabled = state.canUndo,
                    modifier = Modifier.width(40.dp),
                ) { Text("↶", style = MaterialTheme.typography.titleLarge) }
                TextButton(
                    onClick = onRedo,
                    enabled = state.canRedo,
                    modifier = Modifier.width(40.dp),
                ) { Text("↷", style = MaterialTheme.typography.titleLarge) }
            }
        }
    }
}

@Composable
private fun EditorBottomBar(
    state: MainUiState,
    showToolBar: Boolean,
    onTool: (EditorTool) -> Unit,
    onSave: () -> Unit,
    onPrint: () -> Unit,
    onSettings: () -> Unit,
) {
    Surface(shadowElevation = 8.dp) {
        Column(Modifier.fillMaxWidth()) {
            if (state.isPrinting) {
                LinearProgressIndicator(
                    progress = { state.printProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (showToolBar) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EditorTool.entries.forEach { tool ->
                        TextButton(onClick = { onTool(tool) }, modifier = Modifier.weight(1f)) {
                            Text(tool.label)
                        }
                    }
                }
                HorizontalDivider()
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
        Text("打印预览", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        InfoRow("设备", state.currentDevice?.displayName ?: "未连接")
        InfoRow(
            "尺寸",
            rendered?.let { "${format(it.paperWidthMm)} × ${format(it.paperLengthMm)} mm" } ?: "计算中",
        )
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
private fun TemplateList(
    templates: List<CoupletTemplate>,
    onLoad: (CoupletTemplate) -> Unit,
    onDelete: (CoupletTemplate) -> Unit,
) {
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
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(template.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${format(template.settings.paperWidthMm)} mm · ${template.settings.border.style.label}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    TextButton(onClick = { onDelete(template) }) { Text("删除") }
                    Button(onClick = { onLoad(template) }) { Text("载入") }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private val ColorConnected = Color(0xFF2E7D32)

private fun format(value: Float): String = String.format(Locale.CHINA, "%.1f", value)

private fun defaultTemplateName(): String =
    "挽联 ${SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date())}"
