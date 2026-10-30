package com.luoh.imgbed.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luoh.imgbed.ui.agora.*
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun PageScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    reserveDock: Boolean = true,
    bottomAction: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    CompositionLocalProvider(LocalSettingsPaneBackButtonVisible provides (onBack != null)) {
        CollapsingSettingsLazyScaffold(
            title = title,
            onBack = onBack ?: {},
            contentBottomPadding = if (bottomAction != null) 200.dp else if (reserveDock) 112.dp else 32.dp,
            actions = actions,
            floatingActionButton = {
                if (bottomAction != null) {
                    Surface(
                        modifier = Modifier.padding(bottom = if (reserveDock) 96.dp else 0.dp)
                            .widthIn(max = 840.dp).fillMaxWidth().padding(horizontal = 24.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shadowElevation = 4.dp,
                    ) { Box(Modifier.padding(8.dp)) { bottomAction() } }
                }
            },
            content = content,
        )
    }
}

@Composable
internal fun RoundAction(icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    Surface(onClick = onClick, enabled = enabled, modifier = Modifier.padding(4.dp).size(40.dp),
        shape = CircleShape, color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label) }
    }
}

@Composable
internal fun SettingsGroup(title: String, rows: List<@Composable () -> Unit>) {
    com.luoh.imgbed.ui.agora.SettingsGroup(title = title, items = rows)
}

@Composable
internal fun GroupItem(index: Int, count: Int, content: @Composable () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().padding(top = if (index == 0) 0.dp else 2.dp),
        shape = RoundedCornerShape(
            topStart = if (index == 0) 24.dp else 5.dp, topEnd = if (index == 0) 24.dp else 5.dp,
            bottomStart = if (index == count - 1) 24.dp else 5.dp, bottomEnd = if (index == count - 1) 24.dp else 5.dp,
        ),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        content = content,
    )
}

@Composable
internal fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String = "",
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    SettingsItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary) },
        trailingContent = trailing ?: if (value.isNotBlank() || onClick != null) ({
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value.isNotBlank()) Text(value, Modifier.widthIn(max = 128.dp), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (onClick != null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                    Modifier.padding(start = 4.dp).size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }) else null,
        modifier = Modifier.alpha(if (enabled) 1f else .38f)
            .then(if (onClick == null) Modifier else Modifier.clickable(enabled = enabled, onClick = onClick)),
    )
}

@Composable
internal fun EmptyState(icon: ImageVector, title: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (action != null) TextButton(onAction) { Text(action) }
    }
}

@Composable
internal fun ToggleRow(icon: ImageVector, title: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    SettingsItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = { Switch(checked, onCheckedChange = null, enabled = enabled) },
        modifier = Modifier.alpha(if (enabled) 1f else .38f)
            .toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange),
    )
}

@Composable
internal fun <T> EnumMenu(icon: ImageVector, label: String, value: T, values: List<T>, text: (T) -> String,
    enabled: Boolean = true, onChange: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(enabled) { if (!enabled) expanded = false }
    SettingsRow(icon, label, enabled = enabled, onClick = { expanded = true }, trailing = {
        Box {
            Text(text(value), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            AgoraDropdownMenu(expanded, { expanded = false }, containerColor = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 16.dp) {
                values.forEach { item ->
                    AgoraDropdownMenuItem(
                        text = { Text(text(item)) },
                        leadingIcon = { if (item == value) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = { expanded = false; onChange(item) },
                    )
                }
            }
        }
    })
}

@Composable
internal fun SliderSetting(title: String, value: Int, range: IntRange, icon: ImageVector, unit: String = "", onCommit: (Int) -> Unit) {
    var draft by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        SettingsRow(icon, title, "${draft.roundToInt()}$unit")
        Slider(draft.coerceIn(range.first.toFloat(), range.last.toFloat()), { draft = it },
            onValueChangeFinished = { onCommit(draft.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(), steps = (range.last - range.first - 1).coerceAtLeast(0),
            modifier = Modifier.padding(horizontal = 16.dp).semantics { contentDescription = title })
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
