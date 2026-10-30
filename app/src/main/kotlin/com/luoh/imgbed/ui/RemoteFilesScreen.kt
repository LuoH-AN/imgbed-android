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

@Composable
internal fun RemoteFilesScreen(viewModel: AppViewModel) {
    val page by viewModel.remote.collectAsStateWithLifecycle()
    val directory by viewModel.remoteDirectory.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val loaded by viewModel.remoteLoaded.collectAsStateWithLifecycle()
    val loading by viewModel.remoteLoading.collectAsStateWithLifecycle()
    val error by viewModel.remoteError.collectAsStateWithLifecycle()
    val active = LocalSettingsScrollRetentionActive.current
    var search by rememberSaveable(directory) { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(FileFilter.ALL) }
    var newestFirst by rememberSaveable { mutableStateOf(true) }
    var selectedFile by remember { mutableStateOf<RemoteFile?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val files = remember(page, filter, newestFirst) {
        page.files.filter { when (filter) { FileFilter.ALL -> true; FileFilter.IMAGES -> it.isImage; FileFilter.OTHER -> !it.isImage } }
            .let { if (newestFirst) it.sortedByDescending { file -> file.timestamp } else it.sortedBy { file -> file.fileName.lowercase() } }
    }
    BackHandler(active && directory.isNotBlank()) { viewModel.goParentDirectory() }
    LaunchedEffect(Unit) { if (!loaded && !busy && error.isBlank()) viewModel.refreshRemote() }
    LaunchedEffect(active) { if (!active) selectedFile = null }
    PageScaffold("文件", actions = {
        RoundAction(Icons.Default.Refresh, "刷新", { viewModel.refreshRemote(search) }, enabled = !busy)
    }) {
        item {
            OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth().padding(bottom = 24.dp),
                label = { Text("搜索") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { IconButton({ keyboard?.hide(); viewModel.refreshRemote(search) }, enabled = !busy) { Icon(Icons.Default.Search, "搜索文件") } },
                singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); viewModel.refreshRemote(search) }))
        }
        item {
            SettingsGroup("筛选", listOf(
                { EnumMenu(Icons.Default.FilterList, "类型", filter, FileFilter.entries, { it.label }) { filter = it } },
                { EnumMenu(Icons.Default.Sort, "排序", newestFirst, listOf(true, false), { if (it) "最新" else "名称" }) { newestFirst = it } },
            ))
        }
        item {
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (directory.isNotBlank()) IconButton(viewModel::goParentDirectory, enabled = !busy) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "上级目录") }
                Text(if (directory.isBlank()) "根目录" else directory, Modifier.weight(1f).padding(start = 16.dp),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (loaded) Text("${files.size} / ${page.totalCount}", Modifier.padding(end = 16.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = 16.dp)) }
        if (error.isNotBlank()) item {
            Column(Modifier.padding(16.dp)) {
                Text(error, color = MaterialTheme.colorScheme.error)
                TextButton({ viewModel.refreshRemote(search) }, enabled = !busy) { Text("重试") }
            }
        }
        itemsIndexed(page.directories, key = { _, folder -> "dir:$folder" }) { index, folder ->
            GroupItem(index, page.directories.size + files.size) {
                SettingsItem(headlineContent = { Text(folder.substringAfterLast('/'), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingContent = { Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.primary) },
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
            Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (loaded) "暂无文件" else "尚未加载")
                if (!loaded) TextButton({ viewModel.refreshRemote(search) }, enabled = !busy) { Text("加载") }
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
            item { SettingsRow(Icons.Default.ContentCopy, "复制链接", onClick = { clipboard.setText(AnnotatedString(viewModel.remoteUrl(file))); state.requestDismiss() }) }
            item { SettingsRow(Icons.Default.Share, "分享链接", onClick = { shareText(context, viewModel.remoteUrl(file)); state.requestDismiss() }) }
            item { SettingsRow(Icons.Default.Edit, "重命名", enabled = !busy, onClick = { input = file.fileName; dialog = "rename" }) }
            item { SettingsRow(Icons.Default.DriveFileMove, "移动", enabled = !busy, onClick = { input = file.directory.trim('/'); dialog = "move" }) }
            item { SettingsRow(Icons.Default.Delete, "删除", enabled = !busy, onClick = { dialog = "delete" }) }
        }
    }
    if (dialog != null) AlertDialog(
        onDismissRequest = { dialog = null },
        title = { Text(when (dialog) { "rename" -> "重命名"; "move" -> "移动文件"; else -> "删除文件" }) },
        text = {
            if (dialog == "delete") Text("删除 ${file.fileName}？")
            else OutlinedTextField(input, { input = it }, label = { Text(if (dialog == "rename") "文件名" else "目标目录（留空为根目录）") }, singleLine = true)
        },
        confirmButton = {
            TextButton({
                when (dialog) { "rename" -> viewModel.renameRemote(file, input); "move" -> viewModel.moveRemote(file, input); else -> viewModel.deleteRemote(file) }
                dialog = null
                state.requestDismiss()
            }, enabled = !busy && (dialog != "rename" || (input.isNotBlank() && '/' !in input))) { Text("确定") }
        },
        dismissButton = { TextButton({ dialog = null }) { Text("取消") } },
    )
}
