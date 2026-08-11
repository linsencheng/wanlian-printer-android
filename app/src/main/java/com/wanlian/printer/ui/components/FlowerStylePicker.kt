package com.wanlian.printer.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.wanlian.printer.model.FlowerStyle
import com.wanlian.printer.printing.FlowerRenderer

@Composable
fun FlowerStylePicker(
    selected: FlowerStyle,
    onSelected: (FlowerStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val renderer = FlowerRenderer()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowerStyle.entries.chunked(3).forEach { rowStyles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowStyles.forEach { style ->
                    val isSelected = style == selected
                    Card(
                        modifier = Modifier.weight(1f).clickable { onSelected(style) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                        border = if (isSelected) {
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            null
                        },
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Canvas(
                                Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .background(Color.Black),
                            ) {
                                if (style != FlowerStyle.NONE) {
                                    drawIntoCanvas { composeCanvas ->
                                        renderer.draw(
                                            canvas = composeCanvas.nativeCanvas,
                                            style = style,
                                            centerX = size.width / 2f,
                                            topY = size.height * 0.10f,
                                            sizeDots = size.minDimension * 0.80f,
                                            sourcePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                                color = AndroidColor.WHITE
                                            },
                                        )
                                    }
                                }
                            }
                            Text(style.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
                repeat(3 - rowStyles.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
