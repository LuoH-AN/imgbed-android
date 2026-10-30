package com.luoh.imgbed.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luoh.imgbed.compression.ImageCompressor
import com.luoh.imgbed.data.AppPreferences
import com.luoh.imgbed.data.AppAppearance
import com.luoh.imgbed.data.AppearancePreferences
import com.luoh.imgbed.model.CompressionSettings
import com.luoh.imgbed.model.LocalDocument
import com.luoh.imgbed.model.RemoteFile
import com.luoh.imgbed.model.RemotePage
import com.luoh.imgbed.model.TransferStatus
import com.luoh.imgbed.model.UploadSettings
import com.luoh.imgbed.network.ImgBedClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val resolver = application.contentResolver
    private val preferences = AppPreferences(application)
    private val appearancePreferences = AppearancePreferences(application)
    private val _appearance = MutableStateFlow(appearancePreferences.read())
    val appearance = _appearance.asStateFlow()

    fun saveAppearance(value: AppAppearance) {
        _appearance.value = value
        appearancePreferences.save(value)
    }
    private val compressor = ImageCompressor(resolver)
    private val client = ImgBedClient(resolver)

    private val _documents = MutableStateFlow<List<LocalDocument>>(emptyList())
    val documents = _documents.asStateFlow()

    private val _compression = MutableStateFlow(preferences.readCompression())
    val compression = _compression.asStateFlow()

    private val _upload = MutableStateFlow(preferences.readUpload())
    val upload = _upload.asStateFlow()

    private val _token = MutableStateFlow(preferences.readToken())
    val token = _token.asStateFlow()

    private val _remote = MutableStateFlow(RemotePage(emptyList(), emptyList(), 0))
    val remote = _remote.asStateFlow()

    private val _remoteDirectory = MutableStateFlow("")
    val remoteDirectory = _remoteDirectory.asStateFlow()

    private val _remoteLoaded = MutableStateFlow(false)
    val remoteLoaded = _remoteLoaded.asStateFlow()
    private val _remoteLoading = MutableStateFlow(false)
    val remoteLoading = _remoteLoading.asStateFlow()
    private val _remoteError = MutableStateFlow("")
    val remoteError = _remoteError.asStateFlow()
    private val _remoteQuery = MutableStateFlow("")
    val remoteQuery = _remoteQuery.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _uploading = MutableStateFlow(false)
    val uploading = _uploading.asStateFlow()

    private val _message = MutableStateFlow("")
    val message = _message.asStateFlow()

    fun addUris(uris: List<Uri>) {
        val existing = _documents.value.map { it.uri }.toSet()
        val added = uris.distinct().filterNot(existing::contains).mapNotNull(::readDocument)
        _documents.update { it + added }
    }

    fun acceptShare(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            @Suppress("DEPRECATION")
            (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { addUris(listOf(it)) }
        } else if (intent?.action == Intent.ACTION_SEND_MULTIPLE) {
            @Suppress("DEPRECATION")
            addUris(intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty())
        }
    }

    fun remove(uri: Uri) = _documents.update { list -> list.filterNot { it.uri == uri } }
    fun clearCompleted() = _documents.update { list -> list.filterNot { it.status == TransferStatus.SUCCESS } }
    fun clearMessage() { _message.value = "" }
    fun showMessage(value: String) { _message.value = value }

    fun saveCompression(value: CompressionSettings) {
        _compression.value = value
        preferences.saveCompression(value)
    }

    fun saveUpload(value: UploadSettings) {
        _upload.value = value
        preferences.saveUpload(value)
    }

    fun saveToken(value: String) {
        _token.value = value
        preferences.saveToken(value)
    }

    fun uploadAll() {
        if (_busy.value) return
        if (_token.value.isBlank()) {
            _message.value = "请先设置服务连接"
            return
        }
        val waiting = _documents.value.filter { it.status != TransferStatus.SUCCESS }
        if (waiting.isEmpty()) return
        val settings = _upload.value
        val compression = _compression.value
        val token = _token.value
        viewModelScope.launch {
            _busy.value = true
            _uploading.value = true
            try {
                waiting.forEachIndexed { index, item -> uploadOne(item, index + 1, settings, compression, token) }
                val failed = _documents.value.count { it.status == TransferStatus.ERROR }
                _message.value = if (failed == 0) "上传完成" else "$failed 个文件上传失败，可重试"
            } finally {
                _uploading.value = false
                _busy.value = false
            }
        }
    }

    private suspend fun uploadOne(item: LocalDocument, index: Int, settings: UploadSettings, compression: CompressionSettings, token: String) {
        updateDocument(item.uri) { it.copy(status = TransferStatus.PROCESSING, progress = 0f, error = "") }
        runCatching {
            val prepared = compressor.prepare(
                document = item,
                settings = compression,
                namingMode = settings.namingMode,
                namingTemplate = settings.namingTemplate,
                index = index,
            )
            updateDocument(item.uri) { it.copy(status = TransferStatus.UPLOADING) }
            client.upload(prepared, settings, token) { progress ->
                updateDocument(item.uri) { current -> current.copy(progress = progress) }
            }
        }.onSuccess { result ->
            updateDocument(item.uri) {
                it.copy(
                    status = TransferStatus.SUCCESS,
                    progress = 1f,
                    resultUrl = result.publicUrl.ifBlank { result.src },
                )
            }
        }.onFailure { error ->
            updateDocument(item.uri) {
                it.copy(status = TransferStatus.ERROR, error = error.message ?: "上传失败")
            }
        }
    }

    fun refreshRemote(search: String = _remoteQuery.value, append: Boolean = false) {
        if (_busy.value) return
        _remoteQuery.value = search.trim()
        val query = _remoteQuery.value
        if (!append) {
            _remote.value = RemotePage(emptyList(), emptyList(), 0)
            _remoteLoaded.value = false
        }
        viewModelScope.launch {
            _busy.value = true
            _remoteLoading.value = true
            _remoteError.value = ""
            runCatching {
                client.list(_upload.value.serverUrl, _token.value, _remoteDirectory.value, query,
                    start = if (append) _remote.value.files.size else 0)
            }.onSuccess {
                _remote.value = if (append) it.copy(
                    files = (_remote.value.files + it.files).distinctBy { file -> file.id },
                    directories = (_remote.value.directories + it.directories).distinct(),
                ) else it
                _remoteLoaded.value = true
            }.onFailure {
                _remoteError.value = it.message ?: "读取文件列表失败"
            }
            _remoteLoading.value = false
            _busy.value = false
        }
    }

    fun loadMoreRemote() {
        if (_remote.value.files.size < _remote.value.totalCount) refreshRemote(append = true)
    }

    fun enterDirectory(value: String) {
        if (_busy.value) return
        _remoteDirectory.value = value.trim('/')
        _remote.value = RemotePage(emptyList(), emptyList(), 0)
        _remoteLoaded.value = false
        refreshRemote("")
    }

    fun goParentDirectory() {
        if (_busy.value) return
        _remoteDirectory.value = _remoteDirectory.value.substringBeforeLast('/', "")
        _remote.value = RemotePage(emptyList(), emptyList(), 0)
        _remoteLoaded.value = false
        refreshRemote("")
    }

    fun deleteRemote(file: RemoteFile) = remoteAction("已删除 ${file.fileName}") {
        client.delete(_upload.value.serverUrl, _token.value, file.id)
    }

    fun renameRemote(file: RemoteFile, newName: String) {
        val parent = file.id.substringBeforeLast('/', "")
        val newId = listOf(parent, newName.trim()).filter { it.isNotBlank() }.joinToString("/")
        remoteAction("已重命名为 $newName") {
            client.rename(_upload.value.serverUrl, _token.value, file.id, newId)
        }
    }

    fun moveRemote(file: RemoteFile, destination: String) = remoteAction("文件已移动") {
        client.move(_upload.value.serverUrl, _token.value, file.id, destination.trim('/'))
    }

    fun remoteUrl(file: RemoteFile) = client.publicFileUrl(_upload.value.serverUrl, file.id)

    private fun remoteAction(successMessage: String, action: suspend () -> Unit) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            runCatching { action() }
                .onSuccess {
                    _message.value = successMessage
                    runCatching {
                        client.list(_upload.value.serverUrl, _token.value, _remoteDirectory.value, _remoteQuery.value)
                    }.onSuccess { _remote.value = it; _remoteLoaded.value = true; _remoteError.value = "" }
                        .onFailure { _remoteError.value = it.message ?: "刷新失败，请重试" }
                }
                .onFailure { _message.value = it.message ?: "操作失败" }
            _busy.value = false
        }
    }

    private fun updateDocument(uri: Uri, transform: (LocalDocument) -> LocalDocument) {
        _documents.update { list -> list.map { if (it.uri == uri) transform(it) else it } }
    }

    private fun readDocument(uri: Uri): LocalDocument? = runCatching {
        runCatching {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        var name = uri.lastPathSegment ?: "file"
        var size = -1L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                name = cursor.getString(0) ?: name
                if (!cursor.isNull(1)) size = cursor.getLong(1)
            }
        }
        val extension = name.substringAfterLast('.', "").lowercase()
        val inferred = when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "avif" -> "image/avif"
            "heic", "heif" -> "image/heic"
            "gif" -> "image/gif"
            "svg" -> "image/svg+xml"
            else -> "application/octet-stream"
        }
        val providerMime = resolver.getType(uri).orEmpty()
        val mime = if (providerMime.isBlank() || providerMime == "application/octet-stream") inferred else providerMime
        LocalDocument(uri, name, mime, size, mime.startsWith("image/"))
    }.onFailure { _message.value = it.message ?: "无法读取文件" }.getOrNull()
}
