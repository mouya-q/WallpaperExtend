#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <cstdint>
#include <algorithm>
#include <vector>

#define LOG_TAG "WallpaperExtend"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

extern "C" {

static jclass findBitmapClass(JNIEnv* env) {
    jclass clazz = env->FindClass("android/graphics/Bitmap");
    if (clazz == nullptr) {
        LOGD("Failed to find Bitmap class");
        return nullptr;
    }
    return (jclass)env->NewGlobalRef(clazz);
}

static jmethodID findCreateBitmapMethod(JNIEnv* env, jclass bitmapClass) {
    jmethodID methodId = env->GetStaticMethodID(bitmapClass, "createBitmap",
            "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    if (methodId == nullptr) {
        LOGD("Failed to find createBitmap method");
    }
    return methodId;
}

JNIEXPORT jobject JNICALL
Java_com_wallpaperextend_processor_WallpaperProcessor_nativeProcess(
        JNIEnv *env, jobject thiz,
        jobject srcBitmap, jint targetW, jint targetH,
        jint blurRadius, jfloat extendRatio, jint featherWidth) {

    AndroidBitmapInfo srcInfo;
    if (AndroidBitmap_getInfo(env, srcBitmap, &srcInfo) < 0) {
        LOGD("Failed to get bitmap info");
        return nullptr;
    }

    if (srcInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGD("Unsupported bitmap format");
        return nullptr;
    }

    void *srcPixels;
    if (AndroidBitmap_lockPixels(env, srcBitmap, &srcPixels) < 0) {
        LOGD("Failed to lock pixels");
        return nullptr;
    }

    int srcW = srcInfo.width;
    int srcH = srcInfo.height;
    uint32_t *srcData = static_cast<uint32_t*>(srcPixels);

    int extendH = static_cast<int>(targetH * std::max(0.0f, std::min(0.6f, extendRatio)));
    int outH = extendH + srcH;

    // Create output bitmap using Java API
    jclass bitmapClass = findBitmapClass(env);
    if (bitmapClass == nullptr) {
        AndroidBitmap_unlockPixels(env, srcBitmap);
        return nullptr;
    }

    jmethodID createMethod = findCreateBitmapMethod(env, bitmapClass);
    if (createMethod == nullptr) {
        AndroidBitmap_unlockPixels(env, srcBitmap);
        return nullptr;
    }

    jclass configClass = env->FindClass("android/graphics/Bitmap$Config");
    jfieldID argb8888Field = env->GetStaticFieldID(configClass, "ARGB8888",
            "Landroid/graphics/Bitmap$Config;");
    jobject config = env->GetStaticObjectField(configClass, argb8888Field);

    jobject outBitmap = env->CallStaticObjectMethod(bitmapClass, createMethod,
            targetW, outH, config);

    if (outBitmap == nullptr) {
        AndroidBitmap_unlockPixels(env, srcBitmap);
        return nullptr;
    }

    AndroidBitmapInfo outInfo;
    AndroidBitmap_getInfo(env, outBitmap, &outInfo);

    void *outPixels;
    if (AndroidBitmap_lockPixels(env, outBitmap, &outPixels) < 0) {
        AndroidBitmap_unlockPixels(env, srcBitmap);
        return nullptr;
    }

    uint32_t *outData = static_cast<uint32_t*>(outPixels);

    // Fill with transparent
    memset(outData, 0, targetW * outH * 4);

    // Draw original image centered
    int drawX = std::max(0, (targetW - srcW) / 2);
    for (int y = 0; y < srcH; y++) {
        for (int x = 0; x < srcW; x++) {
            outData[(y + extendH) * targetW + (x + drawX)] = srcData[y * srcW + x];
        }
    }

    AndroidBitmap_unlockPixels(env, srcBitmap);
    AndroidBitmap_unlockPixels(env, outBitmap);

    return outBitmap;
}

JNIEXPORT void JNICALL
Java_com_wallpaperextend_processor_WallpaperProcessor_nativeRelease(JNIEnv *env, jobject thiz, jlong handle) {
    // Cleanup if needed
}

}