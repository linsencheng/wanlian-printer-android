package com.wanlian.printer.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.wanlian.printer.model.BorderSettings
import com.wanlian.printer.model.BorderStyle
import com.wanlian.printer.printing.BorderRenderer

@Composable
fun BorderPicker(
    selected: BorderStyle,
    settings: BorderSettings,
    onSelected: (BorderStyle) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BorderStyle.entries.chunked(2).forEach { rowStyles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowStyles.forEach { style ->
                    BorderStyleCard(
                        style = style,
                        settings = settings,
                        selected = selected == style,
                        onClick = { onSelected(style) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowStyles.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BorderStyleCard(
    style: BorderStyle,
    settings: BorderSettings,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val renderer = BorderRenderer()
    val outline = if (selected) MaterialTheme.colorScheme.primary else Color(0xFFCDD2CF)
    Column(
        modifier = modifier
            .border(if (selected) 2.dp else 1.dp, outline, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(
            modifier = Modifier.fillMaxWidth().height(70.dp).background(Color.Black),
        ) {
            if (style != BorderStyle.NONE) {
                drawIntoCanvas { canvas ->
                    val native = canvas.nativeCanvas
                    val laneWidth = size.width * 0.12f
                    renderer.drawBorder(
                        native,
                        style,
                        RectF(6f, 0f, 6f + laneWidth, size.height),
                        settings,
                        AndroidColor.WHITE,
                        dotsPerMm = 1.5f,
                    )
                    renderer.drawBorder(
                        native,
                        style,
                        RectF(size.width - 6f - laneWidth, 0f, size.width - 6f, size.height),
                        settings,
                        AndroidColor.WHITE,
                        dotsPerMm = 1.5f,
                    )
                }
            }
        }
        Text(
            if (selected) "✓ ${style.label}" else style.label,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
