package com.example.moodwave.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.File
import kotlin.math.max

internal object ProfilePhoto {
    private const val SIZE = 384

    fun thumbnail(context: Context, uri: Uri): Bitmap {
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= 28) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                val scale = minOf(1.0, SIZE.toDouble() / max(info.size.width, info.size.height))
                decoder.setTargetSize(max(1, (info.size.width * scale).toInt()), max(1, (info.size.height * scale).toInt()))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        require(options.outWidth > 0 && options.outHeight > 0)
        options.inJustDecodeBounds = false
        while (max(options.outWidth, options.outHeight) / options.inSampleSize.coerceAtLeast(1) > SIZE * 2) {
            options.inSampleSize = options.inSampleSize.coerceAtLeast(1) * 2
        }
        return requireNotNull(resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) })
    }

    fun save(context: Context, uri: Uri): String {
        val bitmap = thumbnail(context, uri)
        val dir = File(context.filesDir, "avatars").apply { mkdirs() }
        val file = File.createTempFile("profile-", ".png", dir)
        try {
            file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            return Uri.fromFile(file).toString()
        } catch (e: Exception) {
            file.delete()
            throw e
        } finally { bitmap.recycle() }
    }

    fun discard(context: Context, value: String) {
        val uri = Uri.parse(value)
        if (uri.scheme != "file") return
        val file = File(uri.path ?: return)
        if (file.parentFile == File(context.filesDir, "avatars")) file.delete()
    }
}
