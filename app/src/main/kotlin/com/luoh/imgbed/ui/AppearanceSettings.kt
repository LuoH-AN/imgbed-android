package com.luoh.imgbed.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.luoh.imgbed.data.AppAppearance
import com.luoh.imgbed.ui.agora.ColorSchemePreset
import com.luoh.imgbed.ui.agora.LocalSettingsScrollRetentionActive
import com.luoh.imgbed.ui.agora.SchemeStyle
import com.luoh.imgbed.ui.agora.ThemeMode

private val Presets = listOf(
    ColorSchemePreset.OCEAN to "海洋", ColorSchemePreset.MIDNIGHT to "午夜",
    ColorSchemePreset.NORDIC to "北欧", ColorSchemePreset.FOREST to "森林",
    ColorSchemePreset.SUNSET to "日落", ColorSchemePreset.ROSE to "玫瑰",
    ColorSchemePreset.LAVENDER to "薰衣草", ColorSchemePreset.SLATE to "岩灰",
)
private val Styles = listOf(
    SchemeStyle.NEUTRAL to "中性", SchemeStyle.TONAL_SPOT to "柔和",
    SchemeStyle.EXPRESSIVE to "表现力", SchemeStyle.VIBRANT to "鲜艳", SchemeStyle.MONOCHROME to "黑白灰",
)

@Composable
internal fun AppearanceSettings(appearance: AppAppearance, onSave: (AppAppearance) -> Unit) {
    val dynamicSupported = Build.VERSION.SDK_INT >= 31
    val dynamicActive = dynamicSupported && appearance.dynamicColor
    var colorPicker by rememberSaveable { mutableStateOf(false) }
    val active = LocalSettingsScrollRetentionActive.current
    LaunchedEffect(active, dynamicActive) { if (!active || dynamicActive) colorPicker = false }
    SettingsGroup("主题与颜色", listOf(
        { EnumMenu(Icons.Default.SettingsBrightness, "主题", appearance.mode, listOf(ThemeMode.FOLLOW_DEVICE, ThemeMode.LIGHT, ThemeMode.DARK), {
            when (it) { ThemeMode.FOLLOW_DEVICE -> "跟随系统"; ThemeMode.LIGHT -> "浅色"; ThemeMode.DARK -> "深色" }
        }) { onSave(appearance.copy(mode = it)) } },
        { ToggleRow(Icons.Default.Contrast, "AMOLED", appearance.amoled) { onSave(appearance.copy(amoled = it)) } },
        { ToggleRow(Icons.Default.ColorLens, "动态颜色", appearance.dynamicColor, enabled = dynamicSupported) { onSave(appearance.copy(dynamicColor = it)) } },
        { EnumMenu(Icons.Default.Palette, "配色方案", if (appearance.accent == null) appearance.preset.name else "custom",
            (if (appearance.accent != null) listOf("custom") else emptyList()) + Presets.map { it.first.name },
            { value -> Presets.firstOrNull { it.first.name == value }?.second ?: "自定义" }, enabled = !dynamicActive) {
            if (it != "custom") onSave(appearance.copy(preset = ColorSchemePreset.valueOf(it), accent = null))
        } },
        { EnumMenu(Icons.Default.Style, "方案风格", appearance.style, Styles.map { it.first }, { value -> Styles.first { it.first == value }.second },
            enabled = !dynamicActive) { onSave(appearance.copy(style = it)) } },
        { SettingsRow(Icons.Default.Palette, "主调颜色", enabled = !dynamicActive, onClick = { colorPicker = true }, trailing = {
            Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }) },
    ))
    if (colorPicker) ColorDialog(
        initial = appearance.accent ?: MaterialTheme.colorScheme.primary.toArgb(),
        onDismiss = { colorPicker = false },
        onApply = { onSave(appearance.copy(accent = it)) },
        onReset = { onSave(appearance.copy(accent = null)) },
    )
}

@Composable
private fun ColorDialog(initial: Int, onDismiss: () -> Unit, onApply: (Int) -> Unit, onReset: () -> Unit) {
    var draft by rememberSaveable { mutableStateOf("#%06X".format(initial and 0xFFFFFF)) }
    val normalized = draft.trim().removePrefix("#").uppercase()
    val valid = normalized.matches(Regex("[0-9A-F]{6}"))
    val presets = listOf("#FFFFFF", "#9FD8FF", "#111111", "#FFB6D5", "#3F683D", "#B9CCB3")
    AlertDialog(onDismissRequest = onDismiss, title = { Text("主调颜色") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { hex ->
                    Box(Modifier.size(32.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(hex)))
                        .clickable { draft = hex }.semantics { contentDescription = "颜色 $hex" }, contentAlignment = Alignment.Center) {
                        if (hex.equals("#$normalized", true)) Icon(Icons.Default.Check, null,
                            tint = if (hex == "#111111" || hex == "#3F683D") Color.White else Color.Black, modifier = Modifier.size(18.dp))
                    }
                }
            }
            OutlinedTextField(draft, { draft = it.take(7) }, label = { Text("十六进制颜色") },
                isError = !valid, singleLine = true, modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.ColorLens, null, tint = if (valid) Color(android.graphics.Color.parseColor("#$normalized")) else MaterialTheme.colorScheme.onSurfaceVariant) })
        }
    }, confirmButton = {
        TextButton({ onApply(android.graphics.Color.parseColor("#$normalized")); onDismiss() }, enabled = valid) { Text("应用") }
    }, dismissButton = {
        Row {
            TextButton({ onReset(); onDismiss() }) { Text("恢复配色方案") }
            TextButton(onDismiss) { Text("取消") }
        }
    })
}
