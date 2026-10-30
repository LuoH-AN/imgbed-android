package com.luoh.imgbed.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luoh.imgbed.model.CompressionSettings
import com.luoh.imgbed.model.NamingMode
import com.luoh.imgbed.model.OutputFormat
import com.luoh.imgbed.model.UploadChannel

internal enum class SettingsPage(val title: String, val description: String) {
    HOME("偏好设置", "让 ImgBed 更合你的习惯。"),
    CONNECTION("服务连接", "连接自己的图床，一切准备就绪。"),
    UPLOAD("上传偏好", "选择文件在云端的归处。"),
    COMPRESSION("图片处理", "画质与体积，找到刚好的平衡。"),
    NAMING("文件命名", "给每个文件一个有迹可循的名字。"),
}

@Composable
internal fun SettingsScreen(viewModel: AppViewModel, page: SettingsPage, navigate: (SettingsPage) -> Unit) {
    val compression by viewModel.compression.collectAsStateWithLifecycle()
    val upload by viewModel.upload.collectAsStateWithLifecycle()
    val token by viewModel.token.collectAsStateWithLifecycle()
    val savedPages = rememberSaveableStateHolder()
    BackHandler(page != SettingsPage.HOME) { navigate(SettingsPage.HOME) }
    Crossfade(page, animationSpec = tween(220), label = "settingsDetail") { destination ->
        savedPages.SaveableStateProvider(destination.name) {
            PageScaffold(destination.title, destination.description, eyebrow = "IMGBED  /  偏好",
                onBack = if (destination == SettingsPage.HOME) null else ({ navigate(SettingsPage.HOME) })) {
                when (destination) {
                    SettingsPage.HOME -> {
                        item {
                            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primary) {
                                            Icon(Icons.Rounded.CloudQueue, null, Modifier.padding(16.dp).size(30.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                        }
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("你的图床，你做主", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            Text("本地处理 · 自由存储", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
                                    StatusPill(if (token.isBlank()) "尚未配置 API Token" else "API Token 已配置")
                                }
                            }
                        }
                        item {
                            SettingsGroup("云端与存储", listOf(
                                { SettingsRow(Icons.Rounded.Dns, "服务连接", upload.serverUrl.ifBlank { "设置图床地址与访问令牌" }, onClick = { navigate(SettingsPage.CONNECTION) }) },
                                { SettingsRow(Icons.Rounded.CloudUpload, "上传偏好", "${upload.channel.label} · ${upload.uploadFolder.ifBlank { "根目录" }}", onClick = { navigate(SettingsPage.UPLOAD) }) },
                            ))
                        }
                        item {
                            SettingsGroup("文件与画质", listOf(
                                { SettingsRow(Icons.Rounded.AutoAwesome, "图片处理", if (compression.enabled) "${compression.format.label} · 质量 ${compression.quality}" else "保持原文件", onClick = { navigate(SettingsPage.COMPRESSION) }) },
                                { SettingsRow(Icons.Rounded.TextFields, "文件命名", upload.namingMode.label, onClick = { navigate(SettingsPage.NAMING) }) },
                            ))
                        }
                        item {
                            SettingsGroup("关于", listOf(
                                { SettingsRow(Icons.Rounded.Palette, "森林色调", "柔和的灰绿配色 · 跟随系统深浅主题") },
                                { SettingsRow(Icons.Rounded.VerifiedUser, "留在本地的安心", "图片在设备上处理，访问令牌加密保存。") },
                            ))
                        }
                        item {
                            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                                Text("IMGBED  /  把美好轻放云端", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    SettingsPage.CONNECTION -> {
                        item {
                            SettingsGroup("图床服务", listOf(
                                { SettingsField("ImgBed 地址", upload.serverUrl, { viewModel.saveUpload(upload.copy(serverUrl = it)) }, Icons.Rounded.Language,
                                    hint = "例如 https://i.luoh.org", keyboardType = KeyboardType.Uri) },
                                { TokenField(token, viewModel::saveToken) },
                            ))
                        }
                        item { InfoCard(Icons.Rounded.Key, "按需配置权限", "上传需要 upload 权限；浏览、删除、重命名和移动还需要 list、delete、manage 权限。") }
                    }
                    SettingsPage.UPLOAD -> {
                        item {
                            SettingsGroup("存储位置", listOf(
                                { EnumMenu(Icons.Rounded.CloudQueue, "上传渠道", upload.channel, UploadChannel.entries, { it.label }) { viewModel.saveUpload(upload.copy(channel = it)) } },
                                { SettingsField("指定渠道名称", upload.channelName, { viewModel.saveUpload(upload.copy(channelName = it)) }, Icons.Rounded.Storage, "可选，留空使用默认渠道") },
                                { SettingsField("默认上传目录", upload.uploadFolder, { viewModel.saveUpload(upload.copy(uploadFolder = it)) }, Icons.Rounded.FolderOpen, "留空上传到根目录") },
                            ))
                        }
                        item {
                            SettingsGroup("上传策略", listOf(
                                { ToggleRow(Icons.Rounded.VerifiedUser, "禁止服务端再次压缩", "Telegram 上传时保留本地处理结果", upload.disableServerCompression) { viewModel.saveUpload(upload.copy(disableServerCompression = it)) } },
                            ))
                        }
                    }
                    SettingsPage.NAMING -> {
                        item {
                            SettingsGroup("命名方式", listOf(
                                { EnumMenu(Icons.Rounded.TextFields, "命名规则", upload.namingMode, NamingMode.entries, { it.label }) { viewModel.saveUpload(upload.copy(namingMode = it)) } },
                            ))
                        }
                        if (upload.namingMode == NamingMode.TEMPLATE) {
                            item {
                                SettingsGroup("自定义模板", listOf(
                                    { SettingsField("命名模板", upload.namingTemplate, { viewModel.saveUpload(upload.copy(namingTemplate = it)) }, Icons.Rounded.Code, "例如 {yyyy}{MM}{dd}_{name}", singleLine = false) },
                                ))
                            }
                            item { InfoCard(Icons.Rounded.DataObject, "可用变量", "{name} 原名称 · {ext} 扩展名\n{yyyy} {MM} {dd} 年月日\n{HH} {mm} {ss} 时分秒\n{uuid} 随机标识 · {index} 文件序号") }
                        } else {
                            item { InfoCard(Icons.Rounded.TextSnippet, upload.namingMode.label, when (upload.namingMode) {
                                NamingMode.ORIGINAL -> "保留选中文件的名称，转换图片格式时自动更新扩展名。"
                                NamingMode.TIMESTAMP -> "使用上传时的时间为文件命名，方便按时间整理。"
                                NamingMode.UUID -> "使用随机标识生成文件名。"
                                else -> "把命名交给图床服务，由服务端生成文件名称。"
                            }) }
                        }
                    }
                    SettingsPage.COMPRESSION -> {
                        item { CompressionPresets(compression, viewModel::saveCompression) }
                        item {
                            SettingsGroup("处理方式", listOf(
                                { ToggleRow(Icons.Rounded.AutoAwesome, "启用图片压缩", "非图片文件始终原样上传", compression.enabled) { viewModel.saveCompression(compression.copy(enabled = it)) } },
                            ))
                        }
                        if (compression.enabled) {
                            item {
                                SettingsGroup("画质与格式", buildList<@Composable () -> Unit> {
                                    add { EnumMenu(Icons.Rounded.Image, "输出格式", compression.format, OutputFormat.entries, { it.label }) { viewModel.saveCompression(compression.copy(format = it)) } }
                                    if (compression.format.lossy || compression.format == OutputFormat.KEEP) add {
                                        SliderSetting("图片质量", compression.quality, "更小体积", "更好画质", 1f..100f, 98) { viewModel.saveCompression(compression.copy(quality = it)) }
                                    }
                                    if (compression.format == OutputFormat.AVIF) add {
                                        SliderSetting("AVIF 编码速度", compression.avifSpeed, "更精细", "更快", 0f..10f, 9) { viewModel.saveCompression(compression.copy(avifSpeed = it)) }
                                    }
                                })
                            }
                            item {
                                SettingsGroup("尺寸与体积", listOf(
                                    { NumberField("最长边像素", compression.maxLongEdge, 16384, "0 表示不缩放，最大 16384 px") { viewModel.saveCompression(compression.copy(maxLongEdge = it)) } },
                                    { NumberField("目标大小 / KB", compression.targetSizeKb, Int.MAX_VALUE, "0 表示不限体积") { viewModel.saveCompression(compression.copy(targetSizeKb = it)) } },
                                    { ToggleRow(Icons.Rounded.Shield, "更大时保留原文件", "避免处理后反而增加文件体积", compression.skipIfLarger) { viewModel.saveCompression(compression.copy(skipIfLarger = it)) } },
                                ))
                            }
                        }
                        item { InfoCard(Icons.Rounded.Info, "给原作多一份照顾", "GIF、SVG 和动画 WebP 默认原样上传，保留动画与矢量内容。") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompressionPresets(settings: CompressionSettings, onChange: (CompressionSettings) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading("从一个方案开始")
        val balanced = settings.enabled && settings.format == OutputFormat.WEBP_LOSSY && settings.quality == 82 && settings.maxLongEdge == 2560 && settings.targetSizeKb == 0 && settings.skipIfLarger
        val small = settings.enabled && settings.format == OutputFormat.AVIF && settings.quality == 62 && settings.maxLongEdge == 1920 && settings.targetSizeKb == 1024 && settings.skipIfLarger
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PresetTile("均衡", "日常分享", Icons.Rounded.Balance, balanced, Modifier.weight(1f)) {
                onChange(settings.copy(enabled = true, format = OutputFormat.WEBP_LOSSY, quality = 82, maxLongEdge = 2560, targetSizeKb = 0, skipIfLarger = true))
            }
            PresetTile("更小", "节省空间", Icons.Rounded.Compress, small, Modifier.weight(1f)) {
                onChange(settings.copy(enabled = true, format = OutputFormat.AVIF, quality = 62, maxLongEdge = 1920, targetSizeKb = 1024, skipIfLarger = true))
            }
            PresetTile("原文件", "保留原貌", Icons.Rounded.HighQuality, !settings.enabled, Modifier.weight(1f)) {
                onChange(settings.copy(enabled = false, targetSizeKb = 0))
            }
        }
    }
}

@Composable
private fun PresetTile(title: String, subtitle: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(selected = selected, onClick = onClick, modifier = modifier, shape = RoundedCornerShape(22.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(if (selected) Icons.Rounded.CheckCircle else icon, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsField(label: String, value: String, onChange: (String) -> Unit, icon: ImageVector, hint: String = "",
    keyboardType: KeyboardType = KeyboardType.Text, singleLine: Boolean = true) {
    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
        OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) },
            shape = RoundedCornerShape(16.dp), singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            supportingText = if (hint.isBlank()) null else ({ Text(hint) }))
    }
}

@Composable
private fun TokenField(token: String, onChange: (String) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Key, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Text("API Token", style = MaterialTheme.typography.titleMedium)
        }
        OutlinedTextField(token, onChange, Modifier.fillMaxWidth(), label = { Text("API Token") }, singleLine = true,
            shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = { IconButton({ visible = !visible }) { Icon(if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if (visible) "隐藏令牌" else "显示令牌") } },
            supportingText = { Text("加密保存在此设备，不参与系统备份") })
    }
}

@Composable
private fun NumberField(label: String, value: Int, maximum: Int, hint: String, onChange: (Int) -> Unit) {
    var input by rememberSaveable { mutableStateOf(value.toString()) }
    LaunchedEffect(value) { if (input.toIntOrNull() != value && input.isNotEmpty()) input = value.toString() }
    SettingsField(label, input, { raw ->
        if (raw.isEmpty()) input = raw
        else if (raw.all(Char::isDigit)) raw.toIntOrNull()?.takeIf { it <= maximum }?.let { input = raw; onChange(it) }
    }, Icons.Rounded.AspectRatio, hint, KeyboardType.Number)
}

@Composable
private fun SliderSetting(title: String, value: Int, start: String, end: String, range: ClosedFloatingPointRange<Float>, steps: Int, onChange: (Int) -> Unit) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            StatusPill(value.toString())
        }
        Slider(value.toFloat(), { onChange(it.toInt()) }, valueRange = range, steps = steps)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(start, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(end, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoCard(icon: ImageVector, title: String, text: String) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        SettingsRow(icon, title, text)
    }
}
