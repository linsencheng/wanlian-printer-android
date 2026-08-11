package com.wanlian.printer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF355F52),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E8E0),
    onPrimaryContainer = Color(0xFF173C32),
    secondary = Color(0xFF625B50),
    background = Color(0xFFF8FAF9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE7ECE9),
    surfaceContainerLowest = Color(0xFFFBFCFB),
    surfaceContainerLow = Color(0xFFF7F8F6),
    surfaceContainer = Color(0xFFEEF2EF),
    surfaceContainerHigh = Color(0xFFE6EBE8),
    error = Color(0xFFB3261E),
)

@Composable
fun WanlianTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
