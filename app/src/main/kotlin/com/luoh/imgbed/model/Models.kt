package com.luoh.imgbed.model

import android.net.Uri

enum class OutputFormat(val label: String, val extension: String, val mimeType: String, val lossy: Boolean) {
    KEEP("保持原格式", "", "", false),
    JPEG("JPEG", "jpg", "image/jpeg", true),
    PNG("PNG", "png", "image/png", false),
    WEBP_LOSSY("WebP 有损", "webp", "image/webp", true),
    WEBP_LOSSLESS("WebP 无损", "webp", "image/webp", false),
    AVIF("AVIF", "avif", "image/avif", true),
}

enum class NamingMode(val label: String) {
    ORIGINAL("原文件名"),
    TIMESTAMP("时间戳"),
    UUID("随机 UUID"),
    TEMPLATE("模板"),
    SERVER("服务端随机命名"),
}

enum class UploadChannel(val apiValue: String, val label: String) {
    TELEGRAM("telegram", "Telegram"),
    R2("cfr2", "Cloudflare R2"),
    S3("s3", "S3"),
    DISCORD("discord", "Discord"),
    HUGGING_FACE("huggingface", "Hugging Face"),
    WEBDAV("webdav", "WebDAV"),
}

data class CompressionSettings(
    val enabled: Boolean = true,
    val format: OutputFormat = OutputFormat.WEBP_LOSSY,
    val quality: Int = 82,
    val maxLongEdge: Int = 2560,
    val targetSizeKb: Int = 0,
    val skipIfLarger: Boolean = true,
    val avifSpeed: Int = 8,
)

data class UploadSettings(
    val serverUrl: String = "https://i.luoh.org",
    val channel: UploadChannel = UploadChannel.TELEGRAM,
    val channelName: String = "",
    val uploadFolder: String = "",
    val namingMode: NamingMode = NamingMode.ORIGINAL,
    val namingTemplate: String = "{yyyy}{MM}{dd}_{HH}{mm}{ss}_{index}_{name}",
    val disableServerCompression: Boolean = true,
)

data class LocalDocument(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val isImage: Boolean,
    val status: TransferStatus = TransferStatus.WAITING,
    val progress: Float = 0f,
    val resultUrl: String = "",
    val error: String = "",
)

enum class TransferStatus { WAITING, PROCESSING, UPLOADING, SUCCESS, ERROR }

data class PreparedUpload(
    val bytes: ByteArray? = null,
    val sourceUri: Uri? = null,
    val fileName: String,
    val mimeType: String,
    val originalBytes: Long,
    val contentBytes: Long = bytes?.size?.toLong() ?: originalBytes,
    val width: Int? = null,
    val height: Int? = null,
)

data class UploadResult(
    val src: String,
    val publicUrl: String = "",
)

data class RemoteFile(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val sizeLabel: String,
    val channel: String,
    val directory: String,
    val timestamp: Long,
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
}

data class RemotePage(
    val files: List<RemoteFile>,
    val directories: List<String>,
    val totalCount: Int,
)
