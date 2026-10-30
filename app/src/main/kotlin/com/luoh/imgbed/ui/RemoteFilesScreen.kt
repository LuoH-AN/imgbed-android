package com.luoh.imgbed.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luoh.imgbed.model.RemoteFile

private enum class FileFilter(val label: String) { ALL("全部"), IMAGES("图片"), OTHER("其他文件") }

@Composable
internal fun RemoteFilesScreen(viewModel: AppViewModel) {
    val page by viewModel.remote.collectAsStateWithLifecycle()
    val directory by viewModel.remoteDirectory.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val loaded by viewModel.remoteLoaded.collectAsStateWithLifecycle()
    val loading by viewModel.remoteLoading.collectAsStateWithLifecycle()
    val error by viewModel.remoteError.collectAsStateWithLifecycle()
    var search by rememberSaveable(directory) { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(FileFilter.ALL) }
    var newestFirst by rememberSaveable { mutableStateOf(true) }
    val keyboard = LocalSoftwareKeyboardController.current
    val visibleFiles = remember(page, filter, newestFirst) {
        page.files.filter { when (filter) {
            FileFilter.ALL -> true
            FileFilter.IMAGES -> it.isImage
            FileFilter.OTHER -> !it.isImage
        } }.let { files -> if (newestFirst) files.sortedByDescending { it.timestamp } else files.sortedBy { it.fileName.lowercase() } }
    }
    BackHandler(directory.isNotBlank()) { viewModel.goParentDirectory() }
    LaunchedEffect(Unit) { if (!loaded && !busy && error.isBlank()) viewModel.refreshRemote() }
    PageScaffold("云端文件", "整理、寻找，再把喜欢的分享出去。", eyebrow = "IMGBED  /  文件库", actions = {
        RoundAction(Icons.Rounded.Refresh, "刷新文件", { viewModel.refreshRemote(search) }, enabled = !busy)
    }) {
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Icon(Icons.Rounded.CloudQueue, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (loaded) "${page.totalCount} 份云端收藏" else "你的随身文件库", style = MaterialTheme.typography.titleLarge)
                        Text(if (directory.isBlank()) "所有文件，从这里开始" else "当前目录 /$directory",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        item {
            TextField(
                search, { search = it }, Modifier.fillMaxWidth(),
                placeholder = { Text("搜索文件名称") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = { IconButton({ keyboard?.hide(); viewModel.refreshRemote(search) }, enabled = !busy) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, "搜索")
                } },
                singleLine = true, shape = RoundedCornerShape(22.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); viewModel.refreshRemote(search) }),
            )
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FileFilter.entries.forEach { item ->
                    FilterChip(filter == item, { filter = item }, label = { Text(item.label) }, shape = RoundedCornerShape(16.dp),
                        leadingIcon = if (filter == item) ({ Icon(Icons.Rounded.Check, null, Modifier.size(16.dp)) }) else null)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (directory.isNotBlank()) IconButton(viewModel::goParentDirectory, enabled = !busy) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "上级目录")
                }
                Column(Modifier.weight(1f)) {
                    Text(if (directory.isBlank()) "全部文件" else directory.substringAfterLast('/'), style = MaterialTheme.typography.titleMedium)
                    if (loaded) Text("已载入 ${page.files.size} 个 · 显示 ${visibleFiles.size} 个", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton({ newestFirst = !newestFirst }) {
                    Icon(Icons.Rounded.Sort, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp))
                    Text(if (newestFirst) "最新" else "名称")
                }
            }
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (error.isNotBlank()) item {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.errorContainer) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("暂时无法读取文件", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                    TextButton({ viewModel.refreshRemote(search) }, enabled = !busy) { Text("重新加载") }
                }
            }
        }
        items(page.directories, key = { "dir:$it" }) { folder ->
            Surface(onClick = { viewModel.enterDirectory(folder) }, enabled = !busy,
                shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(Icons.Rounded.Folder, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Column(Modifier.weight(1f)) {
                        Text(folder.substringAfterLast('/'), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("文件夹", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                    Icon(Icons.Rounded.ChevronRight, null)
                }
            }
        }
        items(visibleFiles, key = { "file:${it.id}" }) { file -> RemoteFileCard(viewModel, file, busy) }
        if (visibleFiles.isEmpty() && page.directories.isEmpty() && !loading && error.isBlank()) item {
            EmptyState(Icons.Rounded.FolderOpen,
                if (!loaded) "连接你的云端" else if (filter != FileFilter.ALL || search.isNotBlank()) "没有找到匹配的文件" else "这里还是空的",
                if (!loaded) "刷新即可读取文件，请先在设置中配置连接。" else "上传新的文件，或换个关键词再试试。") {
                if (!loaded) TextButton({ viewModel.refreshRemote(search) }, enabled = !busy) { Text("读取文件") }
            }
        }
    }
}

@Composable
private fun RemoteFileCard(viewModel: AppViewModel, file: RemoteFile, busy: Boolean) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var input by rememberSaveable { mutableStateOf("") }
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Icon(if (file.isImage) Icons.Rounded.Image else Icons.Rounded.Description, null,
                    Modifier.padding(16.dp).size(26.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(file.fileName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${file.sizeLabel} · ${file.channel.ifBlank { file.mimeType }}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box {
                IconButton({ menu = true }) { Icon(Icons.Rounded.MoreVert, "${file.fileName} 的操作") }
                DropdownMenu(menu, onDismissRequest = { menu = false }, shape = RoundedCornerShape(24.dp)) {
                    DropdownMenuItem({ Text("复制链接") }, { menu = false; clipboard.setText(AnnotatedString(viewModel.remoteUrl(file))) }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) })
                    DropdownMenuItem({ Text("分享链接") }, { menu = false; shareText(context, viewModel.remoteUrl(file)) }, leadingIcon = { Icon(Icons.Rounded.Share, null) })
                    DropdownMenuItem({ Text("重命名") }, { menu = false; input = file.fileName; dialog = "rename" }, enabled = !busy, leadingIcon = { Icon(Icons.Rounded.Edit, null) })
                    DropdownMenuItem({ Text("移动") }, { menu = false; input = file.directory.trim('/'); dialog = "move" }, enabled = !busy, leadingIcon = { Icon(Icons.Rounded.DriveFileMove, null) })
                    DropdownMenuItem({ Text("删除", color = MaterialTheme.colorScheme.error) }, { menu = false; dialog = "delete" }, enabled = !busy, leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) })
                }
            }
        }
    }
    if (dialog != null) AlertDialog(
        onDismissRequest = { dialog = null },
        icon = { Icon(if (dialog == "delete") Icons.Rounded.DeleteOutline else Icons.Rounded.Edit, null) },
        title = { Text(when (dialog) { "rename" -> "重命名"; "move" -> "移动文件"; else -> "删除文件" }) },
        text = {
            if (dialog == "delete") Text("确定删除 ${file.fileName}？此操作会删除远端文件。")
            else OutlinedTextField(input, { input = it }, label = { Text(if (dialog == "rename") "新文件名" else "目标目录，留空为根目录") },
                singleLine = true, shape = RoundedCornerShape(16.dp))
        },
        confirmButton = {
            TextButton(onClick = {
                when (dialog) {
                    "rename" -> viewModel.renameRemote(file, input)
                    "move" -> viewModel.moveRemote(file, input)
                    else -> viewModel.deleteRemote(file)
                }
                dialog = null
            }, enabled = !busy && (dialog != "rename" || (input.isNotBlank() && '/' !in input))) { Text("确定") }
        },
        dismissButton = { TextButton({ dialog = null }) { Text("取消") } },
    )
}
