package com.luoh.imgbed.data

import android.content.Context
import com.luoh.imgbed.ui.agora.ColorSchemePreset
import com.luoh.imgbed.ui.agora.SchemeStyle
import com.luoh.imgbed.ui.agora.ThemeMode

data class AppAppearance(
    val mode: ThemeMode = ThemeMode.FOLLOW_DEVICE,
    val amoled: Boolean = false,
    val dynamicColor: Boolean = false,
    val preset: ColorSchemePreset = ColorSchemePreset.OCEAN,
    val style: SchemeStyle = SchemeStyle.NEUTRAL,
    val accent: Int? = null,
    val reduceMotion: Boolean = false,
)

class AppearancePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("imgbed_appearance", Context.MODE_PRIVATE)

    fun read() = AppAppearance(
        mode = enumValue(prefs.getString("mode", null), ThemeMode.FOLLOW_DEVICE),
        amoled = prefs.getBoolean("amoled", false),
        dynamicColor = prefs.getBoolean("dynamic_color", false),
        preset = enumValue(prefs.getString("preset", null), ColorSchemePreset.OCEAN),
        style = enumValue(prefs.getString("style", null), SchemeStyle.NEUTRAL),
        accent = if (prefs.contains("accent")) prefs.getInt("accent", 0) else null,
        reduceMotion = prefs.getBoolean("reduce_motion", false),
    )

    fun save(value: AppAppearance) {
        prefs.edit().apply {
            putString("mode", value.mode.name)
            putBoolean("amoled", value.amoled)
            putBoolean("dynamic_color", value.dynamicColor)
            putString("preset", value.preset.name)
            putString("style", value.style.name)
            if (value.accent == null) remove("accent") else putInt("accent", value.accent)
            putBoolean("reduce_motion", value.reduceMotion)
        }.apply()
    }

    private inline fun <reified T : Enum<T>> enumValue(raw: String?, fallback: T): T =
        runCatching { enumValueOf<T>(raw.orEmpty()) }.getOrDefault(fallback)
}
