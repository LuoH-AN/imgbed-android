package com.luoh.imgbed.compression

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.luoh.imgbed.model.CompressionSettings
import com.luoh.imgbed.model.LocalDocument
import com.luoh.imgbed.model.NamingMode
import com.luoh.imgbed.model.OutputFormat
import com.luoh.imgbed.model.PreparedUpload
import com.radzivon.bartoshyk.avif.coder.AvSpeed
import com.radzivon.bartoshyk.avif.coder.AvifEncodingOptions
import com.radzivon.bartoshyk.avif.coder.Coder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

class ImageCompressor(private val resolver: ContentResolver) {
    suspend fun prepare(
        document: LocalDocument,
        settings: CompressionSettings,
        namingMode: NamingMode,
        namingTemplate: String,
        index: Int,
    ): PreparedUpload = withContext(Dispatchers.IO) {
        if (!document.isImage || !settings.enabled) {
            return@withContext original(document, namingMode, namingTemplate, index)
        }
        if (document.mimeType.lowercase(Locale.ROOT) in setOf("image/gif", "image/svg+xml")) {
            return@withContext original(document, namingMode, namingTemplate, index)
        }
        if (document.mimeType.equals("image/webp", ignoreCase = true) && isAnimatedWebp(document.uri)) {
            return@withContext original(document, namingMode, namingTemplate, index)
        }

        val requestedFormat = resolveFormat(settings.format, document.mimeType, document.displayName)
        var bitmap = decode(document.uri, settings.maxLongEdge)
        if (requestedFormat == OutputFormat.JPEG && bitmap.hasAlpha()) {
            bitmap = flattenAlpha(bitmap)
        }

        var quality = settings.quality.coerceIn(1, 100)
        var encoded = encode(bitmap, requestedFormat, quality, settings.avifSpeed)
        val targetBytes = settings.targetSizeKb.toLong() * 1024L
        var attempts = 0
        while (targetBytes > 0 && encoded.size > targetBytes && attempts < 12) {
            attempts++
            if (requestedFormat.lossy && quality > 35) {
                quality = (quality - 7).coerceAtLeast(35)
            } else if (bitmap.width > 640 || bitmap.height > 640) {
                val scaled = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * 0.88f).roundToInt().coerceAtLeast(1),
                    (bitmap.height * 0.88f).roundToInt().coerceAtLeast(1),
                    true,
                )
                if (scaled !== bitmap) bitmap.recycle()
                bitmap = scaled
            } else {
                break
            }
            encoded = encode(bitmap, requestedFormat, quality, settings.avifSpeed)
        }

        if (settings.skipIfLarger && document.sizeBytes > 0 && encoded.size >= document.sizeBytes) {
            bitmap.recycle()
            return@withContext original(document, namingMode, namingTemplate, index)
        }

        val fileName = NamingEngine.build(
            originalName = document.displayName,
            outputExtension = requestedFormat.extension,
            mode = namingMode,
            template = namingTemplate,
            index = index,
        )
        PreparedUpload(
            bytes = encoded,
            fileName = fileName,
            mimeType = requestedFormat.mimeType,
            originalBytes = document.sizeBytes,
            contentBytes = encoded.size.toLong(),
            width = bitmap.width,
            height = bitmap.height,
        ).also { bitmap.recycle() }
    }

    private fun original(
        document: LocalDocument,
        namingMode: NamingMode,
        template: String,
        index: Int,
    ): PreparedUpload {
        val extension = document.displayName.substringAfterLast('.', "")
        return PreparedUpload(
            sourceUri = document.uri,
            fileName = NamingEngine.build(document.displayName, extension, namingMode, template, index),
            mimeType = document.mimeType.ifBlank { "application/octet-stream" },
            originalBytes = document.sizeBytes,
        )
    }

    private fun resolveFormat(requested: OutputFormat, mime: String, name: String): OutputFormat {
        if (requested != OutputFormat.KEEP) return requested
        return when (mime.lowercase(Locale.ROOT)) {
            "image/jpeg" -> OutputFormat.JPEG
            "image/png" -> OutputFormat.PNG
            "image/webp" -> OutputFormat.WEBP_LOSSY
            "image/avif" -> OutputFormat.AVIF
            else -> when (name.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
                "jpg", "jpeg" -> OutputFormat.JPEG
                "png" -> OutputFormat.PNG
                "webp" -> OutputFormat.WEBP_LOSSY
                "avif" -> OutputFormat.AVIF
                else -> OutputFormat.WEBP_LOSSY
            }
        }
    }

    private fun decode(uri: Uri, maxLongEdge: Int): Bitmap {
        if (resolver.getType(uri)?.lowercase(Locale.ROOT) == "image/avif") {
            val bytes = resolver.openInputStream(uri).use { requireNotNull(it).readBytes() }
            return scaleToLimit(Coder().decode(bytes), maxLongEdge)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
                val longest = max(info.size.width, info.size.height)
                if (maxLongEdge > 0 && longest > maxLongEdge) {
                    val scale = maxLongEdge.toFloat() / longest
                    decoder.setTargetSize(
                        (info.size.width * scale).roundToInt().coerceAtLeast(1),
                        (info.size.height * scale).roundToInt().coerceAtLeast(1),
                    )
                }
            }
        } else {
            decodeLegacy(uri, maxLongEdge)
        }
    }

    private fun isAnimatedWebp(uri: Uri): Boolean = resolver.openInputStream(uri).use { input ->
        if (input == null) return@use false
        val header = ByteArray(128 * 1024)
        val length = input.read(header)
        if (length <= 0) return@use false
        val text = String(header, 0, length, Charsets.ISO_8859_1)
        text.contains("ANIM") || text.contains("ANMF")
    }

    private fun scaleToLimit(source: Bitmap, maxLongEdge: Int): Bitmap {
        val longest = max(source.width, source.height)
        if (maxLongEdge <= 0 || longest <= maxLongEdge) return source
        val scale = maxLongEdge.toFloat() / longest
        return Bitmap.createScaledBitmap(
            source,
            (source.width * scale).roundToInt().coerceAtLeast(1),
            (source.height * scale).roundToInt().coerceAtLeast(1),
            true,
        ).also { if (it !== source) source.recycle() }
    }

    private fun decodeLegacy(uri: Uri, maxLongEdge: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        if (maxLongEdge > 0) {
            while (max(bounds.outWidth, bounds.outHeight) / sample > maxLongEdge * 2) sample *= 2
        }
        val decoded = resolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            })
        } ?: error("无法解码图片")
        val orientation = resolver.openInputStream(uri).use { stream ->
            stream?.let { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
                ?: ExifInterface.ORIENTATION_NORMAL
        }
        var bitmap = rotateForExif(decoded, orientation)
        val longest = max(bitmap.width, bitmap.height)
        if (maxLongEdge > 0 && longest > maxLongEdge) {
            val scale = maxLongEdge.toFloat() / longest
            val scaled = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).roundToInt().coerceAtLeast(1),
                (bitmap.height * scale).roundToInt().coerceAtLeast(1),
                true,
            )
            if (scaled !== bitmap) bitmap.recycle()
            bitmap = scaled
        }
        return bitmap
    }

    private fun rotateForExif(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return source
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            .also { if (it !== source) source.recycle() }
    }

    private fun flattenAlpha(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(result).apply {
            drawColor(Color.WHITE)
            drawBitmap(source, 0f, 0f, null)
        }
        source.recycle()
        return result
    }

    private fun encode(bitmap: Bitmap, format: OutputFormat, quality: Int, avifSpeed: Int): ByteArray {
        if (format == OutputFormat.AVIF) {
            val speed = when {
                avifSpeed <= 2 -> AvSpeed.EXTRA_SLOW
                avifSpeed <= 5 -> AvSpeed.SLOW
                avifSpeed <= 7 -> AvSpeed.MEDIUM
                else -> AvSpeed.FAST
            }
            return Coder().encodeAvif(bitmap, options = AvifEncodingOptions(quality = quality, speed = speed))
        }
        val stream = ByteArrayOutputStream()
        val compressFormat = when (format) {
            OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
            OutputFormat.PNG -> Bitmap.CompressFormat.PNG
            OutputFormat.WEBP_LOSSLESS -> if (Build.VERSION.SDK_INT >= 30) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
            }
            OutputFormat.WEBP_LOSSY, OutputFormat.KEEP -> if (Build.VERSION.SDK_INT >= 30) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
            }
            OutputFormat.AVIF -> error("AVIF is handled above")
        }
        val effectiveQuality = if (format == OutputFormat.PNG || format == OutputFormat.WEBP_LOSSLESS) 100 else quality
        check(bitmap.compress(compressFormat, effectiveQuality, stream)) { "图片编码失败" }
        return stream.toByteArray()
    }
}
