package com.wanlian.printer.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wanlian.printer.model.PrintSettings
import com.wanlian.printer.printing.RenderedBitmap

/** Fixed compact panel used only for tablet and wide landscape layouts. */
@Composable
fun EditorToolPanel(
    settings: PrintSettings,
    rendered: RenderedBitmap?,
    onSettingsChange: ((PrintSettings) -> PrintSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTool by remember { mutableStateOf(EditorTool.TEXT) }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(EditorTool.TEXT, EditorTool.BORDER, EditorTool.LAYOUT).forEach { tool ->
                    FilterChip(
                        selected = selectedTool == tool,
                        onClick = { selectedTool = tool },
                        label = { Text(tool.label) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (selectedTool) {
                    EditorTool.TEXT -> TextSettingsContent(settings, onSettingsChange)
                    EditorTool.BORDER -> BorderSettingsContent(settings, onSettingsChange)
                    EditorTool.LAYOUT -> LayoutSettingsContent(settings, rendered, onSettingsChange)
                    EditorTool.MORE -> Unit
                }
            }
        }
    }
}
