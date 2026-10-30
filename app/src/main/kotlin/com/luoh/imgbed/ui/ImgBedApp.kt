package com.luoh.imgbed.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luoh.imgbed.ui.agora.LocalSettingsScrollRetention
import com.luoh.imgbed.ui.agora.LocalSettingsScrollPath
import com.luoh.imgbed.ui.agora.LocalSettingsScrollRetentionActive
import com.luoh.imgbed.ui.agora.SettingsScrollRetention

private enum class AppTab(val label: String, val icon: ImageVector) {
    UPLOAD("上传", Icons.Default.CloudUpload),
    FILES("文件", Icons.Default.Folder),
    SETTINGS("设置", Icons.Default.Settings),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImgBedApp(viewModel: AppViewModel) {
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    ImgBedTheme(appearance) {
        var tab by rememberSaveable { mutableStateOf(AppTab.UPLOAD) }
        var settingsPage by rememberSaveable { mutableStateOf(SettingsPage.HOME) }
        var settingsOverlayVisible by remember { mutableStateOf(false) }
        val message by viewModel.message.collectAsStateWithLifecycle()
        val snackbar = remember { SnackbarHostState() }
        val pageStates = rememberSaveableStateHolder()
        val scrollRetention = remember { SettingsScrollRetention() }
        val keyboardVisible = WindowInsets.isImeVisible
        LaunchedEffect(message) {
            if (message.isNotBlank()) {
                snackbar.showSnackbar(message)
                viewModel.clearMessage()
            }
        }
        BackHandler(tab != AppTab.UPLOAD && !(tab == AppTab.SETTINGS && settingsPage != SettingsPage.HOME)) {
            tab = AppTab.UPLOAD
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
                Crossfade(tab, animationSpec = tween(250), label = "page") { current ->
                    CompositionLocalProvider(
                        LocalSettingsScrollRetention provides scrollRetention,
                        LocalSettingsScrollPath provides listOf(current),
                        LocalSettingsScrollRetentionActive provides (current == tab),
                    ) {
                        pageStates.SaveableStateProvider(current.name) {
                            when (current) {
                                AppTab.UPLOAD -> UploadScreen(viewModel, onCompression = {
                                    settingsPage = SettingsPage.COMPRESSION
                                    tab = AppTab.SETTINGS
                                })
                                AppTab.FILES -> RemoteFilesScreen(viewModel)
                                AppTab.SETTINGS -> SettingsScreen(viewModel, settingsPage,
                                    onOverlayChanged = { settingsOverlayVisible = it },
                                    navigate = { settingsPage = it })
                            }
                        }
                    }
                }
                val detailVisible = tab == AppTab.SETTINGS && (settingsPage != SettingsPage.HOME || settingsOverlayVisible) && maxWidth < 840.dp
                if (!keyboardVisible && !detailVisible) {
                    FloatingNavigation(tab, { tab = it }, Modifier.align(Alignment.BottomCenter))
                }
                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding().padding(bottom = if (keyboardVisible || detailVisible) 8.dp else 104.dp))
            }
        }
    }
}

@Composable
private fun FloatingNavigation(selected: AppTab, onSelect: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp).widthIn(max = 380.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Row(Modifier.padding(7.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            AppTab.entries.forEach { item ->
                val active = item == selected
                val container by animateColorAsState(
                    if (active) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    tween(250), label = "navigationBackground",
                )
                val foreground by animateColorAsState(
                    if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                    tween(250), label = "navigationForeground",
                )
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(container)
                        .selectable(active, role = Role.Tab, onClick = { onSelect(item) })
                        .padding(horizontal = 6.dp, vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(item.icon, null, Modifier.size(22.dp), tint = foreground)
                    Text(item.label, style = MaterialTheme.typography.labelMedium, color = foreground)
                }
            }
        }
    }
}
