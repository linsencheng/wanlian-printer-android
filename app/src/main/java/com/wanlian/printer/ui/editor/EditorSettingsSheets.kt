package com.wanlian.printer.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wanlian.printer.MainUiState
import com.wanlian.printer.model.BorderPosition
import com.wanlian.printer.model.ClosingTextBlockRules
import com.wanlian.printer.model.CutGuideStyle
import com.wanlian.printer.model.FlowerStyle
import com.wanlian.printer.model.FlowerAdjustmentLimits
import com.wanlian.printer.model.FooterLabelAdjustmentLimits
import com.wanlian.printer.model.FooterLabelPosition
import com.wanlian.printer.model.FooterTextOrientation
import com.wanlian.printer.model.FooterPerson
import com.wanlian.printer.model.DocumentMode
import com.wanlian.printer.model.PersonBlockHorizontalPreset
import com.wanlian.printer.model.PersonBlockSettings
import com.wanlian.printer.model.PersonLayout
import com.wanlian.printer.model.PersonLayoutRules
import com.wanlian.printer.model.PersonPlacementMode
import com.wanlian.printer.model.PrintSettings
import com.wanlian.printer.model.PrintUnits
import com.wanlian.printer.model.TextHorizontalAlignment
import com.wanlian.printer.model.TextWeight
import com.wanlian.printer.printing.RenderedBitmap
import com.wanlian.printer.ui.components.BorderPicker
import com.wanlian.printer.ui.components.ChoiceChips
import com.wanlian.printer.ui.components.CompactNumberControl
import com.wanlian.printer.ui.components.FontPickerDialog
import com.wanlian.printer.ui.components.FlowerStylePicker
import com.wanlian.printer.ui.components.SwitchRow
import com.wanlian.printer.printing.FontRepository
import java.util.Locale

enum class EditorTool(val label: String) {
    TEXT("文本"),
    BORDER("边框"),
    LAYOUT("排版"),
    FOOTER("页尾"),
}

enum class EditorPanelMode {
    MINIMIZED,
    COMPACT,
    EXPANDED,
}

@Composable
fun EmbeddedEditorPanel(
    selectedTool: EditorTool,
    onToolSelected: (EditorTool) -> Unit,
    state: MainUiState,
    onSettingsChange: ((PrintSettings) -> PrintSettings) -> Unit,
    scrollState: ScrollState,
    panelMode: EditorPanelMode,
    onTogglePreviewSpace: () -> Unit,
    panelFraction: Float,
    workspaceHeightPx: Float,
    onPanelFractionChange: (Float) -> Unit,
    onPanelDragStopped: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dragState = rememberDraggableState { deltaPixels ->
        val nextFraction = panelFraction - (deltaPixels / workspaceHeightPx)
        onPanelFractionChange(nextFraction.coerceIn(0.13f, 0.60f))
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clickable(onClick = onTogglePreviewSpace)
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        onDragStopped = { onPanelDragStopped(panelFraction) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(52.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                EditorTool.entries.forEach { tool ->
                    FilterChip(
                        selected = selectedTool == tool,
                        onClick = { onToolSelected(tool) },
                        label = { Text(tool.label) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (panelMode != EditorPanelMode.MINIMIZED) {
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (panelMode == EditorPanelMode.COMPACT) {
                        CompactToolContent(
                            selectedTool = selectedTool,
                            state = state,
                            onSettingsChange = onSettingsChange,
                        )
                    } else {
                        when (selectedTool) {
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
                            EditorTool.FOOTER -> FooterSettingsContent(
                                state = state,
                                onSettingsChange = onSettingsChange,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TextSettingsContent(
    settings: PrintSettings,
    onSettingsChange: ((PrintSettings) -> PrintSettings) -> Unit,
) {
    var showFontPicker by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = settings.text,
        onValueChange = { text -> onSettingsChange { it.copy(text = text) } },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("挽联内容") },
        supportingText = { Text("每个输入行生成一列竖排文字") },
        minLines = 3,
        maxLines = 6,
    )
    FontSelectorRow(
        fontId = settings.fontId,
        onClick = { showFontPicker = true },
    )
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
        valueRange = 0f..240f,
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
    ClosingTextBlockSettingsSection(
        settings = settings,
        onSettingsChange = onSettingsChange,
    )
    PersonSettingsSection(
        settings = settings,
        onSettingsChange = onSettingsChange,
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
    if (showFontPicker) {
        FontPickerDialog(
            selectedFontId = settings.fontId,
            onSelected = { font ->
                onSettingsChange { current ->
                    val personFontWasFollowingBody = current.personBlock.fontId == current.fontId
                    current.copy(
                        fontId = font.id,
                        personBlock = if (personFontWasFollowingBody) {
                            current.personBlock.copy(fontId = font.id)
                        } else {
                            current.personBlock
                        },
                    )
                }
            },
            onDismiss = { showFontPicker = false },
        )
    }
}

@Composable
fun BorderSettingsContent(
    settings: PrintSettings,
    onSettingsChange: ((PrintSettings) -> PrintSettings) -> Unit,
) {
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
    if (settings.autoPaperLength) {
        CompactNumberControl(
            title = "自动长度目标",
            value = settings.preferredAutoLengthMm,
            unit = "mm",
            valueRange = 100f..1500f,
            step = 10f,
            onValueChange = { value -> onSettingsChange { it.copy(preferredAutoLengthMm = value) } },
        )
    }
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
fun FooterSettingsContent(
    state: MainUiState,
    onSettingsChange: ((PrintSettings) -> PrintSettings) -> Unit,
) {
    var showFooterFontPicker by remember { mutableStateOf(false) }
    Text("落款小标签", style = MaterialTheme.typography.labelLarge)
    if (state.documentMode == DocumentMode.PAIR) {
        Text(
            text = "当前编辑：${state.pairDocument?.selectedSide?.label ?: "左联"}落款",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    SwitchRow(
        title = "启用落款标签",
        subtitle = "独立排版并靠近页尾",
        checked = state.settings.footerLabel.enabled,
        onCheckedChange = { enabled ->
            onSettingsChange { current -> current.copy(footerLabel = current.footerLabel.copy(enabled = enabled)) }
        },
    )
    if (state.settings.footerLabel.enabled) {
        OutlinedTextField(
            value = state.settings.footerLabel.text,
            onValueChange = { value ->
                onSettingsChange { current -> current.copy(footerLabel = current.footerLabel.copy(text = value)) }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("落款文字") },
            supportingText = {
                Text(
                    if (state.settings.footerLabel.orientation == FooterTextOrientation.VERTICAL) {
                        "每个输入行生成一列竖排文字"
                    } else {
                        "每个输入行生成一行横排文字"
                    },
                )
            },
            minLines = 2,
            maxLines = 4,
        )
        OutlinedTextField(
            value = state.settings.footerLabel.secondaryText,
            onValueChange = { value ->
                onSettingsChange { current -> current.copy(footerLabel = current.footerLabel.copy(secondaryText = value)) }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("附加文字（可选）") },
            minLines = 1,
            maxLines = 3,
        )
        Text("落款文字方向", style = MaterialTheme.typography.labelLarge)
        ChoiceChips(
            values = FooterTextOrientation.entries,
            selected = state.settings.footerLabel.orientation,
            label = { it.label },
            onSelected = { orientation ->
                onSettingsChange { current ->
                    current.copy(footerLabel = current.footerLabel.copy(orientation = orientation))
                }
            },
        )
        FontSelectorRow(
            fontId = state.settings.footerLabel.fontId,
            onClick = { showFooterFontPicker = true },
        )
        CompactNumberControl(
            title = "落款字号",
            value = state.settings.footerLabel.fontSizeDots,
            unit = "dots",
            valueRange = 20f..120f,
            step = 2f,
            decimals = 0,
            onValueChange = { value ->
                onSettingsChange { current -> current.copy(footerLabel = current.footerLabel.copy(fontSizeDots = value)) }
            },
        )
        CompactNumberControl(
            title = "落款字间距",
            value = state.settings.footerLabel.spacingDots,
            unit = "dots",
            valueRange = 0f..120f,
            step = 2f,
            decimals = 0,
            onValueChange = { value ->
                onSettingsChange { current -> current.copy(footerLabel = current.footerLabel.copy(spacingDots = value)) }
            },
        )
        CompactNumberControl(
            title = "落款上下偏移",
            value = state.settings.footerLabel.offsetYMm,
            unit = "mm",
            valueRange = FooterLabelAdjustmentLimits.MIN_OFFSET_Y_MM..
                FooterLabelAdjustmentLimits.MAX_OFFSET_Y_MM,
            step = 1f,
            decimals = 1,
            onValueChange = { value ->
                onSettingsChange { current ->
                    current.copy(
                        footerLabel = current.footerLabel.copy(
                            offsetYMm = FooterLabelAdjustmentLimits.clampOffsetYMm(value),
                        ),
                    )
                }
            },
        )
        CompactNumberControl(
            title = "与正文距离",
            value = state.settings.footerLabel.distanceFromMainMm,
            unit = "mm",
            valueRange = 0f..100f,
            step = 1f,
            onValueChange = { value ->
                onSettingsChange { current -> current.copy(footerLabel = current.footerLabel.copy(distanceFromMainMm = value)) }
            },
        )
        CompactNumberControl(
            title = "距页尾距离",
            value = state.settings.footerLabel.bottomMarginMm,
            unit = "mm",
            valueRange = 0f..100f,
            step = 1f,
            onValueChange = { value ->
                onSettingsChange { current -> current.copy(footerLabel = current.footerLabel.copy(bottomMarginMm = value)) }
            },
        )
        Text("落款位置", style = MaterialTheme.typography.labelLarge)
        ChoiceChips(
            values = FooterLabelPosition.entries,
            selected = state.settings.footerLabel.position,
            label = { it.label },
            onSelected = { position ->
                onSettingsChange { current -> current.copy(footerLabel = current.footerLabel.copy(position = position)) }
            },
        )
        Text("花朵装饰", style = MaterialTheme.typography.labelLarge)
        SwitchRow(
            title = "启用花朵装饰",
            checked = state.settings.footerLabel.flower.enabled,
            onCheckedChange = { enabled ->
                onSettingsChange { current ->
                    current.copy(
                        footerLabel = current.footerLabel.copy(
                            flower = current.footerLabel.flower.copy(enabled = enabled),
                        ),
                    )
                }
            },
        )
        if (state.settings.footerLabel.flower.enabled) {
            FlowerStylePicker(
                selected = state.settings.footerLabel.flower.style,
                onSelected = { style ->
                    onSettingsChange { current ->
                        current.copy(
                            footerLabel = current.footerLabel.copy(
                                flower = current.footerLabel.flower.copy(style = style),
                            ),
                        )
                    }
                },
            )
            CompactNumberControl(
                title = "花朵大小",
                value = state.settings.footerLabel.flower.sizeMm,
                unit = "mm",
                valueRange = FlowerAdjustmentLimits.MIN_SIZE_MM..FlowerAdjustmentLimits.MAX_SIZE_MM,
                step = 1f,
                onValueChange = { value ->
                    onSettingsChange { current ->
                        current.copy(
                            footerLabel = current.footerLabel.copy(
                                flower = current.footerLabel.flower.copy(sizeMm = value),
                            ),
                        )
                    }
                },
            )
            CompactNumberControl(
                title = "花朵水平微调",
                value = state.settings.footerLabel.flower.offsetXmm,
                unit = "mm",
                valueRange = FlowerAdjustmentLimits.MIN_OFFSET_MM..FlowerAdjustmentLimits.MAX_OFFSET_MM,
                step = 1f,
                onValueChange = { value ->
                    onSettingsChange { current ->
                        current.copy(
                            footerLabel = current.footerLabel.copy(
                                flower = current.footerLabel.flower.copy(offsetXmm = value),
                            ),
                        )
                    }
                },
            )
            CompactNumberControl(
                title = "花朵垂直微调",
                value = state.settings.footerLabel.flower.offsetYmm,
                unit = "mm",
                valueRange = FlowerAdjustmentLimits.MIN_OFFSET_MM..FlowerAdjustmentLimits.MAX_OFFSET_MM,
                step = 1f,
                onValueChange = { value ->
                    onSettingsChange { current ->
                        current.copy(
                            footerLabel = current.footerLabel.copy(
                                flower = current.footerLabel.flower.copy(offsetYmm = value),
                            ),
                        )
                    }
                },
            )
            CompactNumberControl(
                title = "花朵旋转",
                value = state.settings.footerLabel.flower.rotationDegrees,
                unit = "°",
                valueRange = FlowerAdjustmentLimits.MIN_ROTATION_DEGREES..
                    FlowerAdjustmentLimits.MAX_ROTATION_DEGREES,
                step = 5f,
                decimals = 0,
                onValueChange = { value ->
                    onSettingsChange { current ->
                        current.copy(
                            footerLabel = current.footerLabel.copy(
                                flower = current.footerLabel.flower.copy(rotationDegrees = value),
                            ),
                        )
                    }
                },
            )
        }
        HorizontalDivider()
    }
    CompactNumberControl(
        title = "页尾留白",
        value = state.settings.bottomMarginMm,
        unit = "mm",
        valueRange = 0f..100f,
        step = 1f,
        onValueChange = { value -> onSettingsChange { it.copy(bottomMarginMm = value) } },
    )
    Text("页尾裁切线", style = MaterialTheme.typography.labelLarge)
    SwitchRow(
        title = "启用页尾裁切辅助线",
        subtitle = "只打印细白色剪裁参考线，不填充燕尾区域",
        checked = state.settings.cutGuide.enabled,
        onCheckedChange = { enabled ->
            onSettingsChange { current -> current.copy(cutGuide = current.cutGuide.copy(enabled = enabled)) }
        },
    )
    if (state.settings.cutGuide.enabled) {
        Text("裁切样式", style = MaterialTheme.typography.labelLarge)
        ChoiceChips(
            values = listOf(CutGuideStyle.INWARD_V, CutGuideStyle.STRAIGHT),
            selected = state.settings.cutGuide.style,
            label = { it.label },
            onSelected = { style ->
                onSettingsChange { current -> current.copy(cutGuide = current.cutGuide.copy(style = style)) }
            },
        )
        CutGuideMiniPreview(state.settings.cutGuide.style)
        CompactNumberControl(
            title = "距页尾",
            value = state.settings.cutGuide.bottomOffsetMm,
            unit = "mm",
            valueRange = 1f..40f,
            step = 1f,
            onValueChange = { value ->
                onSettingsChange { current -> current.copy(cutGuide = current.cutGuide.copy(bottomOffsetMm = value)) }
            },
        )
        if (state.settings.cutGuide.style == CutGuideStyle.INWARD_V) {
            CompactNumberControl(
                title = "燕尾深度",
                value = state.settings.cutGuide.notchDepthMm,
                unit = "mm",
                valueRange = 5f..60f,
                step = 1f,
                onValueChange = { value ->
                    onSettingsChange { current -> current.copy(cutGuide = current.cutGuide.copy(notchDepthMm = value)) }
                },
            )
            CompactNumberControl(
                title = "左右边距",
                value = state.settings.cutGuide.edgeInsetMm,
                unit = "mm",
                valueRange = 0f..20f,
                step = 0.5f,
                onValueChange = { value ->
                    onSettingsChange { current -> current.copy(cutGuide = current.cutGuide.copy(edgeInsetMm = value)) }
                },
            )
        }
        CompactNumberControl(
            title = "裁切线宽",
            value = state.settings.cutGuide.lineWidthMm,
            unit = "mm",
            valueRange = 0.2f..1.5f,
            step = 0.1f,
            onValueChange = { value ->
                onSettingsChange { current -> current.copy(cutGuide = current.cutGuide.copy(lineWidthMm = value)) }
            },
        )
    }
    HorizontalDivider()
    if (showFooterFontPicker) {
        FontPickerDialog(
            selectedFontId = state.settings.footerLabel.fontId,
            onSelected = { font ->
                onSettingsChange { current -> current.copy(footerLabel = current.footerLabel.copy(fontId = font.id)) }
            },
            onDismiss = { showFooterFontPicker = false },
        )
    }
}

@Composable
private fun PersonSettingsSection(
    settings: PrintSettings,
    onSettingsChange: ((PrintSettings) -> PrintSettings) -> Unit,
) {
    var showPersonFontPicker by remember { mutableStateOf(false) }
    val block = settings.personBlock
    HorizontalDivider()
    Text("人员姓名", style = MaterialTheme.typography.labelLarge)
    SwitchRow(
        title = "启用人员姓名",
        subtitle = if (block.persons.isEmpty()) {
            "开启后可添加 1～6 人"
        } else {
            "已设置 ${block.persons.size} 人"
        },
        checked = block.enabled,
        onCheckedChange = { enabled ->
            onSettingsChange { current ->
                current.copy(
                    personBlock = current.personBlock.copy(
                        enabled = enabled,
                        persons = if (enabled && current.personBlock.persons.isEmpty()) {
                            listOf(FooterPerson())
                        } else {
                            current.personBlock.persons
                        },
                    ),
                )
            }
        },
    )
    if (block.enabled) {
        Text("姓名排列", style = MaterialTheme.typography.labelLarge)
        ChoiceChips(
            values = PersonLayout.entries,
            selected = block.layout,
            label = { it.label },
            onSelected = { layout ->
                onSettingsChange { current ->
                    current.copy(personBlock = current.personBlock.copy(layout = layout))
                }
            },
        )
        Text("姓名位置", style = MaterialTheme.typography.labelLarge)
        ChoiceChips(
            values = PersonPlacementMode.entries,
            selected = block.placementMode,
            label = { it.label },
            onSelected = { placementMode ->
                onSettingsChange { current ->
                    current.copy(
                        personBlock = current.personBlock.copy(placementMode = placementMode),
                    )
                }
            },
        )
        Text(
            if (block.layout == PersonLayout.PARALLEL_COLUMNS) {
                "每个人各占一列、列内竖排，并从同一顶部开始。"
            } else {
                "所有人员按顺序接成一列竖排。"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("人员列表（1～6 人）", style = MaterialTheme.typography.labelLarge)
        block.persons.forEachIndexed { index, person ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                ),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("人员 ${index + 1}", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = person.relation,
                            onValueChange = { relation ->
                                onSettingsChange { current ->
                                    val updated = current.personBlock.persons.toMutableList().apply {
                                        this[index] = this[index].copy(relation = relation)
                                    }
                                    current.copy(personBlock = current.personBlock.copy(persons = updated))
                                }
                            },
                            modifier = Modifier.weight(0.38f),
                            label = { Text("称谓") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = person.name,
                            onValueChange = { name ->
                                onSettingsChange { current ->
                                    val updated = current.personBlock.persons.toMutableList().apply {
                                        this[index] = this[index].copy(name = name)
                                    }
                                    current.copy(personBlock = current.personBlock.copy(persons = updated))
                                }
                            },
                            modifier = Modifier.weight(0.62f),
                            label = { Text("姓名") },
                            singleLine = true,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        TextButton(
                            onClick = {
                                onSettingsChange { current ->
                                    if (index == 0) current else {
                                        val updated = current.personBlock.persons.toMutableList()
                                        val moved = updated.removeAt(index)
                                        updated.add(index - 1, moved)
                                        current.copy(personBlock = current.personBlock.copy(persons = updated))
                                    }
                                }
                            },
                            enabled = index > 0,
                        ) { Text("上移") }
                        TextButton(
                            onClick = {
                                onSettingsChange { current ->
                                    if (index >= current.personBlock.persons.lastIndex) current else {
                                        val updated = current.personBlock.persons.toMutableList()
                                        val moved = updated.removeAt(index)
                                        updated.add(index + 1, moved)
                                        current.copy(personBlock = current.personBlock.copy(persons = updated))
                                    }
                                }
                            },
                            enabled = index < block.persons.lastIndex,
                        ) { Text("下移") }
                        TextButton(
                            onClick = {
                                onSettingsChange { current ->
                                    val updated = current.personBlock.persons.toMutableList().apply {
                                        if (index in indices) removeAt(index)
                                    }
                                    current.copy(personBlock = current.personBlock.copy(persons = updated))
                                }
                            },
                        ) { Text("删除") }
                    }
                }
            }
        }
        if (block.persons.size < PersonLayoutRules.MAX_PERSONS) {
            TextButton(
                onClick = {
                    onSettingsChange { current ->
                        current.copy(
                            personBlock = current.personBlock.copy(
                                persons = current.personBlock.persons + FooterPerson(),
                            ),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("+ 添加人员（最多 6 人）") }
        }
        if (block.persons.isNotEmpty()) {
            FontSelectorRow(
                fontId = block.fontId,
                label = "姓名字体",
                onClick = { showPersonFontPicker = true },
            )
            TextButton(
                onClick = {
                    onSettingsChange { current ->
                        current.copy(personBlock = current.personBlock.copy(fontId = current.fontId))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("使用正文字体") }
            CompactNumberControl(
                title = "姓名字号",
                value = block.fontSizeDots,
                unit = "dots",
                valueRange = PersonLayoutRules.MIN_FONT_SIZE_DOTS..PersonLayoutRules.MAX_FONT_SIZE_DOTS,
                step = 2f,
                decimals = 0,
                onValueChange = { value ->
                    onSettingsChange { current ->
                        current.copy(personBlock = current.personBlock.copy(fontSizeDots = value))
                    }
                },
            )
            CompactNumberControl(
                title = "姓名字间距",
                value = block.characterSpacingDots,
                unit = "dots",
                valueRange = 0f..240f,
                step = 2f,
                decimals = 0,
                onValueChange = { value ->
                    onSettingsChange { current ->
                        current.copy(
                            personBlock = current.personBlock.copy(characterSpacingDots = value),
                        )
                    }
                },
            )
            if (block.placementMode == PersonPlacementMode.SIDE_OVERLAY) {
                Text("水平位置", style = MaterialTheme.typography.labelLarge)
                ChoiceChips(
                    values = PersonBlockHorizontalPreset.entries,
                    selected = PersonBlockHorizontalPreset.resolve(
                        block.positionXNorm,
                        block.offsetXMm,
                    ),
                    label = { it.label },
                    onSelected = { preset ->
                        preset.positionXNorm?.let { position ->
                            onSettingsChange { current ->
                                current.copy(
                                    personBlock = current.personBlock.copy(
                                        positionXNorm = position,
                                        offsetXMm = 0f,
                                    ),
                                )
                            }
                        }
                    },
                )
                CompactNumberControl(
                    title = "水平微调",
                    value = block.offsetXMm,
                    unit = "mm",
                    valueRange = PersonBlockSettings.MIN_OFFSET_X_MM..
                        PersonBlockSettings.MAX_OFFSET_X_MM,
                    step = 1f,
                    onValueChange = { value ->
                        onSettingsChange { current ->
                            current.copy(personBlock = current.personBlock.copy(offsetXMm = value))
                        }
                    },
                )
                CompactNumberControl(
                    title = "垂直微调",
                    value = block.offsetYMm,
                    unit = "mm",
                    valueRange = PersonBlockSettings.MIN_OFFSET_Y_MM..
                        PersonBlockSettings.MAX_OFFSET_Y_MM,
                    step = 1f,
                    onValueChange = { value ->
                        onSettingsChange { current ->
                            current.copy(personBlock = current.personBlock.copy(offsetYMm = value))
                        }
                    },
                )
                Text(
                    "也可以直接在预览中拖动姓名。旁置不会改变正文字号或纸张长度。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val maximumIndex = bodyCharacterCount(settings.text)
                val resolvedIndex = block.personInsertIndex.coerceIn(0, maximumIndex)
                CompactNumberControl(
                    title = "插入位置（第 $resolvedIndex 字后）",
                    value = resolvedIndex.toFloat(),
                    unit = "字",
                    valueRange = 0f..maximumIndex.coerceAtLeast(1).toFloat(),
                    step = 1f,
                    decimals = 0,
                    onValueChange = { value ->
                        onSettingsChange { current ->
                            val maxIndex = bodyCharacterCount(current.text)
                            current.copy(
                                personBlock = current.personBlock.copy(
                                    personInsertIndex = value.toInt().coerceIn(0, maxIndex),
                                ),
                            )
                        }
                    },
                )
                Text(
                    "姓名块会占用正文纵向空间，并自动把插入点后的正文下移。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (block.layout == PersonLayout.PARALLEL_COLUMNS) {
                CompactNumberControl(
                    title = "姓名列间距",
                    value = block.columnGapMm,
                    unit = "mm",
                    valueRange = PersonLayoutRules.MIN_COLUMN_GAP_MM..
                        PersonLayoutRules.MAX_COLUMN_GAP_MM,
                    step = 1f,
                    onValueChange = { value ->
                        onSettingsChange { current ->
                            current.copy(personBlock = current.personBlock.copy(columnGapMm = value))
                        }
                    },
                )
                val resolved = PersonLayoutRules.resolveParallelColumns(
                    personCount = block.persons.size.coerceAtLeast(1),
                    requestedFontSizeDots = block.fontSizeDots,
                    requestedColumnGapMm = block.columnGapMm,
                    availableWidthDots = PrintUnits.mmToDots(settings.paperWidthMm).toFloat(),
                    fontWidthScale = FontRepository.resolve(block.fontId).textScaleX,
                )
                if (resolved.wasAutoReduced) {
                    Text(
                        "当前人员较多，请减小姓名字号或列间距。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
    if (showPersonFontPicker) {
        FontPickerDialog(
            selectedFontId = block.fontId,
            onSelected = { font ->
                onSettingsChange { current ->
                    current.copy(personBlock = current.personBlock.copy(fontId = font.id))
                }
            },
            onDismiss = { showPersonFontPicker = false },
        )
    }
}

private fun bodyCharacterCount(text: String): Int = text.lineSequence()
    .map(String::trim)
    .filter(String::isNotEmpty)
    .maxOfOrNull { line -> line.codePointCount(0, line.length) }
    ?: 0

@Composable
private fun ClosingTextBlockSettingsSection(
    settings: PrintSettings,
    onSettingsChange: ((PrintSettings) -> PrintSettings) -> Unit,
) {
    val detectedPhrases = remember(settings.text) {
        ClosingTextBlockRules.detectedPhrases(settings.text)
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        CompactNumberControl(
            title = "尾部字上下偏移",
            value = settings.closingTextBlock.offsetYMm,
            unit = "mm",
            valueRange = ClosingTextBlockRules.MIN_OFFSET_Y_MM..
                ClosingTextBlockRules.MAX_OFFSET_Y_MM,
            step = 1f,
            decimals = 1,
            enabled = detectedPhrases.isNotEmpty(),
            onValueChange = { value ->
                onSettingsChange { current ->
                    current.copy(
                        closingTextBlock = current.closingTextBlock.copy(
                            offsetYMm = ClosingTextBlockRules.clampOffsetYMm(value),
                        ),
                    )
                }
            },
        )
        Text(
            text = if (detectedPhrases.isEmpty()) {
                "未识别到千古、叩挽等收尾词"
            } else {
                "当前收尾字：${detectedPhrases.joinToString("、")}；负数上移，正数下移"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CompactToolContent(
    selectedTool: EditorTool,
    state: MainUiState,
    onSettingsChange: ((PrintSettings) -> PrintSettings) -> Unit,
) {
    when (selectedTool) {
        EditorTool.TEXT -> {
            OutlinedTextField(
                value = state.settings.text,
                onValueChange = { text -> onSettingsChange { it.copy(text = text) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("挽联内容") },
                minLines = 1,
                maxLines = 2,
            )
            CompactNumberControl(
                title = "字间距",
                value = state.settings.characterSpacingDots,
                unit = "dots",
                valueRange = 0f..240f,
                step = 2f,
                decimals = 0,
                onValueChange = { value -> onSettingsChange { it.copy(characterSpacingDots = value) } },
            )
            ClosingTextBlockSettingsSection(
                settings = state.settings,
                onSettingsChange = onSettingsChange,
            )
            PersonSettingsSection(
                settings = state.settings,
                onSettingsChange = onSettingsChange,
            )
        }
        EditorTool.BORDER -> {
            InfoValueRow("当前边框", state.settings.border.style.label)
            ChoiceChips(
                values = BorderPosition.entries,
                selected = state.settings.border.position,
                label = { it.label },
                onSelected = { position ->
                    onSettingsChange { current -> current.copy(border = current.border.copy(position = position)) }
                },
            )
        }
        EditorTool.LAYOUT -> {
            CompactNumberControl(
                title = "纸张宽度",
                value = state.settings.paperWidthMm,
                unit = "mm",
                valueRange = 30f..110f,
                step = 1f,
                onValueChange = { value -> onSettingsChange { it.copy(paperWidthMm = value) } },
            )
            SwitchRow(
                title = "自动纸长",
                checked = state.settings.autoPaperLength,
                onCheckedChange = { enabled -> onSettingsChange { it.copy(autoPaperLength = enabled) } },
            )
        }
        EditorTool.FOOTER -> {
            SwitchRow(
                title = "落款标签",
                checked = state.settings.footerLabel.enabled,
                onCheckedChange = { enabled ->
                    onSettingsChange { current -> current.copy(footerLabel = current.footerLabel.copy(enabled = enabled)) }
                },
            )
            InfoValueRow(
                "人员姓名",
                "${state.settings.personBlock.persons.size} 人 · 在“文本”中设置",
            )
            InfoValueRow(
                "裁切线",
                if (state.settings.cutGuide.enabled) state.settings.cutGuide.style.label else "关闭",
            )
        }
    }
}

@Composable
private fun CutGuideMiniPreview(style: CutGuideStyle) {
    val guideColor = Color.White
    val edgeColor = Color(0xFF6F7471)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        val left = 0f
        val right = size.width
        val bottom = size.height * 0.88f
        drawLine(edgeColor, start = androidx.compose.ui.geometry.Offset(left, 0f), end = androidx.compose.ui.geometry.Offset(left, bottom))
        drawLine(edgeColor, start = androidx.compose.ui.geometry.Offset(right, 0f), end = androidx.compose.ui.geometry.Offset(right, bottom))
        when (style) {
            CutGuideStyle.STRAIGHT -> drawLine(
                color = guideColor,
                start = androidx.compose.ui.geometry.Offset(left, bottom),
                end = androidx.compose.ui.geometry.Offset(right, bottom),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
            CutGuideStyle.INWARD_V -> {
                val centerTip = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.22f)
                drawLine(
                    color = guideColor,
                    start = androidx.compose.ui.geometry.Offset(left, bottom),
                    end = centerTip,
                    strokeWidth = 2f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = guideColor,
                    start = androidx.compose.ui.geometry.Offset(right, bottom),
                    end = centerTip,
                    strokeWidth = 2f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun FontSelectorRow(fontId: String, label: String = "字体", onClick: () -> Unit) {
    val definition = FontRepository.resolve(fontId)
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${definition.displayName}  ›", fontWeight = FontWeight.SemiBold)
        }
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
