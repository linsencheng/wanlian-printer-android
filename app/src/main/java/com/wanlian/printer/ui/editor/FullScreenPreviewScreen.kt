package com.wanlian.printer.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.wanlian.printer.printing.RenderedBitmap
import com.wanlian.printer.ui.components.CoupletPreview

@Composable
fun FullScreenPreviewScreen(
    rendered: RenderedBitmap?,
    isRendering: Boolean,
    onBack: () -> Unit,
    onPrint: () -> Unit,
) {
    BackHandler(onBack = onBack)
    CoupletPreview(
        rendered = rendered,
        isRendering = isRendering,
        modifier = Modifier.fillMaxSize(),
        fullScreen = true,
        onBack = onBack,
        onPrint = onPrint,
    )
}
