package com.wanlian.printer.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
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
import com.wanlian.printer.model.CoupletSide
import com.wanlian.printer.model.DocumentMode
import com.wanlian.printer.printing.RenderedBitmap
import com.wanlian.printer.printing.RenderedCoupletPair
import kotlin.math.max
import kotlin.math.min

@Stable
class PreviewTransformState {
    var scale by mutableFloatStateOf(1f)
        internal set
    var offset by mutableStateOf(Offset.Zero)
        internal set
    internal var initialized: Boolean = false

    private var fitAction: () -> Unit = {}
    private var actualSizeAction: () -> Unit = {}
    private var zoomAction: (Float) -> Unit = {}

    fun fit() = fitAction()
    fun actualSize() = actualSizeAction()
    fun zoomOut() = zoomAction(-ZOOM_STEP)
    fun zoomIn() = zoomAction(ZOOM_STEP)

    internal fun bind(
        fit: () -> Unit,
        actualSize: () -> Unit,
        zoomBy: (Float) -> Unit,
    ) {
        fitAction = fit
        actualSizeAction = actualSize
        zoomAction = zoomBy
    }
}

@Composable
fun rememberPreviewTransformState(): PreviewTransformState = remember { PreviewTransformState() }

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
    transformState: PreviewTransformState = rememberPreviewTransformState(),
    showControls: Boolean = true,
    documentMode: DocumentMode = DocumentMode.SINGLE,
    pairRendered: RenderedCoupletPair? = null,
    selectedSide: CoupletSide = CoupletSide.LEFT,
    onSideSelected: ((CoupletSide) -> Unit)? = null,
    onPersonBlockMove: ((Float, Float) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val showPair = documentMode == DocumentMode.PAIR && pairRendered != null
    val paperWidthMm = pairRendered?.left?.paperWidthMm ?: rendered?.paperWidthMm ?: 100f
    val paperLengthMm = pairRendered?.paperLengthMm ?: rendered?.paperLengthMm ?: 200f
    val paperWidthDp = (paperWidthMm * NATURAL_DP_PER_MM).dp
    val paperHeightDp = (paperLengthMm * NATURAL_DP_PER_MM).dp
    val naturalCanvasWidthDp = if (showPair) paperWidthDp * 2 + PAIR_GAP_DP.dp else paperWidthDp
    val naturalWidthPx = with(density) { naturalCanvasWidthDp.toPx() }
    val naturalHeightPx = with(density) { paperHeightDp.toPx() }
    val horizontalPaddingPx = with(density) { 24.dp.toPx() }
    val verticalControlsPx = with(density) {
        when {
            !showControls -> 0.dp
            fullScreen -> 132.dp
            else -> 76.dp
        }.toPx()
    }

    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var personBlockDragging by remember { mutableStateOf(false) }
    var personBlockSelected by remember { mutableStateOf(false) }
    val activePersonBlockAvailable = if (showPair) {
        val selectedRendered = if (selectedSide == CoupletSide.LEFT) {
            pairRendered?.left
        } else {
            pairRendered?.right
        }
        selectedRendered?.personBlockBounds != null && selectedRendered.personBlockDraggable
    } else {
        rendered?.personBlockBounds != null && rendered.personBlockDraggable
    }

    LaunchedEffect(selectedSide, showPair) {
        personBlockSelected = false
    }
    LaunchedEffect(activePersonBlockAvailable) {
        if (!activePersonBlockAvailable) personBlockSelected = false
    }

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
        transformState.scale = calculateFitScale()
        transformState.offset = Offset.Zero
    }

    fun actualSize() {
        transformState.scale = 1f
        transformState.offset = Offset.Zero
    }

    fun zoomBy(delta: Float) {
        val minimum = min(MIN_SCALE, calculateFitScale())
        val newScale = (transformState.scale + delta).coerceIn(minimum, MAX_SCALE)
        transformState.scale = newScale
        transformState.offset = clampOffset(transformState.offset, newScale)
    }

    SideEffect {
        transformState.bind(::fit, ::actualSize, ::zoomBy)
    }

    LaunchedEffect(
        viewportSize,
        rendered?.paperWidthMm,
        rendered?.paperLengthMm,
    ) {
        if (rendered != null && viewportSize.width > 0 && viewportSize.height > 0) {
            if (!transformState.initialized) {
                fit()
                transformState.initialized = true
            }
            // Once initialized, preserve zoom and pan exactly while the editor panel is resized
            // or the bitmap is reactively regenerated. Fit remains available explicitly.
        }
    }

    val workspaceColor = if (fullScreen) {
        Color(0xFF151817)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    Box(
        modifier = modifier
            .background(workspaceColor)
            .onSizeChanged { viewportSize = it }
            .pointerInput(viewportSize, naturalWidthPx, naturalHeightPx) {
                detectTransformGestures(panZoomLock = false) { centroid, pan, zoom, _ ->
                    if (personBlockDragging) return@detectTransformGestures
                    val oldScale = transformState.scale
                    val gestureMinimum = min(MIN_SCALE, calculateFitScale())
                    val newScale = (oldScale * zoom).coerceIn(gestureMinimum, MAX_SCALE)
                    val scaleChange = newScale / oldScale.coerceAtLeast(0.0001f)
                    val viewportCenter = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
                    val focusFromCenter = centroid - viewportCenter
                    // Keep the content point under the gesture centroid stationary while zooming.
                    val focusedOffset = focusFromCenter -
                        (focusFromCenter - transformState.offset) * scaleChange + pan
                    transformState.scale = newScale
                    transformState.offset = clampOffset(focusedOffset, newScale)
                }
            }
            .pointerInput(viewportSize, rendered?.paperWidthMm, rendered?.paperLengthMm) {
                detectTapGestures(
                    onTap = { personBlockSelected = false },
                    onDoubleTap = { fit() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (showPair) {
            Row(
                modifier = Modifier
                    .requiredSize(naturalCanvasWidthDp, paperHeightDp)
                    .graphicsLayer {
                        scaleX = transformState.scale
                        scaleY = transformState.scale
                        translationX = transformState.offset.x
                        translationY = transformState.offset.y
                        transformOrigin = TransformOrigin.Center
                        clip = false
                    },
                horizontalArrangement = Arrangement.spacedBy(PAIR_GAP_DP.dp),
            ) {
                PairPreviewPage(
                    rendered = pairRendered!!.left,
                    side = CoupletSide.LEFT,
                    selected = selectedSide == CoupletSide.LEFT,
                    onSelected = onSideSelected,
                    onPersonBlockMove = if (selectedSide == CoupletSide.LEFT) {
                        onPersonBlockMove
                    } else {
                        null
                    },
                    onPersonBlockDraggingChanged = { personBlockDragging = it },
                    personBlockSelected = personBlockSelected,
                    onPersonBlockSelected = { personBlockSelected = it },
                    modifier = Modifier.requiredSize(paperWidthDp, paperHeightDp),
                )
                PairPreviewPage(
                    rendered = pairRendered.right,
                    side = CoupletSide.RIGHT,
                    selected = selectedSide == CoupletSide.RIGHT,
                    onSelected = onSideSelected,
                    onPersonBlockMove = if (selectedSide == CoupletSide.RIGHT) {
                        onPersonBlockMove
                    } else {
                        null
                    },
                    onPersonBlockDraggingChanged = { personBlockDragging = it },
                    personBlockSelected = personBlockSelected,
                    onPersonBlockSelected = { personBlockSelected = it },
                    modifier = Modifier.requiredSize(paperWidthDp, paperHeightDp),
                )
            }
        } else if (rendered != null) {
            PreviewPage(
                rendered = rendered,
                onPersonBlockMove = onPersonBlockMove,
                onPersonBlockDraggingChanged = { personBlockDragging = it },
                personBlockSelected = personBlockSelected,
                onPersonBlockSelected = { personBlockSelected = it },
                modifier = Modifier
                    .requiredSize(paperWidthDp, paperHeightDp)
                    .graphicsLayer {
                        scaleX = transformState.scale
                        scaleY = transformState.scale
                        translationX = transformState.offset.x
                        translationY = transformState.offset.y
                        transformOrigin = TransformOrigin.Center
                        clip = false
                    }
                    .background(Color.Black)
                    .border(1.dp, Color(0xFF777B79)),
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

        if (showControls) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shape = MaterialTheme.shapes.medium,
            ) {
                CompactPreviewControls(
                    transformState = transformState,
                    onFullscreen = if (!fullScreen) onFullscreen else null,
                )
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

@Composable
private fun PairPreviewPage(
    rendered: RenderedBitmap,
    side: CoupletSide,
    selected: Boolean,
    onSelected: ((CoupletSide) -> Unit)?,
    onPersonBlockMove: ((Float, Float) -> Unit)?,
    onPersonBlockDraggingChanged: (Boolean) -> Unit,
    personBlockSelected: Boolean,
    onPersonBlockSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Color.Black)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF777B79),
            )
            .then(if (onSelected == null) Modifier else Modifier.clickable { onSelected(side) }),
    ) {
        PreviewPage(
            rendered = rendered,
            onPersonBlockMove = onPersonBlockMove,
            onInteractionStart = { onSelected?.invoke(side) },
            onPersonBlockDraggingChanged = onPersonBlockDraggingChanged,
            personBlockSelected = personBlockSelected,
            onPersonBlockSelected = onPersonBlockSelected,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun PreviewPage(
    rendered: RenderedBitmap,
    onPersonBlockMove: ((Float, Float) -> Unit)?,
    modifier: Modifier = Modifier,
    onInteractionStart: (() -> Unit)? = null,
    onPersonBlockDraggingChanged: (Boolean) -> Unit,
    personBlockSelected: Boolean,
    onPersonBlockSelected: (Boolean) -> Unit,
) {
    val density = LocalDensity.current
    Box(modifier = modifier) {
        Image(
            bitmap = rendered.previewBitmap.asImageBitmap(),
            contentDescription = "黑色挽联布白字排版预览",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            filterQuality = FilterQuality.High,
        )
        val bounds = rendered.personBlockBounds
        if (bounds != null && rendered.personBlockDraggable && onPersonBlockMove != null) {
            val pageWidthDp = (rendered.paperWidthMm * NATURAL_DP_PER_MM).dp
            val pageHeightDp = (rendered.paperLengthMm * NATURAL_DP_PER_MM).dp
            Box(
                modifier = Modifier
                    .offset(
                        x = pageWidthDp * bounds.leftNorm,
                        y = pageHeightDp * bounds.topNorm,
                    )
                    .requiredSize(
                        width = pageWidthDp * (bounds.rightNorm - bounds.leftNorm)
                            .coerceAtLeast(0.002f),
                        height = pageHeightDp * (bounds.bottomNorm - bounds.topNorm)
                            .coerceAtLeast(0.002f),
                    )
                    .border(
                        width = if (personBlockSelected) 1.dp else 0.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                    )
                    .clickable {
                        onInteractionStart?.invoke()
                        onPersonBlockSelected(true)
                    }
                    .then(
                        if (!personBlockSelected) {
                            Modifier
                        } else {
                            Modifier.pointerInput(
                                bounds,
                                rendered.paperWidthMm,
                                rendered.paperLengthMm,
                            ) {
                                detectDragGestures(
                                    onDragStart = {
                                        onInteractionStart?.invoke()
                                        onPersonBlockDraggingChanged(true)
                                    },
                                    onDragEnd = { onPersonBlockDraggingChanged(false) },
                                    onDragCancel = { onPersonBlockDraggingChanged(false) },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val deltaXMm = dragAmount.x /
                                            density.density / NATURAL_DP_PER_MM
                                        val deltaYMm = dragAmount.y /
                                            density.density / NATURAL_DP_PER_MM
                                        onPersonBlockMove(deltaXMm, deltaYMm)
                                    },
                                )
                            }
                        },
                    ),
            )
        }
    }
}

@Composable
fun CompactPreviewControls(
    transformState: PreviewTransformState,
    modifier: Modifier = Modifier,
    onFullscreen: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        PreviewControl(
            label = "Fit",
            width = 44.dp,
            onClick = transformState::fit,
            onLongClick = transformState::actualSize,
        )
        PreviewControl("−", 40.dp, transformState::zoomOut)
        PreviewControl(
            label = "${(transformState.scale * 100).toInt()}%",
            width = 44.dp,
            onClick = null,
        )
        PreviewControl("+", 40.dp, transformState::zoomIn)
        if (onFullscreen != null) {
            PreviewControl("⛶", 40.dp, onFullscreen)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PreviewControl(
    label: String,
    width: androidx.compose.ui.unit.Dp,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)? = null,
) {
    val controlModifier = Modifier.width(width).fillMaxHeight().let { base ->
        when {
            onLongClick != null -> base.combinedClickable(
                onClick = onClick ?: {},
                onLongClick = onLongClick,
            )
            onClick != null -> base.clickable(onClick = onClick)
            else -> base
        }
    }
    Box(modifier = controlModifier, contentAlignment = Alignment.Center) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

private const val NATURAL_DP_PER_MM = 2.4f
private const val MIN_SCALE = 0.1f
private const val MIN_FIT_SCALE = 0.02f
private const val MAX_SCALE = 5f
private const val ZOOM_STEP = 0.15f
private const val PAIR_GAP_DP = 64
