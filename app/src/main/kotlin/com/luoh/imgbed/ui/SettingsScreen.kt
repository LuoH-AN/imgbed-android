package com.luoh.imgbed.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luoh.imgbed.model.NamingMode
import com.luoh.imgbed.model.OutputFormat
import com.luoh.imgbed.model.UploadChannel
import com.luoh.imgbed.ui.agora.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal enum class SettingsPage(val title: String, val icon: ImageVector) {
    HOME("设置", Icons.Default.Settings),
    CONNECTION("服务连接", Icons.Default.Dns),
    UPLOAD("上传偏好", Icons.Default.CloudUpload),
    COMPRESSION("图片处理", Icons.Default.Image),
    NAMING("文件命名", Icons.Default.TextFields),
    APPEARANCE("应用外观", Icons.Default.Palette),
    MOTION("应用动效", Icons.Default.Animation),
}

@Composable
internal fun SettingsScreen(viewModel: AppViewModel, page: SettingsPage, onOverlayChanged: (Boolean) -> Unit = {}, navigate: (SettingsPage) -> Unit) {
    val active = LocalSettingsScrollRetentionActive.current
    var lastDetail by rememberSaveable { mutableStateOf(SettingsPage.CONNECTION) }
    SideEffect { if (page != SettingsPage.HOME) lastDetail = page }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 840.dp) {
            SideEffect { onOverlayChanged(false) }
            val selected = if (page == SettingsPage.HOME) lastDetail else page
            BackHandler(active && page != SettingsPage.HOME) { navigate(SettingsPage.HOME) }
            Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                Column(Modifier.width(400.dp).fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)).background(MaterialTheme.colorScheme.background)) {
                    Text("设置", Modifier.padding(start = 24.dp, top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp, bottom = 24.dp),
                        style = MaterialTheme.typography.headlineSmall)
                    LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 112.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SettingsPage.entries.filter { it != SettingsPage.HOME }) { category ->
                            val color by animateColorAsState(if (selected == category) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                                tween(250), label = "settingsCategorySelection")
                            Surface(shape = RoundedCornerShape(24.dp), color = color) {
                                SettingsRow(category.icon, category.title, onClick = { navigate(category) })
                            }
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 16.dp).clipToBounds()) {
                    val parentPath = LocalSettingsScrollPath.current
                    Crossfade(selected, animationSpec = tween(250), label = "settingsCategory") { detail ->
                        CompositionLocalProvider(LocalSettingsScrollPath provides (parentPath + detail), LocalSettingsScrollRetentionActive provides (active && detail == selected)) {
                            SettingsDetail(viewModel, detail, onBack = null, reserveDock = true)
                        }
                    }
                }
            }
        } else {
            SideEffect { if (page != SettingsPage.HOME) onOverlayChanged(true) }
            PageScaffold("设置") {
                val groups = listOf(
                    "连接" to listOf(SettingsPage.CONNECTION),
                    "上传" to listOf(SettingsPage.UPLOAD, SettingsPage.COMPRESSION, SettingsPage.NAMING),
                    "个性化" to listOf(SettingsPage.APPEARANCE, SettingsPage.MOTION),
                )
                groups.forEach { (title, categories) ->
                    item {
                        SettingsGroup(title, categories.map { category ->
                            { SettingsRow(category.icon, category.title, onClick = { navigate(category) }) }
                        })
                    }
                }
            }
            BackHandler(active && page != SettingsPage.HOME) { navigate(SettingsPage.HOME) }
            SettingsOverlayHost(visible = page != SettingsPage.HOME, onDismiss = { navigate(SettingsPage.HOME) }, onExitFinished = { onOverlayChanged(false) }) {
                CompositionLocalProvider(LocalSettingsScrollRetentionActive provides (active && page != SettingsPage.HOME)) {
                    GuardedAnimatedContent(if (page == SettingsPage.HOME) lastDetail else page, forward = true) { detail ->
                        SettingsDetail(viewModel, detail, onBack = { navigate(SettingsPage.HOME) }, reserveDock = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsDetail(viewModel: AppViewModel, page: SettingsPage, onBack: (() -> Unit)?, reserveDock: Boolean) {
    val compression by viewModel.compression.collectAsStateWithLifecycle()
    val upload by viewModel.upload.collectAsStateWithLifecycle()
    val token by viewModel.token.collectAsStateWithLifecycle()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    PageScaffold(page.title, onBack, reserveDock) {
        when (page) {
            SettingsPage.CONNECTION -> item {
                SettingsGroup("图床服务", listOf(
                    { EditSetting(Icons.Default.Language, "ImgBed 地址", upload.serverUrl, keyboard = KeyboardType.Uri) { viewModel.saveUpload(upload.copy(serverUrl = it)) } },
                    { EditSetting(Icons.Default.Key, "API Token", token, password = true) { viewModel.saveToken(it) } },
                ))
            }
            SettingsPage.UPLOAD -> {
                item {
                    SettingsGroup("存储位置", listOf(
                        { EnumMenu(Icons.Default.CloudQueue, "上传渠道", upload.channel, UploadChannel.entries, { it.label }) { viewModel.saveUpload(upload.copy(channel = it)) } },
                        { EditSetting(Icons.Default.Storage, "渠道名称", upload.channelName, emptyValue = "默认") { viewModel.saveUpload(upload.copy(channelName = it)) } },
                        { EditSetting(Icons.Default.Folder, "上传目录", upload.uploadFolder, emptyValue = "根目录") { viewModel.saveUpload(upload.copy(uploadFolder = it)) } },
                    ))
                }
                item {
                    SettingsGroup("上传策略", listOf(
                        { ToggleRow(Icons.Default.VerifiedUser, "禁止服务端再次压缩", upload.disableServerCompression) { viewModel.saveUpload(upload.copy(disableServerCompression = it)) } },
                    ))
                }
            }
            SettingsPage.NAMING -> {
                item {
                    SettingsGroup("命名规则", buildList<@Composable () -> Unit> {
                        add { EnumMenu(Icons.Default.TextFields, "命名方式", upload.namingMode, NamingMode.entries, { it.label }) { viewModel.saveUpload(upload.copy(namingMode = it)) } }
                        if (upload.namingMode == NamingMode.TEMPLATE) add {
                            EditSetting(Icons.Default.Code, "命名模板", upload.namingTemplate, singleLine = false,
                                placeholder = "{name} {ext} {yyyy} {MM} {dd} {HH} {mm} {ss} {uuid} {index}") { viewModel.saveUpload(upload.copy(namingTemplate = it)) }
                        }
                    })
                }
            }
            SettingsPage.COMPRESSION -> {
                item {
                    SettingsGroup("处理方式", listOf(
                        { EnumMenu(Icons.Default.Tune, "快速方案", when {
                            !compression.enabled -> "原文件"
                            compression.format == OutputFormat.WEBP_LOSSY && compression.quality == 82 && compression.maxLongEdge == 2560 && compression.targetSizeKb == 0 && compression.skipIfLarger -> "均衡"
                            compression.format == OutputFormat.AVIF && compression.quality == 62 && compression.maxLongEdge == 1920 && compression.targetSizeKb == 1024 && compression.skipIfLarger -> "更小"
                            else -> "自定义"
                        }, listOf("均衡", "更小", "原文件"), { it }) {
                            viewModel.saveCompression(when (it) {
                                "均衡" -> compression.copy(enabled = true, format = OutputFormat.WEBP_LOSSY, quality = 82, maxLongEdge = 2560, targetSizeKb = 0, skipIfLarger = true)
                                "更小" -> compression.copy(enabled = true, format = OutputFormat.AVIF, quality = 62, maxLongEdge = 1920, targetSizeKb = 1024, skipIfLarger = true)
                                else -> compression.copy(enabled = false, targetSizeKb = 0)
                            })
                        } },
                        { ToggleRow(Icons.Default.AutoAwesome, "启用图片压缩", compression.enabled) { viewModel.saveCompression(compression.copy(enabled = it)) } },
                    ))
                }
                if (compression.enabled) {
                    item {
                        SettingsGroup("画质与格式", buildList<@Composable () -> Unit> {
                            add { EnumMenu(Icons.Default.Image, "输出格式", compression.format, OutputFormat.entries, { it.label }) { viewModel.saveCompression(compression.copy(format = it)) } }
                            if (compression.format.lossy || compression.format == OutputFormat.KEEP) add {
                                SliderSetting("图片质量", compression.quality, 1..100, Icons.Default.HighQuality, "%") { viewModel.saveCompression(compression.copy(quality = it)) }
                            }
                            if (compression.format == OutputFormat.AVIF) add {
                                SliderSetting("AVIF 编码速度", compression.avifSpeed, 0..10, Icons.Default.Speed) { viewModel.saveCompression(compression.copy(avifSpeed = it)) }
                            }
                        })
                    }
                    item {
                        SettingsGroup("尺寸与体积", listOf(
                            { NumberSetting("最长边", compression.maxLongEdge, 16384, "px") { viewModel.saveCompression(compression.copy(maxLongEdge = it)) } },
                            { NumberSetting("目标大小", compression.targetSizeKb, Int.MAX_VALUE, "KB") { viewModel.saveCompression(compression.copy(targetSizeKb = it)) } },
                            { ToggleRow(Icons.Default.Shield, "更大时保留原文件", compression.skipIfLarger) { viewModel.saveCompression(compression.copy(skipIfLarger = it)) } },
                        ))
                    }
                }
            }
            SettingsPage.APPEARANCE -> item { AppearanceSettings(appearance, viewModel::saveAppearance) }
            SettingsPage.MOTION -> item {
                SettingsGroup("应用动效", listOf(
                    { ToggleRow(Icons.Default.MotionPhotosOff, "减少动效", appearance.reduceMotion) { viewModel.saveAppearance(appearance.copy(reduceMotion = it)) } },
                ))
            }
            SettingsPage.HOME -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditSetting(icon: ImageVector, title: String, value: String, keyboard: KeyboardType = KeyboardType.Text,
    password: Boolean = false, emptyValue: String = "未设置", singleLine: Boolean = true, placeholder: String = "",
    enabled: Boolean = true, onSave: (String) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val active = LocalSettingsScrollRetentionActive.current
    LaunchedEffect(active, enabled) { if (!active || !enabled) editing = false }
    SettingsRow(icon, title, if (password && value.isNotBlank()) "已设置" else value.ifBlank { emptyValue }, enabled = enabled, onClick = {
        draft = value; visible = false; editing = true
    })
    if (editing) {
        val input = rememberTextFieldState(value)
        val text = if (password) draft else input.text.toString()
        val parsedUrl = if (keyboard == KeyboardType.Uri) text.trim().toHttpUrlOrNull() else null
        val valid = keyboard != KeyboardType.Uri || (parsedUrl != null && parsedUrl.username.isEmpty() && parsedUrl.password.isEmpty())
        AlertDialog(onDismissRequest = { editing = false }, title = { Text(title) }, text = {
            if (password) {
                OutlinedTextField(draft, { draft = it }, Modifier.fillMaxWidth(), label = { Text(title) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (!visible) PasswordVisualTransformation() else VisualTransformation.None,
                    trailingIcon = { IconButton({ visible = !visible }) { Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (visible) "隐藏" else "显示") } })
            } else {
                OutlinedTextField(state = input, modifier = Modifier.fillMaxWidth(), label = { Text(title) },
                    lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine(),
                    placeholder = if (placeholder.isBlank()) null else ({ Text(placeholder) }),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboard), isError = !valid,
                    supportingText = if (!valid) ({ Text("请输入有效的 http:// 或 https:// 地址") }) else null)
            }
        }, confirmButton = { TextButton({ onSave(text.trim()); editing = false }, enabled = valid) { Text("保存") } },
            dismissButton = { TextButton({ editing = false }) { Text("取消") } })
    }
}

@Composable
private fun NumberSetting(title: String, value: Int, maximum: Int, unit: String, onSave: (Int) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    val number = draft.toIntOrNull()
    val valid = number != null && number in 0..maximum
    val active = LocalSettingsScrollRetentionActive.current
    LaunchedEffect(active) { if (!active) editing = false }
    SettingsRow(Icons.Default.AspectRatio, title, if (value == 0) "不限" else "$value $unit", onClick = { draft = value.toString(); editing = true })
    if (editing) AlertDialog(onDismissRequest = { editing = false }, title = { Text(title) }, text = {
        OutlinedTextField(draft, { draft = it }, label = { Text("$unit（0 为不限）") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = draft.isNotBlank() && !valid,
            supportingText = if (draft.isNotBlank() && !valid) ({ Text("请输入 0–$maximum") }) else null)
    }, confirmButton = { TextButton({ number?.let(onSave); editing = false }, enabled = valid) { Text("保存") } },
        dismissButton = { TextButton({ editing = false }) { Text("取消") } })
}
