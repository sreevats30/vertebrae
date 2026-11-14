package com.companion.yogawellness.ui.codex

import com.companion.yogawellness.ui.components.cardSurface
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.companion.yogawellness.codex.AsanaCatalog32
import com.companion.yogawellness.codex.AsanaCategory
import com.companion.yogawellness.codex.AsanaCodexItem
import com.companion.yogawellness.codex.Difficulty
import com.companion.yogawellness.ui.components.ClinicalTopBar
import com.companion.yogawellness.ui.components.Overline
import com.companion.yogawellness.ui.components.Pill
import com.companion.yogawellness.ui.theme.AppTheme
import com.companion.yogawellness.ui.theme.Brand
import com.companion.yogawellness.ui.theme.ScoreColors

/** List filter: all, live-scored only, or one category. */
data class CodexFilter(val liveOnly: Boolean = false, val category: AsanaCategory? = null) {
    companion object {
        val ALL = CodexFilter()
        val LIVE = CodexFilter(liveOnly = true)
    }
}

/** Search + filter, grouped by category in catalog order; empty groups are dropped. */
fun filterCodex(
    items: List<AsanaCodexItem>,
    query: String,
    filter: CodexFilter
): List<Pair<AsanaCategory, List<AsanaCodexItem>>> {
    val q = query.trim().lowercase()
    val kept = items.filter { item ->
        (!filter.liveOnly || item.isLive) &&
            (filter.category == null || item.category == filter.category) &&
            (q.isEmpty() || listOf(item.englishName, item.sanskritName, item.everydayBenefit, item.id.replace('_', ' '))
                .any { it.lowercase().contains(q) })
    }
    return AsanaCategory.entries.mapNotNull { c -> kept.filter { it.category == c }.takeIf { it.isNotEmpty() }?.let { c to it } }
}

/** The Learn tab: 32 poses with search, filters and static peak-pose thumbnails. */
@Composable
fun AsanaCodexScreen(onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    var liveOnly by rememberSaveable { mutableStateOf(false) }
    var categoryName by rememberSaveable { mutableStateOf<String?>(null) }
    val filter = CodexFilter(liveOnly, categoryName?.let { AsanaCategory.valueOf(it) })
    val groups = remember(query, filter) { filterCodex(AsanaCatalog32.items, query, filter) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ClinicalTopBar(
                title = "Learn",
                subtitle = "${AsanaCatalog32.items.size} poses · animated demos",
                bottom = { SearchField(query) { query = it } }
            )
        }
    ) { inner ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(inner),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    "32 poses with animated demos. Four of them can also be scored live with the camera.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            item {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterPill("All", filter == CodexFilter.ALL) { liveOnly = false; categoryName = null }
                    FilterPill("Live-scored", filter == CodexFilter.LIVE, live = true) { liveOnly = true; categoryName = null }
                    AsanaCategory.entries.forEach { c ->
                        FilterPill(c.shortTitle, filter.category == c) { liveOnly = false; categoryName = c.name }
                    }
                }
            }
            if (groups.isEmpty()) {
                item {
                    Text(
                        "No poses match “${query.trim()}”.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.inkMuted,
                        modifier = Modifier.padding(vertical = 24.dp, horizontal = 4.dp)
                    )
                }
            }
            groups.forEach { (category, list) ->
                item(key = "header-${category.name}") {
                    Column(Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Overline("${list.size} ${if (list.size == 1) "pose" else "poses"}", color = AppTheme.colors.inkMuted)
                        Text(
                            category.title, style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(category.subtitle, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.inkMuted)
                    }
                }
                items(list, key = { it.id }) { item -> CodexRow(item, onClick = { onOpen(item.id) }) }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    // Light theme: a white field on the navy header. Dark theme: a frosted field, so it doesn't glare.
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val container = if (dark) Color(0xFF16263B) else Color.White
    val ink = if (dark) Color.White else Brand.Navy900
    val icon = if (dark) Brand.Mint else Brand.Teal600
    val hint = if (dark) Color(0xFF9FB0C4) else Color(0xFF64748B)
    TextField(
        value = query,
        onValueChange = onChange,
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) ({
            IconButton(onClick = { onChange("") }) { Icon(Icons.Rounded.Close, contentDescription = "Clear search") }
        }) else null,
        placeholder = { Text("Search poses") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container.copy(alpha = 0.94f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = ink,
            unfocusedTextColor = ink,
            focusedLeadingIconColor = icon,
            unfocusedLeadingIconColor = icon,
            focusedTrailingIconColor = hint,
            unfocusedTrailingIconColor = hint,
            focusedPlaceholderColor = hint,
            unfocusedPlaceholderColor = hint,
            cursorColor = icon
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (dark) Modifier.border(1.dp, AppTheme.colors.glassBorder, RoundedCornerShape(16.dp)) else Modifier)
    )
}

@Composable
private fun FilterPill(text: String, selected: Boolean, live: Boolean = false, onClick: () -> Unit) {
    val accent = if (live) ScoreColors.Green else MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .clip(CircleShape)
            .background(
                if (selected) Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.86f))) else AppTheme.colors.cardBrush
            )
            .border(1.dp, if (selected) Color.White.copy(alpha = 0.30f) else AppTheme.colors.cardEdge, CircleShape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val on = if (live) Color.White else MaterialTheme.colorScheme.onPrimary
        if (live) Icon(Icons.Rounded.Videocam, contentDescription = null, tint = if (selected) on else accent, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) on else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CodexRow(item: AsanaCodexItem, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .cardSurface(shape)
            .clickable(role = Role.Button, onClickLabel = "Open ${item.englishName}", onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box {
            SkeletonThumbnail(item, item.defaultView, Modifier.width(72.dp))
            if (item.isLive) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(ScoreColors.Green)
                        .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("${item.number}. ${item.englishName}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(item.sanskritName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(item.everydayBenefit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                Tag(item.difficulty.label, if (item.difficulty == Difficulty.INTERMEDIATE) ScoreColors.Amber else AppTheme.colors.inkMuted)
                if (item.isLive) Tag("Live-scored", ScoreColors.Green)
            }
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = AppTheme.colors.inkMuted)
    }
}

@Composable
internal fun Tag(text: String, color: Color) {
    Pill(text, color = color, container = color.copy(alpha = 0.12f))
}
