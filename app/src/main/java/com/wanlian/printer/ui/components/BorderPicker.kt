package com.wanlian.printer.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wanlian.printer.model.BorderSettings
import com.wanlian.printer.model.BorderTemplate
import com.wanlian.printer.model.BorderTemplateCategory
import com.wanlian.printer.printing.BorderThumbnailRenderer

@Composable
fun BorderPicker(
    selected: BorderTemplate,
    settings: BorderSettings,
    onSelected: (BorderTemplate) -> Unit,
) {
    var selectedCategory by rememberSaveable {
        mutableStateOf<BorderTemplateCategory?>(null)
    }
    val visibleTemplates = remember(selectedCategory) {
        BorderTemplate.entries.filter { template ->
            selectedCategory == null || template.category == selectedCategory
        }
    }
    val gridState = rememberLazyGridState()

    LaunchedEffect(selected, selectedCategory) {
        val selectedIndex = visibleTemplates.indexOf(selected)
        if (selectedIndex >= 0) gridState.animateScrollToItem(selectedIndex)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "当前：${selected.label}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    selected.category.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("全部") },
            )
            BorderTemplateCategory.entries.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category.label) },
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().height(340.dp),
            state = gridState,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            items(visibleTemplates, key = { it.name }) { template ->
                BorderTemplateCard(
                    template = template,
                    settings = settings,
                    selected = selected == template,
                    onClick = { onSelected(template) },
                )
            }
        }
    }
}

@Composable
private fun BorderTemplateCard(
    template: BorderTemplate,
    settings: BorderSettings,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbnailRenderer = remember { BorderThumbnailRenderer() }
    val outline = if (selected) MaterialTheme.colorScheme.primary else Color(0xFFCDD2CF)
    val background = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Column(
        modifier = modifier
            .border(if (selected) 2.dp else 1.dp, outline, RoundedCornerShape(12.dp))
            .background(background, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(72.dp).background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawIntoCanvas { canvas ->
                    thumbnailRenderer.drawPair(
                        canvas = canvas.nativeCanvas,
                        width = size.width,
                        height = size.height,
                        template = template,
                        settings = settings,
                        color = AndroidColor.WHITE,
                    )
                }
            }
            if (template == BorderTemplate.NONE) {
                Text(
                    "无装饰",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            if (selected) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        "✓",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        Text(
            template.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
