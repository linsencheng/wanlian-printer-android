package com.wanlian.printer.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wanlian.printer.MainUiState
import com.wanlian.printer.model.BorderPosition
import com.wanlian.printer.model.PrintDirection
import com.wanlian.printer.model.PrintSettings
import com.wanlian.printer.model.TextHorizontalAlignment
import com.wanlian.printer.model.TextWeight
import com.wanlian.printer.printing.RenderedBitmap
import com.wanlian.printer.ui.components.BorderPicker
import com.wanlian.printer.ui.components.ChoiceChips
import com.wanlian.printer.ui.components.CompactNumberControl
import com.wanlian.printer.ui.components.SwitchRow
import java.util.Locale

enum class EditorTool(val label: String) {
    TEXT("文本"),
    BORDER("边框"),
    LAYOUT("排版"),
    MORE("更多"),
}

@Composable
fun EditorSettingsSheet(
    tool: EditorTool,
    state: MainUiState,
    onSettingsChange: ((PrintSettings) -> PrintSettings) -> Unit,
    onOpenSettings: () -> Unit,
    onPrintTest: () -> Unit,
    onPrintPolarityTest: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f)
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${tool.label}设置",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onDone) { Text("完成") }
        }
        HorizontalDivider()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (tool) {
                EditorTool.TEXT -> TextSettingsContent(
                    settings = state.settings,
                    onSettingsChange = onSettingsChange,
                )
                EditorTool.BORDER -> BorderSettingsContent(
                    settings = state.settings,
                    onSettingsChange = onSettingsChange,
                )
                EditorTool.LAYOUT -> LayoutSettingsContent(
                    settings = state.settings,
                    rendered = state.preview,
                    onSettingsChange = onSettingsChange,
                )
                EditorTool.MORE -> MoreSettingsContent(
                    state = state,
                    onSettingsChange = onSettingsChange,
                    onOpenSettings = onOpenSettings,
                    onPrintTest = onPrintTest,
                    onPrintPolarityTest = onPrintPolarityTest,
                )
            }
        }
    }
}

@Composable
fun TextSettingsContent(
    settings: PrintSettings,
    onSettingsChange: ((PrintSettings) -> PrintSettings) -> Unit,
) {
    OutlinedTextField(
        value = settings.text,
        onValueChange = { text -> onSettingsChange { it.copy(text = text) } },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("挽联内容") },
        supportingText = { Text("每个输入行生成一列竖排文字") },
        minLines = 3,
        maxLines = 6,
    )
    InfoValueRow("字体", "系统 Serif")
    SwitchRow(
        title = "自动字号",
        subtitle = "根据纸宽、列数与边框自动计算",
        checked = settings.autoFontSize,
        onCheckedChange = { checked -> onSettingsChange { it.copy(autoFontSize = checked) } },
    )
    CompactNumberControl(
        title = "字体大小",
        value = settings.fontSizeDots,
        unit = "dots",
        valueRange = 24f..300f,
        step = 2f,
        decimals = 0,
        enabled = !settings.autoFontSize,
        onValueChange = { value -> onSettingsChange { it.copy(fontSizeDots = value) } },
    )
    CompactNumberControl(
        title = "字间距",
        value = settings.characterSpacingDots,
        unit = "dots",
        valueRange = 0f..120f,
        step = 2f,
        decimals = 0,
        onValueChange = { value -> onSettingsChange { it.copy(characterSpacingDots = value) } },
    )
    Text("字体粗细", style = MaterialTheme.typography.labelLarge)
    ChoiceChips(
        values = TextWeight.entries,
        selected = settings.textWeight,
        label = { it.label },
        onSelected = { weight -> onSettingsChange { it.copy(textWeight = weight) } },
    )
    CompactNumberControl(
        title = "顶部空白",
        value = settings.topMarginMm,
        unit = "mm",
        valueRange = 0f..100f,
        step = 1f,
        onValueChange = { value -> onSettingsChange { it.copy(topMarginMm = value) } },
    )
    CompactNumberControl(
        title = "底部空白",
        value = settings.bottomMarginMm,
        unit = "mm",
        valueRange = 0f..100f,
        step = 1f,
        onValueChange = { value -> onSettingsChange { it.copy(bottomMarginMm = value) } },
    )
}

@Composable
fun BorderSettingsContent(
    settings: PrintSettings,
    onSettingsChange: ((PrintSettings) -> PrintSettings) -> Unit,
) {
    Text("选择样式", style = MaterialTheme.typography.labelLarge)
    BorderPicker(
        selected = settings.border.style,
        settings = settings.border,
        onSelected = { style ->
            onSettingsChange { current -> current.copy(border = current.border.copy(style = style)) }
        },
    )
    HorizontalDivider()
    Text("边框位置", style = MaterialTheme.typography.labelLarge)
    ChoiceChips(
        values = BorderPosition.entries,
        selected = settings.border.position,
        label = { it.label },
        onSelected = { position ->
            onSettingsChange { current -> current.copy(border = current.border.copy(position = position)) }
        },
    )
    CompactNumberControl(
        title = "边框宽度",
        value = settings.border.widthMm,
        unit = "mm",
        valueRange = 2f..15f,
        step = 0.5f,
        onValueChange = { value ->
            onSettingsChange { current -> current.copy(border = current.border.copy(widthMm = value)) }
        },
    )
    CompactNumberControl(
        title = "距纸张边缘",
        value = settings.border.edgeInsetMm,
        unit = "mm",
        valueRange = 0f..15f,
        step = 0.5f,
        onValueChange = { value ->
            onSettingsChange { current -> current.copy(border = current.border.copy(edgeInsetMm = value)) }
        },
    )
    CompactNumberControl(
        title = "边框线宽",
        value = settings.border.strokeWidthMm,
        unit = "mm",
        valueRange = 0.2f..2.5f,
        step = 0.1f,
        onValueChange = { value ->
            onSettingsChange { current -> current.copy(border = current.border.copy(strokeWidthMm = value)) }
        },
    )
    CompactNumberControl(
        title = "纹样单元高度",
        value = settings.border.patternUnitHeightMm,
        unit = "mm",
        valueRange = 6f..30f,
        step = 1f,
        onValueChange = { value ->
            onSettingsChange {
                current -> current.copy(border = current.border.copy(patternUnitHeightMm = value))
            }
        },
    )
}

@Composable
fun LayoutSettingsContent(
    settings: PrintSettings,
    rendered: RenderedBitmap?,
    onSettingsChange: ((PrintSettings) -> PrintSettings) -> Unit,
) {
    CompactNumberControl(
        title = "纸张宽度",
        value = settings.paperWidthMm,
        unit = "mm",
        valueRange = 30f..110f,
        step = 1f,
        onValueChange = { value -> onSettingsChange { it.copy(paperWidthMm = value) } },
    )
    SwitchRow(
        title = "自动计算纸张长度",
        subtitle = rendered?.let { "当前 ${format(it.paperLengthMm, 1)} mm" },
        checked = settings.autoPaperLength,
        onCheckedChange = { checked -> onSettingsChange { it.copy(autoPaperLength = checked) } },
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
    Text("内容位置", style = MaterialTheme.typography.labelLarge)
    ChoiceChips(
        values = TextHorizontalAlignment.entries,
        selected = settings.textAlignment,
        label = { it.label },
        onSelected = { alignment -> onSettingsChange { it.copy(textAlignment = alignment) } },
    )
    CompactNumberControl(
        title = "顶部留白",
        value = settings.topMarginMm,
        unit = "mm",
        valueRange = 0f..100f,
        step = 1f,
        onValueChange = { value -> onSettingsChange { it.copy(topMarginMm = value) } },
    )
    CompactNumberControl(
        title = "底部留白",
        value = settings.bottomMarginMm,
        unit = "mm",
        valueRange = 0f..100f,
        step = 1f,
        onValueChange = { value -> onSettingsChange { it.copy(bottomMarginMm = value) } },
    )
    CompactNumberControl(
        title = "正文与边框间距",
        value = settings.border.textGapMm,
        unit = "mm",
        valueRange = 0f..20f,
        step = 0.5f,
        onValueChange = { value ->
            onSettingsChange { current -> current.copy(border = current.border.copy(textGapMm = value)) }
        },
    )
}

@Composable
private fun MoreSettingsContent(
    state: MainUiState,
    onSettingsChange: ((PrintSettings) -> PrintSettings) -> Unit,
    onOpenSettings: () -> Unit,
    onPrintTest: () -> Unit,
    onPrintPolarityTest: () -> Unit,
) {
    var showAdvanced by remember { mutableStateOf(false) }
    CompactNumberControl(
        title = "打印浓度",
        value = state.settings.density.toFloat(),
        unit = "",
        valueRange = 0f..15f,
        step = 1f,
        decimals = 0,
        onValueChange = { value -> onSettingsChange { it.copy(density = value.toInt()) } },
    )
    CompactNumberControl(
        title = "打印速度",
        value = state.settings.speedInchesPerSecond,
        unit = "ips",
        valueRange = 1f..6f,
        step = 0.5f,
        onValueChange = { value -> onSettingsChange { it.copy(speedInchesPerSecond = value) } },
    )
    Text("打印方向", style = MaterialTheme.typography.labelLarge)
    ChoiceChips(
        values = PrintDirection.entries,
        selected = state.settings.printDirection,
        label = { it.label },
        onSelected = { direction -> onSettingsChange { it.copy(printDirection = direction) } },
    )
    SwitchRow(
        title = "反转打印",
        subtitle = "黑布白带正常情况下保持关闭",
        checked = state.settings.reversePrinting,
        onCheckedChange = { checked -> onSettingsChange { it.copy(reversePrinting = checked) } },
    )
    OutlinedButton(
        onClick = { showAdvanced = !showAdvanced },
        modifier = Modifier.fillMaxWidth(),
    ) { Text(if (showAdvanced) "收起高级 TSPL 参数" else "高级 TSPL 参数") }
    if (showAdvanced) {
        CompactNumberControl(
            title = "BITMAP 分块",
            value = state.settings.bitmapChunkSize.toFloat(),
            unit = "bytes",
            valueRange = 256f..4096f,
            step = 256f,
            decimals = 0,
            onValueChange = { value ->
                onSettingsChange { it.copy(bitmapChunkSize = value.toInt()) }
            },
        )
        CompactNumberControl(
            title = "分块间延迟",
            value = state.settings.chunkDelayMs.toFloat(),
            unit = "ms",
            valueRange = 0f..50f,
            step = 1f,
            decimals = 0,
            onValueChange = { value ->
                onSettingsChange { it.copy(chunkDelayMs = value.toLong()) }
            },
        )
    }
    HorizontalDivider()
    Text("打印机测试", style = MaterialTheme.typography.labelLarge)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onPrintPolarityTest,
            enabled = !state.isPrinting,
            modifier = Modifier.weight(1f),
        ) { Text("极性测试") }
        OutlinedButton(
            onClick = onPrintTest,
            enabled = !state.isPrinting,
            modifier = Modifier.weight(1f),
        ) { Text("完整测试") }
    }
    Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
        Text("打开完整设置")
    }
}

@Composable
private fun InfoValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

private fun format(value: Float, decimals: Int): String =
    String.format(Locale.CHINA, "%.${decimals}f", value)
