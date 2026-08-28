package com.wallpaperextend.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.exifinterface.media.ExifInterface as AndroidXExif

object ImageLoader {

    sealed class LoadResult {
        data class Success(val bitmap: Bitmap) : LoadResult()
        data class Error(val message: String, val exception: Exception? = null) : LoadResult()
    }

    fun loadFromUri(context: Context, uri: Uri, maxSide: Int = 1600): LoadResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return LoadResult.Error("无法打开文件流")
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, opts)
            inputStream.close()

            opts.inSampleSize = calculateInSampleSize(opts.outWidth, opts.outHeight, maxSide)
            opts.inJustDecodeBounds = false

            val stream2 = context.contentResolver.openInputStream(uri)
                ?: return LoadResult.Error("无法重新打开文件流")
            val bitmap = BitmapFactory.decodeStream(stream2, null, opts)
                ?: return LoadResult.Error("解码失败，返回空Bitmap")
            stream2.close()

            LoadResult.Success(rotateByExif(context, uri, bitmap))
        } catch (e: Exception) {
            LoadResult.Error("加载图片失败: ${e.message}", e)
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxSide: Int): Int {
        var sample = 1
        val larger = width.coerceAtLeast(height)
        while (larger / (sample * 2) >= maxSide) sample *= 2
        return sample
    }

    private fun rotateByExif(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: return bitmap
            val exif = AndroidXExif(stream)
            stream.close()
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }
}