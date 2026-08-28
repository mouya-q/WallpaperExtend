package com.wallpaperextend.util

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Log

/**
 * 壁纸设置工具 - 支持国产 ROM 兼容性
 */
object WallpaperSetter {

    private const val TAG = "WallpaperSetter"

    const val TARGET_HOME = 1
    const val TARGET_LOCK = 2
    const val TARGET_BOTH = 3

    // 国产 ROM 壁纸组件
    private val MIUI_WALLPAPER_PICKER = ComponentName(
        "com.android.thememanager",
        "com.android.thememanager.wallpaper.WallpaperPickerActivity"
    )

    private val COLOROS_WALLPAPER_PICKER = ComponentName(
        "com.coloros.gallery3d",
        "com.coloros.gallery3d.app.GalleryActivity"
    )

    private val FUNTOUCH_WALLPAPER_PICKER = ComponentName(
        "com.vivo.wallpaper",
        "com.vivo.wallpaper.ui.WallpaperActivity"
    )

    /**
     * 设置壁纸（兼容国产 ROM）
     */
    fun setWallpaper(context: Context, bitmap: Bitmap, target: Int = TARGET_BOTH): Boolean {
        // 先尝试标准 API
        return try {
            val manager = WallpaperManager.getInstance(context)

            when (target) {
                TARGET_HOME -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    } else {
                        @Suppress("DEPRECATION")
                        manager.setBitmap(bitmap)
                    }
                }
                TARGET_LOCK -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                    } else {
                        @Suppress("DEPRECATION")
                        manager.setBitmap(bitmap)
                    }
                }
                TARGET_BOTH -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        manager.setBitmap(bitmap, null, true,
                            WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                    } else {
                        @Suppress("DEPRECATION")
                        manager.setBitmap(bitmap)
                    }
                }
            }
            Log.d(TAG, "Wallpaper set successfully, target=$target")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Standard API failed, trying ROM-specific approach", e)
            // 标准 API 失败，尝试国产 ROM 特定方式
            setWallpaperROMFallback(context, bitmap, target)
        }
    }

    /**
     * 国产 ROM 回退方案
     */
    private fun setWallpaperROMFallback(context: Context, bitmap: Bitmap, target: Int): Boolean {
        return try {
            // 先保存到文件
            val savedPath = ImageSaver.saveToInternalStorage(context, bitmap, "wallpaper_fallback")
            if (savedPath == null) {
                Log.e(TAG, "Failed to save bitmap for fallback")
                return false
            }

            // 尝试启动系统壁纸选择器
            val intent = Intent(Intent.ACTION_ATTACH_DATA)
            intent.setType("image/*")
            intent.putExtra("mimeType", "image/*")

            val uri = android.net.Uri.parse(savedPath)
            intent.setDataAndType(uri, "image/*")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val chooser = Intent.createChooser(intent, "设置壁纸")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Log.d(TAG, "Launched wallpaper picker fallback")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Fallback also failed", e)
            false
        }
    }

    /**
     * 检测是否为 MIUI 设备
     */
    fun isMIUI(): Boolean {
        return getProp("ro.miui.ui.version.name") != null
    }

    /**
     * 检测是否为 ColorOS (OPPO/一加)
     */
    fun isColorOS(): Boolean {
        return getProp("ro.build.version.opporom") != null ||
               getProp("ro.build.version.oplus") != null
    }

    /**
     * 检测是否为 Funtouch OS (vivo)
     */
    fun isFuntouchOS(): Boolean {
        return getProp("ro.vivo.product.brand") != null
    }

    /**
     * 获取系统属性
     */
    private fun getProp(name: String): String? {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java)
            method.invoke(null, name) as? String
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 获取屏幕尺寸（用于计算延展目标尺寸）
     */
    fun getScreenDimensions(context: Context): Pair<Int, Int> {
        val displayMetrics = context.resources.displayMetrics
        return displayMetrics.widthPixels to displayMetrics.heightPixels
    }

    /**
     * 获取推荐的壁纸尺寸（考虑状态栏和导航栏）
     */
    fun getRecommendedWallpaperSize(context: Context): Pair<Int, Int> {
        val (width, height) = getScreenDimensions(context)
        // 壁纸通常需要比屏幕稍大以支持视差效果
        val wallpaperWidth = width
        val wallpaperHeight = (height * 1.2f).toInt()
        return wallpaperWidth to wallpaperHeight
    }
}