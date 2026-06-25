package com.masar.maintenance.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

data class UploadFile(val file: File, val fileName: String, val mime: String)

/** تجهيز الملفات للرفع: تُضغط الصور، وتُنسخ ملفات PDF كما هي. */
object Uploads {

    fun prepare(context: Context, uri: Uri): UploadFile? {
        val mime = context.contentResolver.getType(uri) ?: ""
        return if (mime.startsWith("image/")) compressImage(context, uri)
        else copyRaw(context, uri, mime.ifBlank { "application/octet-stream" })
    }

    fun compressImage(context: Context, uri: Uri, maxDim: Int = 1400, quality: Int = 80): UploadFile? {
        return try {
            val cr = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            val largest = maxOf(bounds.outWidth, bounds.outHeight)
            while (largest > 0 && largest / sample > maxDim * 2) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: return null
            val scaled = scaleTo(bmp, maxDim)
            val out = File(context.cacheDir, "up_${System.currentTimeMillis()}.jpg")
            FileOutputStream(out).use { scaled.compress(Bitmap.CompressFormat.JPEG, quality, it) }
            if (scaled !== bmp) bmp.recycle()
            UploadFile(out, out.name, "image/jpeg")
        } catch (e: Exception) { null }
    }

    private fun copyRaw(context: Context, uri: Uri, mime: String): UploadFile? {
        return try {
            val ext = if (mime.contains("pdf")) "pdf" else "bin"
            val out = File(context.cacheDir, "up_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { inp ->
                FileOutputStream(out).use { inp.copyTo(it) }
            } ?: return null
            UploadFile(out, out.name, mime)
        } catch (e: Exception) { null }
    }

    private fun scaleTo(b: Bitmap, maxDim: Int): Bitmap {
        val largest = maxOf(b.width, b.height)
        if (largest <= maxDim) return b
        val r = maxDim.toFloat() / largest
        return Bitmap.createScaledBitmap(b, (b.width * r).toInt(), (b.height * r).toInt(), true)
    }
}
