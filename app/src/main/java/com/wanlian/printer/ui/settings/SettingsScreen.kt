package com.wanlian.printer.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.unit.dp
import com.wanlian.printer.MainUiState
import com.wanlian.printer.model.ConnectionStatus
import com.wanlian.printer.model.PrintDirection
import com.wanlian.printer.model.PrintSettings
import com.wanlian.printer.printing.RenderedBitmap
import com.wanlian.printer.ui.components.ChoiceChips
import com.wanlian.printer.ui.components.SettingSlider
import com.wanlian.printer.ui.components.SwitchRow
import java.util.Locale
import java.text.DateFormat
import java.util.Date
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onOpenDevices: () -> Unit,
    onSettingsChange: ((PrintSettings) -> PrintSettings) -> Unit,
    onAutoReconnectChange: (Boolean) -> Unit,
    onPrintTest: () -> Unit,
    onPrintPolarityTest: () -> Unit,
    onApplyPrinterSettings: () -> Unit,
    onRefreshDiagnosticLogs: () -> Unit,
    onClearDiagnosticLogs: () -> Unit,
) {
    var advancedExpanded by remember { mutableStateOf(false) }
    var showDiagnosticLogs by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionTitle("打印设置")
            SettingsCard {
                InfoRow("打印分辨率", "203 DPI")
                SettingSlider(
                    title = "打印浓度",
                    value = state.settings.density.toFloat(),
                    valueRange = 0f..15f,
                    steps = 14,
                    valueText = state.settings.density.toString(),
                    onValueChange = { value -> onSettingsChange { it.copy(density = value.toInt()) } },
                )
                SettingSlider(
                    title = "打印速度",
                    value = state.settings.speedInchesPerSecond,
                    valueRange = 1f..6f,
                    steps = 9,
                    valueText = "${format(state.settings.speedInchesPerSecond, 1)} ips",
                    onValueChange = { value ->
                        onSettingsChange { it.copy(speedInchesPerSecond = value) }
                    },
                )
                PrinterSettingsDeliveryStatus(state)
                Button(
                    onClick = onApplyPrinterSettings,
                    enabled = state.connectionStatus == ConnectionStatus.CONNECTED &&
                        !state.isPrinting && !state.isApplyingPrinterSettings,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.isApplyingPrinterSettings) "正在写入打印机…" else "发送浓度和速度到打印机")
                }
                Text(
                    "正式打印时也会在每一联图像前再次发送 DENSITY 和 SPEED。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("打印方向", style = MaterialTheme.typography.bodyMedium)
                ChoiceChips(
                    values = PrintDirection.entries,
                    selected = state.settings.printDirection,
                    label = { it.label },
                    onSelected = { direction -> onSettingsChange { it.copy(printDirection = direction) } },
                )
                SwitchRow(
                    title = "反转打印",
                    subtitle = "反转 PRINT/NOT_PRINT 区域；黑布白带通常保持关闭",
                    checked = state.settings.reversePrinting,
                    onCheckedChange = { checked ->
                        onSettingsChange { it.copy(reversePrinting = checked) }
                    },
                )
            }

            SectionTitle("App 设置")
            SettingsCard {
                SwitchRow(
                    title = "启动时自动连接上次打印机",
                    checked = state.autoReconnect,
                    onCheckedChange = onAutoReconnectChange,
                )
                OutlinedButton(onClick = onOpenDevices, modifier = Modifier.fillMaxWidth()) {
                    Text("管理打印设备")
                }
            }

            SectionTitle("高级设置")
            OutlinedButton(
                onClick = { advancedExpanded = !advancedExpanded },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (advancedExpanded) "收起高级参数" else "展开高级参数") }
            if (advancedExpanded) {
                SettingsCard {
                    SettingSlider(
                        title = "BITMAP 分块大小",
                        value = state.settings.bitmapChunkSize.toFloat(),
                        valueRange = 256f..4096f,
                        valueText = "${state.settings.bitmapChunkSize} bytes",
                        onValueChange = { value ->
                            val aligned = (value.toInt() / 256 * 256).coerceIn(256, 4096)
                            onSettingsChange { it.copy(bitmapChunkSize = aligned) }
                        },
                    )
                    SettingSlider(
                        title = "分块间延迟",
                        value = state.settings.chunkDelayMs.toFloat(),
                        valueRange = 0f..50f,
                        valueText = "${state.settings.chunkDelayMs} ms",
                        onValueChange = { value ->
                            onSettingsChange { it.copy(chunkDelayMs = value.toLong()) }
                        },
                    )
                    Text(
                        "默认 1024 bytes / 8 ms 已通过当前实机链路验证。BLE 内部仍按 MTU 再分片。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider()
            SectionTitle("打印机测试")
            Text(
                "如果字迹发淡或覆盖不完整，先发送设置，再打印下面的小型覆盖测试。建议从浓度 12 / 速度 1.5 开始；仍发淡可试浓度 15 / 速度 1.0。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TestPreview(state.polarityTestPreview, height = 220)
            Button(
                onClick = onPrintPolarityTest,
                enabled = !state.isPrinting,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("应用当前设置并打印覆盖测试") }
            TestPreview(state.testPreview, height = 280)
            OutlinedButton(
                onClick = onPrintTest,
                enabled = !state.isPrinting,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("打印完整测试页") }

            HorizontalDivider()
            SectionTitle("诊断日志")
            Text(
                "记录蓝牙连接、位图上传进度、断开回调和底层异常。日志保存在 App 内，重启后仍可查看。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = {
                    onRefreshDiagnosticLogs()
                    showDiagnosticLogs = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("查看打印与蓝牙日志") }
        }
    }

    if (showDiagnosticLogs) {
        AlertDialog(
            onDismissRequest = { showDiagnosticLogs = false },
            title = { Text("打印与蓝牙诊断日志") },
            text = {
                SelectionContainer {
                    Text(
                        text = state.diagnosticLogText.ifBlank { "暂无诊断日志" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 430.dp)
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(state.diagnosticLogText))
                    },
                    enabled = state.diagnosticLogText.isNotBlank(),
                ) { Text("复制全部") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onClearDiagnosticLogs) { Text("清空") }
                    TextButton(onClick = { showDiagnosticLogs = false }) { Text("关闭") }
                }
            },
        )
    }
}

@Composable
private fun PrinterSettingsDeliveryStatus(state: MainUiState) {
    val delivery = state.printerSettingsDelivery
    val currentDevice = state.currentDevice
    val currentDensity = state.settings.density.coerceIn(0, 15)
    val currentSpeed = state.settings.speedInchesPerSecond.coerceIn(1f, 6f)
    val matchesCurrent = delivery != null &&
        delivery.deviceAddress == currentDevice?.address &&
        delivery.settings.density == currentDensity &&
        abs(delivery.settings.speedInchesPerSecond - currentSpeed) < 0.01f
    val status = when {
        state.isApplyingPrinterSettings -> "正在通过蓝牙写入设置命令…"
        matchesCurrent -> {
            val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(delivery!!.sentAtEpochMs))
            "✓ 蓝牙写入成功（$time）\nDENSITY ${delivery.settings.density} · SPEED ${format(delivery.settings.speedInchesPerSecond, 1)} ips"
        }
        currentDevice == null -> "连接打印机后，可验证浓度和速度命令是否成功写入。"
        delivery != null -> "当前滑块值尚未发送到这台打印机。"
        else -> "尚未发送本组浓度和速度。"
    }
    Text(
        status,
        style = MaterialTheme.typography.bodyMedium,
        color = if (matchesCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        "“蓝牙写入成功”表示命令已交给打印机连接；此机型无可靠参数回读，因此请用覆盖测试确认实际热量和碳带覆盖。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
private fun TestPreview(rendered: RenderedBitmap?, height: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF282B2A))) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxWidth().height(height.dp).padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            rendered?.let {
                Image(
                    bitmap = it.previewBitmap.asImageBitmap(),
                    contentDescription = "打印测试预览",
                    modifier = Modifier.height((height - 20).dp).background(Color.Black),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private fun format(value: Float, decimals: Int): String =
    String.format(Locale.CHINA, "%.${decimals}f", value)
