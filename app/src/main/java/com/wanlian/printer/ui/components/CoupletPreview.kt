package com.wanlian.printer.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.wanlian.printer.printing.RenderedBitmap
import kotlin.math.max
import kotlin.math.min

/**
 * High-resolution preview viewport. Scale and pan deliberately survive bitmap updates so
 * reactive editing does not throw the user back to Fit while inspecting a detail.
 */
@Composable
fun CoupletPreview(
    rendered: RenderedBitmap?,
    isRendering: Boolean,
    modifier: Modifier = Modifier,
    fullScreen: Boolean = false,
    onFullscreen: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onPrint: (() -> Unit)? = null,
) {
    val density = LocalDensity.current
    val paperWidthDp = ((rendered?.paperWidthMm ?: 100f) * NATURAL_DP_PER_MM).dp
    val paperHeightDp = ((rendered?.paperLengthMm ?: 200f) * NATURAL_DP_PER_MM).dp
    val naturalWidthPx = with(density) { paperWidthDp.toPx() }
    val naturalHeightPx = with(density) { paperHeightDp.toPx() }
    val horizontalPaddingPx = with(density) { 24.dp.toPx() }
    val verticalControlsPx = with(density) {
        (if (fullScreen) 132.dp else 76.dp).toPx()
    }

    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var initialized by remember { mutableStateOf(false) }

    fun calculateFitScale(): Float {
        if (viewportSize.width <= 0 || viewportSize.height <= 0) return MIN_SCALE
        val availableWidth = (viewportSize.width - horizontalPaddingPx).coerceAtLeast(1f)
        val availableHeight = (viewportSize.height - verticalControlsPx).coerceAtLeast(1f)
        return min(
            availableWidth / naturalWidthPx.coerceAtLeast(1f),
            availableHeight / naturalHeightPx.coerceAtLeast(1f),
        ).coerceIn(MIN_FIT_SCALE, MAX_SCALE)
    }

    fun clampOffset(candidate: Offset, atScale: Float): Offset {
        if (viewportSize.width <= 0 || viewportSize.height <= 0) return Offset.Zero
        val scaledWidth = naturalWidthPx * atScale
        val scaledHeight = naturalHeightPx * atScale
        val maxX = max(0f, (scaledWidth - viewportSize.width) / 2f)
        val maxY = max(0f, (scaledHeight - viewportSize.height) / 2f)
        return Offset(
            x = candidate.x.coerceIn(-maxX, maxX),
            y = candidate.y.coerceIn(-maxY, maxY),
        )
    }

    fun fit() {
        scale = calculateFitScale()
        offset = Offset.Zero
    }

    fun actualSize() {
        scale = 1f
        offset = Offset.Zero
    }

    LaunchedEffect(
        viewportSize,
        rendered?.paperWidthMm,
        rendered?.paperLengthMm,
    ) {
        if (rendered != null && viewportSize.width > 0 && viewportSize.height > 0) {
            if (!initialized) {
                fit()
                initialized = true
            } else {
                // Preserve zoom and position after reactive re-rendering; only enforce bounds.
                offset = clampOffset(offset, scale)
            }
        }
    }

    val workspaceColor = if (fullScreen) {
        Color(0xFF151817)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = modifier
            .background(workspaceColor)
            .onSizeChanged { viewportSize = it }
            .pointerInput(viewportSize, naturalWidthPx, naturalHeightPx) {
                detectTransformGestures(panZoomLock = false) { centroid, pan, zoom, _ ->
                    val oldScale = scale
                    val gestureMinimum = min(MIN_SCALE, calculateFitScale())
                    val newScale = (oldScale * zoom).coerceIn(gestureMinimum, MAX_SCALE)
                    val scaleChange = newScale / oldScale.coerceAtLeast(0.0001f)
                    val viewportCenter = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
                    val focusFromCenter = centroid - viewportCenter
                    // Keep the content point under the gesture centroid stationary while zooming.
                    val focusedOffset = focusFromCenter -
                        (focusFromCenter - offset) * scaleChange + pan
                    scale = newScale
                    offset = clampOffset(focusedOffset, newScale)
                }
            }
            .pointerInput(viewportSize, rendered?.paperWidthMm, rendered?.paperLengthMm) {
                detectTapGestures(onDoubleTap = { fit() })
            },
        contentAlignment = Alignment.Center,
    ) {
        if (rendered != null) {
            Image(
                bitmap = rendered.previewBitmap.asImageBitmap(),
                contentDescription = "黑色挽联布白字排版预览",
                modifier = Modifier
                    .requiredSize(paperWidthDp, paperHeightDp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                        transformOrigin = TransformOrigin.Center
                        clip = false
                    }
                    .background(Color.Black)
                    .border(1.dp, Color(0xFF777B79)),
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.High,
            )
        } else {
            Text("正在生成预览…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (fullScreen && onBack != null) {
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shape = MaterialTheme.shapes.medium,
            ) {
                TextButton(onClick = onBack) { Text("返回") }
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shape = MaterialTheme.shapes.medium,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                TextButton(onClick = ::fit) { Text("Fit") }
                TextButton(onClick = ::actualSize) { Text("100%") }
                Text(
                    "${(scale * 100).toInt()}%",
                    modifier = Modifier.width(50.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
                if (!fullScreen && onFullscreen != null) {
                    TextButton(onClick = onFullscreen) { Text("⛶") }
                }
            }
        }

        if (fullScreen && onPrint != null) {
            Button(
                onClick = onPrint,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            ) { Text("打印") }
        }

        if (isRendering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
            )
        }
    }
}

private const val NATURAL_DP_PER_MM = 2.4f
private const val MIN_SCALE = 0.1f
private const val MIN_FIT_SCALE = 0.02f
private const val MAX_SCALE = 5f
