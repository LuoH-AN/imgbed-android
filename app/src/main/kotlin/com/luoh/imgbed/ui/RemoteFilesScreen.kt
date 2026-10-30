package com.luoh.imgbed.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
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
import com.luoh.imgbed.ui.agora.*

private enum class FileFilter(val label: String) { ALL("全部"), IMAGES("图片"), OTHER("其他文件") }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RemoteFilesScreen(viewModel: AppViewModel, onConnection: () -> Unit, onUpload: () -> Unit) {
    val page by viewModel.remote.collectAsStateWithLifecycle()
    val directory by viewModel.remoteDirectory.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val loaded by viewModel.remoteLoaded.collectAsStateWithLifecycle()
    val loading by viewModel.remoteLoading.collectAsStateWithLifecycle()
    val error by viewModel.remoteError.collectAsStateWithLifecycle()
    val token by viewModel.token.collectAsStateWithLifecycle()
    val query by viewModel.remoteQuery.collectAsStateWithLifecycle()
    val active = LocalSettingsScrollRetentionActive.current
    var search by rememberSaveable(directory) { mutableStateOf(query) }
    var filter by rememberSaveable { mutableStateOf(FileFilter.ALL) }
    var newestFirst by rememberSaveable { mutableStateOf(true) }
    var selectedFile by remember { mutableStateOf<RemoteFile?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val files = remember(page, filter, newestFirst) {
        page.files.filter { when (filter) { FileFilter.ALL -> true; FileFilter.IMAGES -> it.isImage; FileFilter.OTHER -> !it.isImage } }
            .let { if (newestFirst) it.sortedByDescending { file -> file.timestamp } else it.sortedBy { file -> file.fileName.lowercase() } }
    }
    fun clearSearch() { search = ""; keyboard?.hide(); viewModel.refreshRemote("") }
    BackHandler(active && (directory.isNotBlank() || query.isNotBlank())) {
        if (query.isNotBlank()) clearSearch() else viewModel.goParentDirectory()
    }
    LaunchedEffect(query) { search = query }
    LaunchedEffect(active, busy, token) {
        if (active && token.isNotBlank() && !loaded && !busy && error.isBlank()) viewModel.refreshRemote()
    }
    LaunchedEffect(active) { if (!active) selectedFile = null }
    PageScaffold("文件", actions = {
        if (token.isNotBlank()) RoundAction(Icons.Default.Refresh, "刷新", { viewModel.refreshRemote() }, enabled = !busy)
    }) {
        if (token.isBlank()) {
            item { EmptyState(Icons.Default.CloudOff, "连接图床后查看文件", "设置服务连接", onConnection) }
            return@PageScaffold
        }
        item {
            OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth().padding(bottom = 8.dp),
                placeholder = { Text("搜索文件名") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (search.isNotEmpty() || query.isNotEmpty()) IconButton(::clearSearch, enabled = !busy) { Icon(Icons.Default.Close, "清除搜索") }
                },
                singleLine = true, enabled = !busy, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); viewModel.refreshRemote(search) }))
            FlowRow(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)) {
                FileFilter.entries.forEach { choice ->
                    FilterChip(selected = filter == choice, onClick = { filter = choice }, label = { Text(choice.label) })
                }
                var sortMenu by remember { mutableStateOf(false) }
                Box {
                    AssistChip(onClick = { sortMenu = true }, label = { Text(if (newestFirst) "最新优先" else "按名称") },
                        leadingIcon = { Icon(Icons.Default.Sort, null, Modifier.size(18.dp)) })
                    AgoraDropdownMenu(sortMenu, { sortMenu = false }) {
                        listOf(true, false).forEach { newest ->
                            AgoraDropdownMenuItem(text = { Text(if (newest) "最新优先" else "按名称") },
                                leadingIcon = { if (newest == newestFirst) Icon(Icons.Default.Check, null) },
                                onClick = { newestFirst = newest; sortMenu = false })
                        }
                    }
                }
            }
            if (query.isNotBlank()) Text("搜索：$query", Modifier.padding(start = 16.dp, bottom = 12.dp),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (directory.isNotBlank()) IconButton(viewModel::goParentDirectory, enabled = !busy) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "上级目录") }
                Text(if (directory.isBlank()) "根目录" else directory, Modifier.weight(1f).padding(start = 16.dp),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (loaded) Text("${files.size} 个文件", Modifier.padding(end = 16.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = 16.dp)) }
        if (error.isNotBlank()) item {
            Column(Modifier.padding(16.dp)) {
                Text(error, color = MaterialTheme.colorScheme.error)
                Row {
                    TextButton({ viewModel.refreshRemote() }, enabled = !busy) { Text("重试") }
                    TextButton(onConnection, enabled = !busy) { Text("检查连接") }
                }
            }
        }
        itemsIndexed(page.directories, key = { _, folder -> "dir:$folder" }) { index, folder ->
            GroupItem(index, page.directories.size + files.size) {
                SettingsItem(headlineContent = { Text(folder.substringAfterLast('/'), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingContent = { Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "打开目录") },
                    modifier = Modifier.clickable(enabled = !busy) { viewModel.enterDirectory(folder) })
            }
        }
        itemsIndexed(files, key = { _, file -> "file:${file.id}" }) { index, file ->
            GroupItem(index + page.directories.size, page.directories.size + files.size) {
                SettingsItem(
                    headlineContent = { Text(file.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text("${file.sizeLabel} · ${file.channel.ifBlank { file.mimeType }}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingContent = { Icon(if (file.isImage) Icons.Default.Image else Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { IconButton({ selectedFile = file }) { Icon(Icons.Default.MoreVert, "${file.fileName} 的操作") } },
                    modifier = Modifier.clickable { selectedFile = file },
                )
            }
        }
        if (files.isEmpty() && page.directories.isEmpty() && !loading && error.isBlank()) item {
            when {
                query.isNotBlank() -> EmptyState(Icons.Default.SearchOff, "没有找到匹配的文件", "清除搜索", ::clearSearch)
                filter != FileFilter.ALL -> EmptyState(Icons.Default.FilterList, "暂无${filter.label}", "查看全部文件", { filter = FileFilter.ALL })
                loaded -> EmptyState(Icons.Default.FolderOpen, "此目录还没有文件", "去上传", onUpload)
                else -> EmptyState(Icons.Default.CloudQueue, "等待加载文件")
            }
        }
        if (loaded && page.files.size < page.totalCount) item {
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("已加载 ${page.files.size} / ${page.totalCount} 个文件", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(viewModel::loadMoreRemote, enabled = !busy) { Text(if (loading) "正在加载" else "加载更多") }
            }
        }
    }
    selectedFile?.let { file -> RemoteFileActions(viewModel, file, busy) { selectedFile = null } }
}

@Composable
private fun RemoteFileActions(viewModel: AppViewModel, file: RemoteFile, busy: Boolean, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val state = rememberSmoothBottomSheetState()
    val list = rememberLazyListState()
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var input by rememberSaveable { mutableStateOf("") }
    SmoothBottomSheet(state = state, onDismissRequest = onDismiss,
        contentAtTop = { list.firstVisibleItemIndex == 0 && list.firstVisibleItemScrollOffset == 0 },
        header = {
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(file.fileName, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton({ state.requestDismiss() }) { Icon(Icons.Default.Close, "关闭") }
            }
        },
    ) {
        LazyColumn(state = list, modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            item { SettingsRow(Icons.Default.ContentCopy, "复制链接", onClick = { clipboard.setText(AnnotatedString(viewModel.remoteUrl(file))); viewModel.showMessage("链接已复制"); state.requestDismiss() }) }
            item { SettingsRow(Icons.Default.Share, "分享链接", onClick = { shareText(context, viewModel.remoteUrl(file)); state.requestDismiss() }) }
            item { SettingsRow(Icons.Default.Edit, "重命名", enabled = !busy, onClick = { input = file.fileName; dialog = "rename" }) }
            item { SettingsRow(Icons.Default.DriveFileMove, "移动", enabled = !busy, onClick = { input = file.directory.trim('/'); dialog = "move" }) }
            item {
                SettingsItem(headlineContent = { Text("删除", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable(enabled = !busy) { dialog = "delete" })
            }
        }
    }
    if (dialog != null) AlertDialog(
        onDismissRequest = { dialog = null },
        title = { Text(when (dialog) { "rename" -> "重命名"; "move" -> "移动文件"; else -> "删除文件" }) },
        text = {
            if (dialog == "delete") Text("删除 ${file.fileName}？此操作无法撤销。")
            else OutlinedTextField(input, { input = it }, label = { Text(if (dialog == "rename") "文件名" else "目标目录（留空为根目录）") }, singleLine = true)
        },
        confirmButton = {
            TextButton({
                when (dialog) { "rename" -> viewModel.renameRemote(file, input); "move" -> viewModel.moveRemote(file, input); else -> viewModel.deleteRemote(file) }
                dialog = null
                state.requestDismiss()
            }, enabled = !busy && (dialog != "rename" || (input.trim().isNotBlank() && '/' !in input && input.trim() != file.fileName)),
                colors = ButtonDefaults.textButtonColors(contentColor = if (dialog == "delete") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)) {
                Text(when (dialog) { "rename" -> "重命名"; "move" -> "移动"; else -> "删除" })
            }
        },
        dismissButton = { TextButton({ dialog = null }) { Text("取消") } },
    )
}
