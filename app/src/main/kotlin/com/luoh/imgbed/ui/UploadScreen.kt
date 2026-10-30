package com.luoh.imgbed.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luoh.imgbed.model.LocalDocument
import com.luoh.imgbed.model.TransferStatus
import com.luoh.imgbed.model.UploadChannel
import com.luoh.imgbed.ui.agora.SettingsItem

@Composable
internal fun UploadScreen(viewModel: AppViewModel, onSettings: (SettingsPage) -> Unit) {
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val uploading by viewModel.uploading.collectAsStateWithLifecycle()
    val compression by viewModel.compression.collectAsStateWithLifecycle()
    val upload by viewModel.upload.collectAsStateWithLifecycle()
    val token by viewModel.token.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { viewModel.addUris(it) }
    val pending = documents.count { it.status != TransferStatus.SUCCESS }
    val completed = documents.count { it.status == TransferStatus.SUCCESS }
    val failed = documents.count { it.status == TransferStatus.ERROR }
    val needsConnection = token.isBlank()
    val chooseFiles = { picker.launch(arrayOf("*/*")) }
    PageScaffold("上传", actions = {
        if (documents.isNotEmpty()) RoundAction(Icons.Default.Add, "添加文件", chooseFiles)
    }, bottomAction = {
        Button(onClick = {
            when {
                documents.isEmpty() || pending == 0 -> chooseFiles()
                needsConnection -> onSettings(SettingsPage.CONNECTION)
                else -> viewModel.uploadAll()
            }
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = !busy) {
            if (uploading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Icon(if (documents.isEmpty() || pending == 0) Icons.Default.Add else if (needsConnection) Icons.Default.Key else Icons.Default.CloudUpload, null)
            Spacer(Modifier.width(8.dp))
            Text(when {
                uploading -> "正在上传 · 已完成 $completed/${documents.size}"
                documents.isEmpty() -> "选择文件"
                pending == 0 -> "继续选择文件"
                needsConnection -> "先设置服务连接"
                pending == failed -> "重试 $failed 个文件"
                else -> "上传 $pending 个文件"
            })
        }
    }) {
        if (documents.isEmpty()) item {
            EmptyState(Icons.Default.AddPhotoAlternate, "选择要上传的图片或文件")
        }
        item {
            SettingsGroup("上传到", buildList<@Composable () -> Unit> {
                if (needsConnection) add {
                    SettingsRow(Icons.Default.Key, "服务连接", "未设置", onClick = { onSettings(SettingsPage.CONNECTION) })
                }
                add { EnumMenu(Icons.Default.CloudQueue, "上传渠道", upload.channel, UploadChannel.entries, { it.label }, enabled = !uploading) {
                    viewModel.saveUpload(upload.copy(channel = it))
                } }
                add { EditSetting(Icons.Default.Folder, "上传目录", upload.uploadFolder, emptyValue = "根目录", enabled = !uploading) {
                    viewModel.saveUpload(upload.copy(uploadFolder = it))
                } }
                add { SettingsRow(Icons.Default.AutoAwesome, "图片处理", if (compression.enabled) compression.format.label else "原文件",
                    enabled = !uploading, onClick = { onSettings(SettingsPage.COMPRESSION) }) }
            })
        }
        if (documents.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("文件 · ${documents.size}", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    if (completed > 0) TextButton(viewModel::clearCompleted, enabled = !uploading) { Text("清除已完成") }
                }
                if (completed > 1) TextButton({
                    clipboard.setText(AnnotatedString(documents.filter { it.status == TransferStatus.SUCCESS }.joinToString("\n") { it.resultUrl }))
                    viewModel.showMessage("已复制 $completed 个链接")
                }) { Text("复制全部链接（$completed）") }
            }
            itemsIndexed(documents, key = { _, file -> file.uri.toString() }) { index, file ->
                GroupItem(index, documents.size) {
                    LocalFileRow(file, { viewModel.remove(file.uri) }, !uploading, { viewModel.showMessage("链接已复制") })
                }
            }
        }
    }
}

@Composable
private fun LocalFileRow(file: LocalDocument, onRemove: () -> Unit, canRemove: Boolean, onCopied: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val status = when (file.status) {
        TransferStatus.WAITING -> "等待上传"
        TransferStatus.PROCESSING -> "处理中"
        TransferStatus.UPLOADING -> "${(file.progress * 100).toInt()}%"
        TransferStatus.SUCCESS -> "已完成"
        TransferStatus.ERROR -> "上传失败，可重试"
    }
    Column {
        SettingsItem(
            headlineContent = { Text(file.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = { Text("${formatBytes(file.sizeBytes)} · $status") },
            leadingContent = { Icon(if (file.isImage) Icons.Default.Image else Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = {
                when (file.status) {
                    TransferStatus.WAITING, TransferStatus.ERROR -> IconButton(onRemove, enabled = canRemove) { Icon(Icons.Default.Close, "移除 ${file.displayName}") }
                    TransferStatus.PROCESSING, TransferStatus.UPLOADING -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    TransferStatus.SUCCESS -> Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                }
            },
        )
        if (file.status == TransferStatus.UPLOADING) LinearProgressIndicator(progress = { file.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
        if (file.error.isNotBlank()) Text(file.error, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.error)
        if (file.resultUrl.isNotBlank()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                TextButton({ clipboard.setText(AnnotatedString(file.resultUrl)); onCopied() }) { Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("复制链接") }
                TextButton({ shareText(context, file.resultUrl) }) { Icon(Icons.Default.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("分享") }
            }
        }
    }
}
