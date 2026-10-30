package com.luoh.imgbed.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luoh.imgbed.model.LocalDocument
import com.luoh.imgbed.model.TransferStatus

@Composable
internal fun UploadScreen(viewModel: AppViewModel, onCompression: () -> Unit) {
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val compression by viewModel.compression.collectAsStateWithLifecycle()
    val upload by viewModel.upload.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { viewModel.addUris(it) }
    val completed = documents.count { it.status == TransferStatus.SUCCESS }
    val pending = documents.size - completed
    PageScaffold("轻放云端", "每一份珍藏，都有自己的去处。", eyebrow = "IMGBED  /  工作空间", actions = {
        RoundAction(Icons.Rounded.Add, "添加文件", { picker.launch(arrayOf("*/*")) })
    }) {
        item {
            UploadHero { picker.launch(arrayOf("*/*")) }
        }
        item {
            Surface(onClick = onCompression, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                SettingsRow(Icons.Rounded.AutoAwesome, "${if (compression.enabled) compression.format.label else "原文件"} · 本地处理",
                    if (compression.enabled) "质量 ${compression.quality} · ${if (compression.maxLongEdge == 0) "原始尺寸" else "最长边 ${compression.maxLongEdge}px"}"
                    else "保留文件原本的画质与格式",
                    trailing = { Icon(Icons.Rounded.Tune, "调整压缩", tint = MaterialTheme.colorScheme.primary) })
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QueueStat("待上传", pending.toString().padStart(2, '0'), Modifier.weight(1f))
                QueueStat("已完成", completed.toString().padStart(2, '0'), Modifier.weight(1f))
                QueueStat("上传渠道", upload.channel.label, Modifier.weight(1.3f), compact = true)
            }
        }
        item {
            SectionHeading("传输队列") {
                if (completed > 0) TextButton(onClick = viewModel::clearCompleted) { Text("清理完成") }
                else StatusPill("${documents.size} 个文件")
            }
        }
        if (documents.isEmpty()) {
            item {
                EmptyState(Icons.Rounded.Inbox, "准备好，就从这里开始", "支持图片、视频与文档\n也可以从其他应用直接分享到 ImgBed")
            }
        } else {
            item {
                Button(viewModel::uploadAll, Modifier.fillMaxWidth().heightIn(min = 56.dp), enabled = !busy && pending > 0,
                    shape = RoundedCornerShape(20.dp)) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(if (pending == 0) Icons.Rounded.DoneAll else Icons.Rounded.CloudUpload, null)
                    Spacer(Modifier.width(10.dp))
                    Text(if (busy) "正在处理文件…" else if (pending == 0) "全部上传完成" else "开始上传 · $pending 个文件")
                }
            }
            items(documents, key = { it.uri.toString() }) { file ->
                LocalFileCard(file, onRemove = { viewModel.remove(file.uri) }, canRemove = !busy)
            }
        }
    }
}

@Composable
private fun UploadHero(onPick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(32.dp), color = colors.primaryContainer) {
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(colors.primaryContainer, colors.secondaryContainer)))
            .padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                StatusPill("本地处理 · 轻松分享", colors.onPrimaryContainer)
                Icon(Icons.Rounded.NorthEast, null, tint = colors.onPrimaryContainer)
            }
            Box(Modifier.fillMaxWidth().height(146.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(180.dp)) {
                    drawCircle(colors.primary.copy(alpha = .10f), radius = size.minDimension * .47f, style = Stroke(1.dp.toPx()))
                    drawCircle(colors.primary.copy(alpha = .10f), radius = size.minDimension * .35f, style = Stroke(1.dp.toPx()))
                    drawCircle(colors.primary.copy(alpha = .22f), radius = 4.dp.toPx(), center = Offset(size.width * .1f, size.height * .3f))
                    drawCircle(colors.primary.copy(alpha = .20f), radius = 3.dp.toPx(), center = Offset(size.width * .88f, size.height * .75f))
                }
                Surface(Modifier.offset(x = 22.dp, y = (-4).dp).rotate(13f).size(83.dp, 95.dp),
                    shape = RoundedCornerShape(20.dp), color = colors.primary.copy(alpha = .22f)) {}
                Surface(Modifier.offset(x = (-10).dp, y = 3.dp).rotate(-10f).size(90.dp, 103.dp),
                    shape = RoundedCornerShape(20.dp), color = colors.surfaceContainerLowest, shadowElevation = 4.dp) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Landscape, null, Modifier.size(52.dp), tint = colors.primary)
                    }
                }
                Surface(Modifier.offset(x = 45.dp, y = 39.dp).size(44.dp), shape = CircleShape, color = colors.primary) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Add, null, tint = colors.onPrimary) }
                }
            }
            Text("让分享，\n再轻一点。", style = MaterialTheme.typography.headlineMedium, color = colors.onPrimaryContainer)
            Spacer(Modifier.height(8.dp))
            Text("选好文件，剩下的交给 ImgBed。", style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryContainer.copy(alpha = .75f))
            Spacer(Modifier.height(20.dp))
            Button(onPick, Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Rounded.AddPhotoAlternate, null, Modifier.size(21.dp))
                Spacer(Modifier.width(10.dp))
                Text("选择文件", Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun QueueStat(label: String, value: String, modifier: Modifier, compact: Boolean = false) {
    Surface(modifier, shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LocalFileCard(file: LocalDocument, onRemove: () -> Unit, canRemove: Boolean) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val status = when (file.status) {
        TransferStatus.WAITING -> "等待上传"
        TransferStatus.PROCESSING -> "本地处理中"
        TransferStatus.UPLOADING -> "上传 ${(file.progress * 100).toInt()}%"
        TransferStatus.SUCCESS -> "上传完成"
        TransferStatus.ERROR -> "上传失败"
    }
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Icon(if (file.isImage) Icons.Rounded.Image else Icons.Rounded.Description, null,
                        Modifier.padding(15.dp).size(24.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(file.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatBytes(file.sizeBytes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (file.status == TransferStatus.WAITING || file.status == TransferStatus.ERROR) {
                    IconButton(onRemove, enabled = canRemove) { Icon(Icons.Rounded.Close, "移除 ${file.displayName}") }
                } else if (file.status == TransferStatus.SUCCESS) {
                    Icon(Icons.Rounded.CheckCircle, "已完成", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(status, if (file.status == TransferStatus.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                if (file.status == TransferStatus.PROCESSING) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            }
            if (file.status == TransferStatus.UPLOADING) LinearProgressIndicator(progress = { file.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            if (file.error.isNotBlank()) Text(file.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            if (file.resultUrl.isNotBlank()) {
                Text(file.resultUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton({ clipboard.setText(AnnotatedString(file.resultUrl)) }, Modifier.weight(1f)) {
                        Icon(Icons.Rounded.ContentCopy, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("复制链接")
                    }
                    TextButton({ shareText(context, file.resultUrl) }) { Icon(Icons.Rounded.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("分享") }
                }
            }
        }
    }
}
