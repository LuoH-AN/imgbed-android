package com.luoh.imgbed.compression

import com.luoh.imgbed.model.NamingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object NamingEngine {
    fun build(
        originalName: String,
        outputExtension: String,
        mode: NamingMode,
        template: String,
        index: Int,
        now: Date = Date(),
        uuid: String = UUID.randomUUID().toString(),
    ): String {
        val safeOriginal = sanitize(originalName.substringAfterLast('/').substringAfterLast('\\'))
        val originalBase = safeOriginal.substringBeforeLast('.', safeOriginal)
        val originalExt = safeOriginal.substringAfterLast('.', "")
        val extension = outputExtension.ifBlank { originalExt }.lowercase(Locale.ROOT)
        val generated = when (mode) {
            NamingMode.ORIGINAL -> originalBase
            NamingMode.TIMESTAMP -> SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.ROOT).format(now)
            NamingMode.UUID -> uuid
            NamingMode.SERVER -> originalBase
            NamingMode.TEMPLATE -> applyTemplate(template, originalBase, extension, index, now, uuid)
        }.let(::sanitize).ifBlank { "file" }
        val base = if (extension.isNotBlank() && generated.lowercase(Locale.ROOT).endsWith(".${extension.lowercase(Locale.ROOT)}")) {
            generated.dropLast(extension.length + 1)
        } else {
            generated
        }
        return if (extension.isBlank()) base else "$base.$extension"
    }

    private fun applyTemplate(
        template: String,
        name: String,
        extension: String,
        index: Int,
        now: Date,
        uuid: String,
    ): String {
        val fields = mapOf(
            "{name}" to name,
            "{ext}" to extension,
            "{index}" to index.toString().padStart(3, '0'),
            "{uuid}" to uuid,
            "{yyyy}" to date(now, "yyyy"),
            "{MM}" to date(now, "MM"),
            "{dd}" to date(now, "dd"),
            "{HH}" to date(now, "HH"),
            "{mm}" to date(now, "mm"),
            "{ss}" to date(now, "ss"),
        )
        return fields.entries.fold(template.ifBlank { "{name}" }) { value, field ->
            value.replace(field.key, field.value)
        }
    }

    private fun date(value: Date, pattern: String) = SimpleDateFormat(pattern, Locale.ROOT).format(value)

    fun sanitize(value: String): String = value
        .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
        .replace(Regex("\\s+"), " ")
        .trim(' ', '.')
        .take(180)
}
