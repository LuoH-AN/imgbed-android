package com.luoh.imgbed.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luoh.imgbed.model.LocalDocument
import com.luoh.imgbed.model.NamingMode
import com.luoh.imgbed.model.OutputFormat
import com.luoh.imgbed.model.RemoteFile
import com.luoh.imgbed.model.TransferStatus
import com.luoh.imgbed.model.UploadChannel
import java.util.Locale

private enum class AppTab(val label: String) { UPLOAD("上传"), FILES("文件"), SETTINGS("设置") }

@Composable
fun ImgBedApp(viewModel: AppViewModel) {
    val context = LocalContext.current
    val colorScheme = if (android.os.Build.VERSION.SDK_INT >= 31) {
        if (androidx.compose.foundation.isSystemInDarkTheme()) dynamicDarkColorScheme(context)
        else dynamicLightColorScheme(context)
    } else if (androidx.compose.foundation.isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colorScheme) {
        var tab by remember { mutableStateOf(AppTab.UPLOAD) }
        val message by viewModel.message.collectAsStateWithLifecycle()
        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(message) {
            if (message.isNotBlank()) {
                snackbar.showSnackbar(message)
                viewModel.clearMessage()
            }
        }
        Scaffold(
            topBar = { AppTopBar(tab) },
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                    NavigationBarItem(tab == AppTab.UPLOAD, { tab = AppTab.UPLOAD }, icon = { Icon(Icons.Default.CloudUpload, null) }, label = { Text("上传") })
                    NavigationBarItem(tab == AppTab.FILES, { tab = AppTab.FILES }, icon = { Icon(Icons.Default.Folder, null) }, label = { Text("文件") })
                    NavigationBarItem(tab == AppTab.SETTINGS, { tab = AppTab.SETTINGS }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("设置") })
                }
            },
        ) { padding ->
            when (tab) {
                AppTab.UPLOAD -> UploadScreen(viewModel, Modifier.padding(padding))
                AppTab.FILES -> RemoteFilesScreen(viewModel, Modifier.padding(padding))
                AppTab.SETTINGS -> SettingsScreen(viewModel, Modifier.padding(padding))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(tab: AppTab) {
    TopAppBar(title = {
        Column {
            Text("ImgBed")
            Text(tab.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    })
}

@Composable
private fun UploadScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val compression by viewModel.compression.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { viewModel.addUris(it) }
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("图片在手机本地处理", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (compression.enabled) "输出 ${compression.format.label} · 质量 ${compression.quality} · 最长边 ${compression.maxLongEdge}px"
                        else "图片保持原文件，其他类型始终原样上传",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { picker.launch(arrayOf("*/*")) }) { Icon(Icons.Default.Add, null); Text("选择文件") }
                        if (documents.any { it.status == TransferStatus.SUCCESS }) {
                            TextButton(onClick = viewModel::clearCompleted) { Text("清除已完成") }
                        }
                    }
                }
            }
        }
        if (documents.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CloudUpload, null, Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("选择图片、视频、文档或任意文件")
                        Text("也可以从其他应用直接分享到 ImgBed", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        items(documents, key = { it.uri.toString() }) { file ->
            LocalFileCard(file, onRemove = { viewModel.remove(file.uri) })
        }
        if (documents.isNotEmpty()) {
            item {
                Button(
                    onClick = viewModel::uploadAll,
                    enabled = !busy && documents.any { it.status != TransferStatus.SUCCESS },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.CloudUpload, null)
                    Spacer(Modifier.size(8.dp))
                    Text(if (busy) "处理中…" else "开始上传 ${documents.size} 个文件")
                }
            }
        }
    }
}

@Composable
private fun LocalFileCard(file: LocalDocument, onRemove: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (file.isImage) Icons.Default.Image else Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(file.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${formatBytes(file.sizeBytes)} · ${file.mimeType}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                when (file.status) {
                    TransferStatus.SUCCESS -> Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    TransferStatus.ERROR -> Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                    TransferStatus.PROCESSING, TransferStatus.UPLOADING -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    TransferStatus.WAITING -> IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, "移除") }
                }
            }
            if (file.status == TransferStatus.UPLOADING) LinearProgressIndicator(progress = { file.progress }, modifier = Modifier.fillMaxWidth())
            if (file.error.isNotBlank()) Text(file.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            if (file.resultUrl.isNotBlank()) {
                Text(file.resultUrl, maxLines = 2, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Row {
                    TextButton(onClick = { clipboard.setText(AnnotatedString(file.resultUrl)) }) { Icon(Icons.Default.ContentCopy, null); Text("复制") }
                    TextButton(onClick = { shareText(context, file.resultUrl) }) { Icon(Icons.Default.Share, null); Text("分享") }
                }
            }
        }
    }
}

@Composable
private fun RemoteFilesScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val page by viewModel.remote.collectAsStateWithLifecycle()
    val directory by viewModel.remoteDirectory.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var search by remember { mutableStateOf("") }
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (directory.isNotBlank()) IconButton(onClick = viewModel::goParentDirectory) { Icon(Icons.Default.ArrowBack, "上级") }
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text(if (directory.isBlank()) "搜索全部文件" else "搜索 $directory") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { viewModel.refreshRemote(search) }, enabled = !busy) { Icon(Icons.Default.Refresh, "刷新") }
        }
        Text("${if (directory.isBlank()) "根目录" else directory} · ${page.totalCount} 个文件", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 10.dp))
        if (busy && page.files.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            items(page.directories, key = { "dir:$it" }) { folder ->
                Card(onClick = { viewModel.enterDirectory(folder) }) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary)
                        Text(folder.substringAfterLast('/'), Modifier.padding(start = 12.dp))
                    }
                }
            }
            items(page.files, key = { it.id }) { file -> RemoteFileCard(viewModel, file) }
            if (page.files.isEmpty() && page.directories.isEmpty() && !busy) {
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("还没有读取文件")
                        TextButton(onClick = { viewModel.refreshRemote(search) }) { Text("立即刷新") }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteFileCard(viewModel: AppViewModel, file: RemoteFile) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }
    Card {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (file.isImage) Icons.Default.Image else Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(file.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${file.sizeLabel} · ${file.channel.ifBlank { file.mimeType }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, "操作") }
                DropdownMenu(menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem({ Text("复制链接") }, onClick = { menu = false; clipboard.setText(AnnotatedString(viewModel.remoteUrl(file))) })
                    DropdownMenuItem({ Text("分享链接") }, onClick = { menu = false; shareText(context, viewModel.remoteUrl(file)) })
                    DropdownMenuItem({ Text("重命名") }, onClick = { menu = false; input = file.fileName; dialog = "rename" })
                    DropdownMenuItem({ Text("移动") }, onClick = { menu = false; input = file.directory.trim('/'); dialog = "move" })
                    DropdownMenuItem({ Text("删除", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; dialog = "delete" })
                }
            }
        }
    }
    if (dialog != null) {
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(when (dialog) { "rename" -> "重命名"; "move" -> "移动文件"; else -> "删除文件" }) },
            text = {
                if (dialog == "delete") Text("确定删除 ${file.fileName}？此操作会删除远端文件。")
                else OutlinedTextField(input, { input = it }, label = { Text(if (dialog == "rename") "新文件名" else "目标目录，留空为根目录") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    when (dialog) {
                        "rename" -> viewModel.renameRemote(file, input)
                        "move" -> viewModel.moveRemote(file, input)
                        else -> viewModel.deleteRemote(file)
                    }
                    dialog = null
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun SettingsScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val compression by viewModel.compression.collectAsStateWithLifecycle()
    val upload by viewModel.upload.collectAsStateWithLifecycle()
    val token by viewModel.token.collectAsStateWithLifecycle()
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).imePadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionTitle("连接")
        OutlinedTextField(upload.serverUrl, { viewModel.saveUpload(upload.copy(serverUrl = it)) }, label = { Text("ImgBed 地址") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(token, viewModel::saveToken, label = { Text("API Token") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth(), supportingText = { Text("文件管理需要 upload、list、delete、manage 权限") })
        EnumMenu("上传渠道", upload.channel, UploadChannel.entries, { it.label }) { viewModel.saveUpload(upload.copy(channel = it)) }
        OutlinedTextField(upload.channelName, { viewModel.saveUpload(upload.copy(channelName = it)) }, label = { Text("指定渠道名称（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(upload.uploadFolder, { viewModel.saveUpload(upload.copy(uploadFolder = it)) }, label = { Text("默认上传目录") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        HorizontalDivider()
        SectionTitle("命名")
        EnumMenu("命名规则", upload.namingMode, NamingMode.entries, { it.label }) { viewModel.saveUpload(upload.copy(namingMode = it)) }
        if (upload.namingMode == NamingMode.TEMPLATE) {
            OutlinedTextField(upload.namingTemplate, { viewModel.saveUpload(upload.copy(namingTemplate = it)) }, label = { Text("命名模板") }, modifier = Modifier.fillMaxWidth(), supportingText = { Text("支持 {name} {ext} {yyyy} {MM} {dd} {HH} {mm} {ss} {uuid} {index}") })
        }
        ToggleRow("禁止服务端再次压缩", "Telegram 上传时保留本地压缩结果", upload.disableServerCompression) { viewModel.saveUpload(upload.copy(disableServerCompression = it)) }

        HorizontalDivider()
        SectionTitle("本地图片压缩")
        Text("快速方案", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = {
                viewModel.saveCompression(compression.copy(enabled = true, format = OutputFormat.WEBP_LOSSY, quality = 82, maxLongEdge = 2560, targetSizeKb = 0, skipIfLarger = true))
            }, modifier = Modifier.weight(1f)) { Text("均衡") }
            FilledTonalButton(onClick = {
                viewModel.saveCompression(compression.copy(enabled = true, format = OutputFormat.AVIF, quality = 62, maxLongEdge = 1920, targetSizeKb = 1024, skipIfLarger = true))
            }, modifier = Modifier.weight(1f)) { Text("更小") }
            FilledTonalButton(onClick = {
                viewModel.saveCompression(compression.copy(enabled = false, targetSizeKb = 0))
            }, modifier = Modifier.weight(1f)) { Text("原文件") }
        }
        ToggleRow("启用图片压缩", "非图片文件不会被修改", compression.enabled) { viewModel.saveCompression(compression.copy(enabled = it)) }
        if (compression.enabled) {
            EnumMenu("输出格式", compression.format, OutputFormat.entries, { it.label }) { viewModel.saveCompression(compression.copy(format = it)) }
            Text("质量 ${compression.quality}")
            Slider(compression.quality.toFloat(), { viewModel.saveCompression(compression.copy(quality = it.toInt())) }, valueRange = 1f..100f, steps = 98)
            NumberField("最长边像素（0 表示不缩放）", compression.maxLongEdge) { viewModel.saveCompression(compression.copy(maxLongEdge = it)) }
            NumberField("目标大小 KB（0 表示不限）", compression.targetSizeKb) { viewModel.saveCompression(compression.copy(targetSizeKb = it)) }
            ToggleRow("结果更大时上传原文件", "避免压缩后反而增大体积", compression.skipIfLarger) { viewModel.saveCompression(compression.copy(skipIfLarger = it)) }
            if (compression.format == OutputFormat.AVIF) {
                Text("AVIF 编码速度 ${compression.avifSpeed}（数值越高越快）")
                Slider(compression.avifSpeed.toFloat(), { viewModel.saveCompression(compression.copy(avifSpeed = it.toInt())) }, valueRange = 0f..10f, steps = 9)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(value: String) = Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onChange)
    }
}

@Composable
private fun NumberField(label: String, value: Int, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { text -> text.filter(Char::isDigit).toIntOrNull()?.let(onChange) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun <T> EnumMenu(label: String, value: T, values: List<T>, text: (T) -> String, onChange: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        FilledTonalButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label：${text(value)}", modifier = Modifier.weight(1f))
            Text("⌄")
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            values.forEach { item ->
                DropdownMenuItem({ Text(text(item)) }, onClick = { expanded = false; onChange(item) })
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "大小未知"
    if (bytes < 1024) return "$bytes B"
    if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0)
    return String.format(Locale.ROOT, "%.2f MB", bytes / 1024.0 / 1024.0)
}

private fun shareText(context: Context, value: String) {
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, value)
    }, "分享链接"))
}
