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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wanlian.printer.MainUiState
import com.wanlian.printer.model.PrintDirection
import com.wanlian.printer.model.PrintSettings
import com.wanlian.printer.printing.RenderedBitmap
import com.wanlian.printer.ui.components.ChoiceChips
import com.wanlian.printer.ui.components.SettingSlider
import com.wanlian.printer.ui.components.SwitchRow
import java.util.Locale

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
) {
    var advancedExpanded by remember { mutableStateOf(false) }
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
                "测试功能已从编辑首页移到这里。先执行白字极性测试，确认黑色背景不转印。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TestPreview(state.polarityTestPreview, height = 220)
            Button(
                onClick = onPrintPolarityTest,
                enabled = !state.isPrinting,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("打印白字极性测试") }
            TestPreview(state.testPreview, height = 280)
            OutlinedButton(
                onClick = onPrintTest,
                enabled = !state.isPrinting,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("打印完整测试页") }
        }
    }
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
