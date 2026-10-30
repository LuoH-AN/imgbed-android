package com.luoh.imgbed.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

/** One title follows the header's scroll, then docks above the content. */
@Composable
internal fun PageScaffold(
    title: String,
    subtitle: String,
    eyebrow: String = "IMGBED",
    state: LazyListState = rememberLazyListState(),
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    val density = LocalDensity.current
    val travelPx = with(density) { 66.dp.toPx() }
    val fraction by remember(state, travelPx) {
        derivedStateOf {
            if (state.firstVisibleItemIndex > 0) 1f
            else (state.firstVisibleItemScrollOffset / travelPx).coerceIn(0f, 1f)
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = 840.dp).fillMaxSize()) {
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 124.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "page-header") {
                    Box(Modifier.fillMaxWidth().height(160.dp)) {
                        Text(subtitle, Modifier.align(Alignment.BottomStart).padding(bottom = 6.dp),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                content()
            }
            Box(Modifier.fillMaxWidth().height(64.dp).background(MaterialTheme.colorScheme.background)) {
                if (onBack != null) {
                    RoundAction(Icons.AutoMirrored.Rounded.ArrowBack, "返回", onBack, Modifier.padding(start = 16.dp, top = 8.dp))
                } else {
                    Text(eyebrow, Modifier.padding(start = 26.dp, top = 24.dp).graphicsLayer { alpha = 1f - fraction },
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Row(Modifier.align(Alignment.CenterEnd).padding(end = 16.dp), content = actions)
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 24.dp, end = 80.dp).fillMaxWidth()
                    .graphicsLayer {
                        translationY = with(density) { (82.dp - 66.dp * fraction).toPx() }
                        translationX = with(density) { (if (onBack != null) 44.dp * fraction else 0.dp).toPx() }
                        scaleX = 1f - .35f * fraction
                        scaleY = scaleX
                        transformOrigin = TransformOrigin(0f, 0f)
                    }.semantics { heading() },
            )
        }
    }
}

@Composable
internal fun RoundAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    FilledTonalIconButton(onClick, modifier.size(48.dp), enabled = enabled,
        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Icon(icon, label, Modifier.size(22.dp))
    }
}

@Composable
internal fun SectionHeading(title: String, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleMedium)
        trailing()
    }
}

@Composable
internal fun SettingsGroup(title: String, rows: List<@Composable () -> Unit>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, Modifier.padding(start = 16.dp, bottom = 10.dp).semantics { heading() },
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        rows.forEachIndexed { index, row ->
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = if (index == 0) 24.dp else 5.dp,
                    topEnd = if (index == 0) 24.dp else 5.dp,
                    bottomStart = if (index == rows.lastIndex) 24.dp else 5.dp,
                    bottomEnd = if (index == rows.lastIndex) 24.dp else 5.dp,
                ),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) { row() }
        }
    }
}

@Composable
internal fun SettingsRow(
    icon: ImageVector,
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, Modifier.size(23.dp), tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (summary.isNotBlank()) Text(summary, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) trailing()
        else if (onClick != null) Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun ToggleRow(icon: ImageVector, title: String, summary: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    SettingsRow(icon, title, summary,
        modifier = Modifier.toggleable(checked, role = Role.Switch, onValueChange = onChange),
        trailing = { Switch(checked, onCheckedChange = null) })
}

@Composable
internal fun <T> EnumMenu(icon: ImageVector, label: String, value: T, values: List<T>, text: (T) -> String, onChange: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        SettingsRow(icon, label, text(value), onClick = { expanded = true }, trailing = {
            Icon(Icons.Rounded.ExpandMore, null, tint = MaterialTheme.colorScheme.primary)
        })
        DropdownMenu(expanded, onDismissRequest = { expanded = false }, shape = RoundedCornerShape(24.dp)) {
            values.forEach { item ->
                DropdownMenuItem(
                    text = { Text(text(item), color = if (item == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) },
                    onClick = { expanded = false; onChange(item) },
                    modifier = Modifier.padding(horizontal = 6.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (item == value) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
                )
            }
        }
    }
}

@Composable
internal fun StatusPill(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Surface(shape = CircleShape, color = color.copy(alpha = .10f)) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = color, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun EmptyState(icon: ImageVector, title: String, description: String, action: @Composable () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Icon(icon, null, Modifier.padding(22.dp).size(32.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        action()
    }
}

internal fun formatBytes(bytes: Long): String = when {
    bytes < 0 -> "大小未知"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.ROOT, "%.2f MB", bytes / 1024.0 / 1024.0)
}

internal fun shareText(context: Context, value: String) {
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, value)
    }, "分享链接"))
}
