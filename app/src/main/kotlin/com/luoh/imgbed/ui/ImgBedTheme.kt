package com.luoh.imgbed.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import com.luoh.imgbed.data.AppAppearance
import com.luoh.imgbed.ui.agora.AgoraTheme
import com.luoh.imgbed.ui.agora.ProvideAgoraMotionPolicy
import com.luoh.imgbed.ui.agora.ThemeMode

@Composable
internal fun ImgBedTheme(appearance: AppAppearance, content: @Composable () -> Unit) {
    val activity = LocalContext.current as? Activity
    val systemDark = isSystemInDarkTheme()
    val dark = appearance.mode == ThemeMode.DARK || appearance.mode == ThemeMode.FOLLOW_DEVICE && systemDark
    SideEffect {
        activity?.let {
            WindowInsetsControllerCompat(it.window, it.window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
            if (Build.VERSION.SDK_INT >= 29) {
                it.window.isNavigationBarContrastEnforced = false
                it.window.isStatusBarContrastEnforced = false
            }
        }
    }
    AgoraTheme(
        themeMode = appearance.mode,
        colorSchemePreset = appearance.preset,
        schemeStyle = appearance.style,
        dynamicColor = appearance.dynamicColor,
        amoledEnabled = appearance.amoled,
        seedColor = appearance.accent,
    ) { ProvideAgoraMotionPolicy(appearance.reduceMotion, content) }
}
