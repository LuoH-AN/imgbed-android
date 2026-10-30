package com.luoh.imgbed.network

import android.content.ContentResolver
import com.luoh.imgbed.model.NamingMode
import com.luoh.imgbed.model.PreparedUpload
import com.luoh.imgbed.model.RemoteFile
import com.luoh.imgbed.model.RemotePage
import com.luoh.imgbed.model.UploadResult
import com.luoh.imgbed.model.UploadSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.BufferedSink
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ImgBedClient(
    private val resolver: ContentResolver,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun upload(
        prepared: PreparedUpload,
        settings: UploadSettings,
        token: String,
        onProgress: (Float) -> Unit,
    ): UploadResult = withContext(Dispatchers.IO) {
        require(token.isNotBlank()) { "请先在设置中填写 API Token" }
        val base = normalizedBase(settings.serverUrl)
        val url = base.newBuilder()
            .addPathSegment("upload")
            .addQueryParameter("uploadChannel", settings.channel.apiValue)
            .addQueryParameter("returnFormat", "full")
            .addQueryParameter("uploadNameType", if (settings.namingMode == NamingMode.SERVER) "default" else "origin")
            .apply {
                settings.channelName.takeIf { it.isNotBlank() }?.let { addQueryParameter("channelName", it) }
                settings.uploadFolder.takeIf { it.isNotBlank() }?.let { addQueryParameter("uploadFolder", it) }
                if (settings.disableServerCompression) addQueryParameter("serverCompress", "false")
            }
            .build()

        val content = prepared.bytes?.let {
            ProgressRequestBody(it, prepared.mimeType, onProgress)
        } ?: UriRequestBody(resolver, requireNotNull(prepared.sourceUri), prepared.contentBytes, prepared.mimeType, onProgress)
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", prepared.fileName, content)
            .build()
        val request = authorized(Request.Builder().url(url).post(body), token).build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            ensureSuccess(response, raw)
            val item = JSONArray(raw).optJSONObject(0) ?: error("上传响应格式无效")
            UploadResult(item.optString("src"), item.optString("publicUrl"))
        }
    }

    suspend fun list(
        serverUrl: String,
        token: String,
        directory: String = "",
        search: String = "",
        start: Int = 0,
        count: Int = 100,
    ): RemotePage = withContext(Dispatchers.IO) {
        val url = normalizedBase(serverUrl).newBuilder()
            .addPathSegments("api/manage/list")
            .addQueryParameter("start", start.toString())
            .addQueryParameter("count", count.toString())
            .apply {
                if (directory.isNotBlank()) addQueryParameter("dir", directory)
                if (search.isNotBlank()) addQueryParameter("search", search)
            }.build()
        val request = authorized(Request.Builder().url(url).get(), token).build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            ensureSuccess(response, raw)
            val root = JSONObject(raw)
            val files = root.optJSONArray("files") ?: JSONArray()
            val directories = root.optJSONArray("directories") ?: JSONArray()
            RemotePage(
                files = buildList {
                    for (i in 0 until files.length()) {
                        val item = files.optJSONObject(i) ?: continue
                        val metadata = item.optJSONObject("metadata") ?: JSONObject()
                        val id = item.optString("name")
                        add(
                            RemoteFile(
                                id = id,
                                fileName = metadata.optString("FileName", id.substringAfterLast('/')),
                                mimeType = metadata.optString("FileType", "application/octet-stream"),
                                sizeLabel = metadata.optString("FileSize", "0") + " MB",
                                channel = metadata.optString("Channel", ""),
                                directory = metadata.optString("Directory", ""),
                                timestamp = metadata.optLong("TimeStamp", 0L),
                            )
                        )
                    }
                },
                directories = buildList {
                    for (i in 0 until directories.length()) add(directories.optString(i))
                },
                totalCount = root.optInt("totalCount", files.length()),
            )
        }
    }

    suspend fun delete(serverUrl: String, token: String, fileId: String) = withContext(Dispatchers.IO) {
        val url = managementUrl(serverUrl, "delete", fileId)
        executeUnit(authorized(Request.Builder().url(url).delete(), token).build())
    }

    suspend fun rename(serverUrl: String, token: String, fileId: String, newFileId: String) = withContext(Dispatchers.IO) {
        val body = JSONObject().put("newFileId", newFileId).toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val url = managementUrl(serverUrl, "rename", fileId)
        executeUnit(authorized(Request.Builder().url(url).post(body), token).build())
    }

    suspend fun move(serverUrl: String, token: String, fileId: String, destination: String) = withContext(Dispatchers.IO) {
        val url = managementUrl(serverUrl, "move", fileId).newBuilder()
            .addQueryParameter("dist", destination)
            .build()
        val empty = ByteArray(0).toRequestBody(null)
        executeUnit(authorized(Request.Builder().url(url).post(empty), token).build())
    }

    fun publicFileUrl(serverUrl: String, fileId: String): String = normalizedBase(serverUrl).newBuilder()
        .addPathSegment("file")
        .apply { fileId.split('/').filter { it.isNotEmpty() }.forEach(::addPathSegment) }
        .build().toString()

    private fun managementUrl(serverUrl: String, action: String, fileId: String): HttpUrl =
        normalizedBase(serverUrl).newBuilder()
            .addPathSegments("api/manage/$action")
            .apply { fileId.split('/').filter { it.isNotEmpty() }.forEach(::addPathSegment) }
            .build()

    private fun executeUnit(request: Request) {
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            ensureSuccess(response, raw)
        }
    }

    private fun authorized(builder: Request.Builder, token: String): Request.Builder {
        require(token.isNotBlank()) { "API Token 为空" }
        return builder.header("Authorization", "Bearer $token").header("Accept", "application/json")
    }

    private fun ensureSuccess(response: Response, raw: String) {
        if (response.isSuccessful) return
        val message = runCatching {
            val json = JSONObject(raw)
            json.optString("message").ifBlank { json.optString("error") }
        }.getOrDefault("").ifBlank { raw.take(300) }.ifBlank { "HTTP ${response.code}" }
        error(message)
    }

    private fun normalizedBase(value: String): HttpUrl {
        val text = value.trim().trimEnd('/')
        require(text.startsWith("https://") || text.startsWith("http://")) { "服务器地址必须包含 http:// 或 https://" }
        val url = text.toHttpUrl()
        require(url.username.isEmpty() && url.password.isEmpty()) { "服务器地址不能包含账号信息" }
        return url
    }
}

private class ProgressRequestBody(
    private val bytes: ByteArray,
    private val mime: String,
    private val progress: (Float) -> Unit,
) : RequestBody() {
    override fun contentType() = mime.toMediaTypeOrNull()
    override fun contentLength() = bytes.size.toLong()
    override fun writeTo(sink: BufferedSink) {
        val chunk = 64 * 1024
        var offset = 0
        while (offset < bytes.size) {
            val count = minOf(chunk, bytes.size - offset)
            sink.write(bytes, offset, count)
            offset += count
            progress(offset.toFloat() / bytes.size.coerceAtLeast(1))
        }
    }
}

private class UriRequestBody(
    private val resolver: ContentResolver,
    private val uri: android.net.Uri,
    private val size: Long,
    private val mime: String,
    private val progress: (Float) -> Unit,
) : RequestBody() {
    override fun contentType() = mime.toMediaTypeOrNull()
    override fun contentLength() = if (size > 0) size else -1L
    override fun writeTo(sink: BufferedSink) {
        resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法读取所选文件" }
            val buffer = ByteArray(64 * 1024)
            var sent = 0L
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                sink.write(buffer, 0, count)
                sent += count
                if (size > 0) progress((sent.toFloat() / size).coerceIn(0f, 1f))
            }
        }
    }
}
