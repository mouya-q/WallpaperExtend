package com.wallpaperextend.processor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object WallpaperProcessor {


    data class Config(
        val blurRadius: Int = 30,
        val extendRatio: Float = 0.25f,
        val featherWidth: Int = 40,
        val topOnly: Boolean = true
    )

    data class Result(val bitmap: Bitmap, val width: Int, val height: Int)


    suspend fun processAsync(
        src: Bitmap,
        targetW: Int,
        targetH: Int,
        config: Config = Config(),
        onProgress: ((Float) -> Unit)? = null
    ): Result = withContext(Dispatchers.Default) {
        onProgress?.invoke(0.1f)
        val safe = ensureOpaque(src)
        onProgress?.invoke(0.2f)

        val scaled = scaleToWidth(safe, targetW)
        val srcW = scaled.width
        val srcH = scaled.height
        onProgress?.invoke(0.3f)

        val extendH = if (config.topOnly) {
            (targetH * config.extendRatio.coerceIn(0f, 0.6f)).roundToInt().coerceAtLeast(0)
        } else {
            0
        }

        val outH = extendH + srcH
        val out = Bitmap.createBitmap(targetW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.TRANSPARENT)
        onProgress?.invoke(0.4f)

        if (extendH > 0) {
            drawTopExtension(canvas, scaled, targetW, extendH, config)
            onProgress?.invoke(0.6f)
            drawFeather(canvas, targetW, extendH, config.featherWidth.coerceIn(8, 80))
        }

        val drawX = ((targetW - srcW) / 2f).coerceAtLeast(0f)
        canvas.drawBitmap(scaled, drawX, extendH.toFloat(), null)
        onProgress?.invoke(0.8f)

        onProgress?.invoke(1.0f)
        Result(out, targetW, outH)
    }

    private fun drawTopExtension(canvas: Canvas, src: Bitmap, w: Int, extendH: Int, config: Config) {
        val stripH = max(8, src.height / 40)
        val topStrip = Bitmap.createBitmap(src, 0, 0, src.width, min(stripH, src.height))
        val stretched = Bitmap.createScaledBitmap(topStrip, w, extendH, true)

        val blurred = stackBlur(stretched, config.blurRadius.coerceIn(1, 120))

        canvas.drawBitmap(blurred, 0f, 0f, null)

        val topAvg = sampleTopColor(src, ratio = 0.2f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(30, Color.red(topAvg), Color.green(topAvg), Color.blue(topAvg))
        }
        canvas.drawRect(0f, 0f, w.toFloat(), extendH.toFloat(), paint)
        paint.color = Color.argb(15, 255, 255, 255)
        canvas.drawRect(0f, 0f, w.toFloat(), extendH.toFloat(), paint)
    }

    private fun drawFeather(canvas: Canvas, w: Int, extendH: Int, featherWidth: Int) {
        val feather = featherWidth.coerceIn(0, extendH)
        val startY = (extendH - feather).toFloat()
        val endY = extendH.toFloat()
        if (endY <= startY) return

        canvas.save()
        canvas.clipRect(0f, startY, w.toFloat(), endY)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            shader = LinearGradient(
                0f, startY, 0f, endY,
                intArrayOf(Color.TRANSPARENT, Color.BLACK),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, startY, w.toFloat(), endY, paint)
        paint.shader = null
        canvas.restore()
    }

    private fun sampleTopColor(src: Bitmap, ratio: Float): Int {
        val sample = Bitmap.createScaledBitmap(src, 32, 32, true)
        var r = 0; var g = 0; var b = 0; var count = 0
        val endY = (sample.height * ratio).roundToInt().coerceAtLeast(1)
        for (y in 0 until endY) {
            for (x in 0 until sample.width) {
                val c = sample.getPixel(x, y)
                if (Color.alpha(c) < 128) continue
                r += Color.red(c); g += Color.green(c); b += Color.blue(c); count++
            }
        }
        if (count == 0) return Color.WHITE
        return Color.rgb(r / count, g / count, b / count)
    }

    private fun ensureOpaque(src: Bitmap): Bitmap {
        if (!src.hasAlpha()) return src
        val b = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        Canvas(b).apply {
            drawColor(Color.WHITE)
            drawBitmap(src, 0f, 0f, null)
        }
        return b
    }

    private fun scaleToWidth(src: Bitmap, targetW: Int): Bitmap {
        if (src.width == targetW) return src
        val targetH = (targetW.toFloat() / src.width * src.height).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, targetW, targetH, true)
    }

    private fun stackBlur(s: Bitmap, radius: Int): Bitmap {
        val r = radius.coerceIn(1, 255)
        val w = s.width
        val h = s.height
        if (w <= 0 || h <= 0) return s

        val MAX_DIM = 1024
        val work = if (max(w, h) > MAX_DIM) {
            val scale = MAX_DIM.toFloat() / max(w, h)
            Bitmap.createScaledBitmap(s, (w * scale).roundToInt().coerceAtLeast(1), (h * scale).roundToInt().coerceAtLeast(1), true)
        } else {
            s
        }
        val ww = work.width
        val hh = work.height

        val pixels = IntArray(ww * hh)
        work.getPixels(pixels, 0, ww, 0, 0, ww, hh)

        val maxRad = (min(ww, hh) - 1) / 2
        val rad = min(r, maxRad).coerceAtLeast(1)

        try {
            stackBlurH(pixels, ww, hh, rad)
            stackBlurV(pixels, ww, hh, rad)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val out = Bitmap.createBitmap(ww, hh, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, ww, 0, 0, ww, hh)

        return out
    }

    private fun stackBlurH(pixels: IntArray, w: Int, h: Int, radius: Int) {
        val div = (2 * radius + 1).coerceAtLeast(1)
        val dv = IntArray(256 * div)
        for (i in 0 until 256 * div) dv[i] = i / div

        for (y in 0 until h) {
            var sumR = 0; var sumG = 0; var sumB = 0; var sumA = 0
            for (i in -radius..radius) {
                val xi = (i + w) % w
                val p = pixels[y * w + xi]
                sumR += Color.red(p); sumG += Color.green(p); sumB += Color.blue(p); sumA += Color.alpha(p)
            }
            for (x in 0 until w) {
                val outIdx = y * w + x
                pixels[outIdx] = Color.argb(
                    dv[sumA.coerceIn(0, 255 * div)],
                    dv[sumR.coerceIn(0, 255 * div)],
                    dv[sumG.coerceIn(0, 255 * div)],
                    dv[sumB.coerceIn(0, 255 * div)]
                )
                val xiOut = (x - radius + w) % w
                val xiIn = (x + radius + 1 + w) % w
                val pOut = pixels[y * w + xiOut]
                val pIn = pixels[y * w + xiIn]
                sumR += Color.red(pIn) - Color.red(pOut)
                sumG += Color.green(pIn) - Color.green(pOut)
                sumB += Color.blue(pIn) - Color.blue(pOut)
                sumA += Color.alpha(pIn) - Color.alpha(pOut)
            }
        }
    }

    private fun stackBlurV(pixels: IntArray, w: Int, h: Int, radius: Int) {
        val div = (2 * radius + 1).coerceAtLeast(1)
        val dv = IntArray(256 * div)
        for (i in 0 until 256 * div) dv[i] = i / div

        for (x in 0 until w) {
            var sumR = 0; var sumG = 0; var sumB = 0; var sumA = 0
            for (i in -radius..radius) {
                val yi = ((i + h) % h) * w + x
                sumR += Color.red(pixels[yi]); sumG += Color.green(pixels[yi]); sumB += Color.blue(pixels[yi]); sumA += Color.alpha(pixels[yi])
            }
            for (y in 0 until h) {
                val outIdx = y * w + x
                pixels[outIdx] = Color.argb(
                    dv[sumA.coerceIn(0, 255 * div)],
                    dv[sumR.coerceIn(0, 255 * div)],
                    dv[sumG.coerceIn(0, 255 * div)],
                    dv[sumB.coerceIn(0, 255 * div)]
                )
                val yiOut = ((y - radius + h) % h) * w + x
                val yiIn = ((y + radius + 1 + h) % h) * w + x
                sumR += Color.red(pixels[yiIn]) - Color.red(pixels[yiOut])
                sumG += Color.green(pixels[yiIn]) - Color.green(pixels[yiOut])
                sumB += Color.blue(pixels[yiIn]) - Color.blue(pixels[yiOut])
                sumA += Color.alpha(pixels[yiIn]) - Color.alpha(pixels[yiOut])
            }
        }
    }
}