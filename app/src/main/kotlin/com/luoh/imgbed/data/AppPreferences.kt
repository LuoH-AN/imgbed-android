package com.luoh.imgbed.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.luoh.imgbed.model.CompressionSettings
import com.luoh.imgbed.model.NamingMode
import com.luoh.imgbed.model.OutputFormat
import com.luoh.imgbed.model.UploadChannel
import com.luoh.imgbed.model.UploadSettings
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("imgbed_settings", Context.MODE_PRIVATE)
    private val secrets = SecretTokenStore(context)

    fun readCompression() = CompressionSettings(
        enabled = prefs.getBoolean("compression_enabled", true),
        format = enumValue(prefs.getString("format", null), OutputFormat.WEBP_LOSSY),
        quality = prefs.getInt("quality", 82).coerceIn(1, 100),
        maxLongEdge = prefs.getInt("max_edge", 2560).coerceIn(0, 16384),
        targetSizeKb = prefs.getInt("target_kb", 0).coerceAtLeast(0),
        skipIfLarger = prefs.getBoolean("skip_if_larger", true),
        avifSpeed = prefs.getInt("avif_speed", 8).coerceIn(0, 10),
    )

    fun saveCompression(value: CompressionSettings) = prefs.edit()
        .putBoolean("compression_enabled", value.enabled)
        .putString("format", value.format.name)
        .putInt("quality", value.quality)
        .putInt("max_edge", value.maxLongEdge)
        .putInt("target_kb", value.targetSizeKb)
        .putBoolean("skip_if_larger", value.skipIfLarger)
        .putInt("avif_speed", value.avifSpeed)
        .apply()

    fun readUpload() = UploadSettings(
        serverUrl = prefs.getString("server_url", "https://i.luoh.org").orEmpty(),
        channel = enumValue(prefs.getString("channel", null), UploadChannel.TELEGRAM),
        channelName = prefs.getString("channel_name", "").orEmpty(),
        uploadFolder = prefs.getString("upload_folder", "").orEmpty(),
        namingMode = enumValue(prefs.getString("naming_mode", null), NamingMode.ORIGINAL),
        namingTemplate = prefs.getString("naming_template", "{yyyy}{MM}{dd}_{HH}{mm}{ss}_{index}_{name}").orEmpty(),
        disableServerCompression = prefs.getBoolean("disable_server_compression", true),
    )

    fun saveUpload(value: UploadSettings) = prefs.edit()
        .putString("server_url", value.serverUrl.trim())
        .putString("channel", value.channel.name)
        .putString("channel_name", value.channelName.trim())
        .putString("upload_folder", value.uploadFolder.trim('/'))
        .putString("naming_mode", value.namingMode.name)
        .putString("naming_template", value.namingTemplate)
        .putBoolean("disable_server_compression", value.disableServerCompression)
        .apply()

    fun readToken(): String = secrets.read()
    fun saveToken(value: String) = secrets.save(value.trim())

    private inline fun <reified T : Enum<T>> enumValue(raw: String?, fallback: T): T =
        runCatching { enumValueOf<T>(raw.orEmpty()) }.getOrDefault(fallback)
}

private class SecretTokenStore(context: Context) {
    private val file = context.noBackupFilesDir.resolve("imgbed-api-token")
    private val alias = "imgbed-api-token"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
        }.generateKey()
    }

    fun save(value: String) {
        if (value.isBlank()) {
            file.delete()
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        file.writeText(Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray()), Base64.NO_WRAP))
    }

    fun read(): String = runCatching {
        if (!file.exists()) return ""
        val data = Base64.decode(file.readText(), Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, data.copyOfRange(0, 12)))
        }
        String(cipher.doFinal(data.copyOfRange(12, data.size)), Charsets.UTF_8)
    }.getOrDefault("")
}
