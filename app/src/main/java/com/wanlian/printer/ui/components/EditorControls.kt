package com.wanlian.printer.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun SettingSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true,
    steps: Int = 0,
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(valueText, style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            steps = steps,
        )
    }
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun <T> ChoiceChips(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { value ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelected(value) },
                label = { Text(label(value)) },
            )
        }
    }
}

/** Compact, precise numeric editor. Holding +/- repeats after a short delay. */
@Composable
fun CompactNumberControl(
    title: String,
    value: Float,
    unit: String,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    decimals: Int = 1,
    enabled: Boolean = true,
) {
    var showInput by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }

    fun normalized(candidate: Float): Float {
        val factor = 10f.pow(decimals)
        return ((candidate.coerceIn(valueRange.start, valueRange.endInclusive) * factor).roundToInt() / factor)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        RepeatStepButton(
            label = "−",
            enabled = enabled && value > valueRange.start,
            onStep = { onValueChange(normalized(value - step)) },
        )
        TextButton(
            onClick = {
                input = formatNumber(value, decimals)
                showInput = true
            },
            enabled = enabled,
            modifier = Modifier.width(104.dp),
        ) {
            Text(
                "${formatNumber(value, decimals)} $unit".trim(),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
        RepeatStepButton(
            label = "+",
            enabled = enabled && value < valueRange.endInclusive,
            onStep = { onValueChange(normalized(value + step)) },
        )
    }

    if (showInput) {
        AlertDialog(
            onDismissRequest = { showInput = false },
            title = { Text("输入$title") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(unit.ifBlank { "数值" }) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        input.replace(',', '.').toFloatOrNull()?.let {
                            onValueChange(normalized(it))
                        }
                        showInput = false
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showInput = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun RepeatStepButton(
    label: String,
    enabled: Boolean,
    onStep: () -> Unit,
) {
    val currentStep by rememberUpdatedState(onStep)
    Surface(
        modifier = Modifier
            .width(42.dp)
            .height(36.dp)
            .semantics {
                role = Role.Button
                onClick {
                    if (enabled) currentStep()
                    enabled
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        currentStep()
                        coroutineScope {
                            val repeatJob = launch {
                                delay(420)
                                while (isActive) {
                                    currentStep()
                                    delay(85)
                                }
                            }
                            tryAwaitRelease()
                            repeatJob.cancel()
                        }
                    },
                )
            },
        shape = RoundedCornerShape(10.dp),
        color = if (enabled) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
        }
    }
}

private fun formatNumber(value: Float, decimals: Int): String =
    String.format(Locale.CHINA, "%.${decimals}f", value)
