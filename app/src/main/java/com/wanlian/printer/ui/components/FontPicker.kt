package com.wanlian.printer.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wanlian.printer.model.FontDefinition
import com.wanlian.printer.model.TextWeight
import com.wanlian.printer.printing.FontImportResult
import com.wanlian.printer.printing.FontRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class FontPickerTab(val label: String) {
    BUILT_IN("内置字体"),
    MY_FONTS("我的字体"),
}

@Composable
fun FontPickerDialog(
    selectedFontId: String,
    onSelected: (FontDefinition) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fonts by FontRepository.fontsFlow.collectAsState()
    var tab by rememberSaveable(selectedFontId) {
        mutableStateOf(
            if (fonts.any { it.id == selectedFontId && it.localFilePath != null }) {
                FontPickerTab.MY_FONTS
            } else {
                FontPickerTab.BUILT_IN
            },
        )
    }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isImporting = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { FontRepository.importFont(context, uri) }
            isImporting = false
            when (result) {
                is FontImportResult.Success -> {
                    tab = FontPickerTab.MY_FONTS
                    statusMessage = "字体已导入并应用：${result.font.displayName}"
                    onSelected(result.font)
                }
                is FontImportResult.Failure -> statusMessage = result.message
            }
        }
    }

    val visibleFonts = fonts.filter { font ->
        when (tab) {
            FontPickerTab.BUILT_IN -> font.localFilePath == null
            FontPickerTab.MY_FONTS -> font.localFilePath != null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择字体") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FontPickerTab.entries.forEach { candidate ->
                        FilterChip(
                            selected = tab == candidate,
                            onClick = { tab = candidate },
                            label = { Text(candidate.label) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (tab == FontPickerTab.MY_FONTS) {
                    Button(
                        onClick = {
                            importLauncher.launch(
                                arrayOf(
                                    "font/ttf",
                                    "font/otf",
                                    "application/x-font-ttf",
                                    "application/x-font-opentype",
                                    "application/octet-stream",
                                ),
                            )
                        },
                        enabled = !isImporting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (isImporting) "正在导入…" else "+ 导入 TTF / OTF 字体")
                    }
                }
                statusMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (tab == FontPickerTab.MY_FONTS && visibleFonts.isEmpty()) {
                    Text(
                        "尚未导入字体。字体将复制到 App 私有目录，无需存储权限。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visibleFonts, key = { it.id }) { font ->
                        FontOption(
                            font = font,
                            selected = font.id == selectedFontId,
                            onClick = {
                                onSelected(font)
                                onDismiss()
                            },
                            onDelete = if (font.localFilePath == null) {
                                null
                            } else {
                                {
                                    if (FontRepository.deleteImportedFont(font.id)) {
                                        if (font.id == selectedFontId) onSelected(FontRepository.defaultFont)
                                        statusMessage = "字体已删除"
                                    } else {
                                        statusMessage = "删除字体失败，请稍后重试。"
                                    }
                                }
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        },
    )
}

@Composable
private fun FontOption(
    font: FontDefinition,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(font.displayName, fontWeight = FontWeight.SemiBold)
                    Text(
                        font.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (selected) Text("已选择", color = MaterialTheme.colorScheme.primary)
                if (onDelete != null) TextButton(onClick = onDelete) { Text("删除") }
            }
            FontSample(font)
        }
    }
}

@Composable
private fun FontSample(font: FontDefinition) {
    val density = LocalDensity.current
    val color = MaterialTheme.colorScheme.onSurface
    val textSize = with(density) { 25.sp.toPx() }
    Canvas(Modifier.fillMaxWidth().height(46.dp)) {
        val paint = FontRepository.createPaint(
            fontId = font.id,
            sizeDots = textSize,
            weight = TextWeight.NORMAL,
            color = color.toArgbCompat(),
        )
        val metrics = paint.fontMetrics
        val baseline = size.height / 2f - (metrics.ascent + metrics.descent) / 2f
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText("任绍夏泽宇叩挽", size.width / 2f, baseline, paint)
        }
    }
}

private fun Color.toArgbCompat(): Int =
    ((alpha * 255).toInt() shl 24) or
        ((red * 255).toInt() shl 16) or
        ((green * 255).toInt() shl 8) or
        (blue * 255).toInt()
