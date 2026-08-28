#include "Image.h"
#include <android/log.h>

#define LOG_TAG "WallpaperExtend"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

namespace wallpaper {

Image::Image(int w, int h) : data(w, h), valid(true) {}

bool Image::loadFromBitmap(JNIEnv* env, jobject bitmap) {
    if (bitmap == nullptr) return false;

    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        LOGD("Failed to get bitmap info");
        return false;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGD("Unsupported bitmap format: %d", info.format);
        return false;
    }

    data.width = info.width;
    data.height = info.height;
    data.pixels.resize(info.width * info.height);

    void* pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        LOGD("Failed to lock pixels");
        return false;
    }

    memcpy(data.pixels.data(), pixels, data.pixels.size() * sizeof(uint32_t));
    AndroidBitmap_unlockPixels(env, bitmap);

    valid = true;
    return true;
}

jobject Image::createBitmap(JNIEnv* env) const {
    if (data.empty()) return nullptr;

    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    if (bitmapClass == nullptr) return nullptr;

    jmethodID createMethod = env->GetStaticMethodID(bitmapClass, "createBitmap",
            "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    if (createMethod == nullptr) {
        env->DeleteLocalRef(bitmapClass);
        return nullptr;
    }

    jclass configClass = env->FindClass("android/graphics/Bitmap$Config");
    jfieldID argb8888Field = env->GetStaticFieldID(configClass, "ARGB8888",
            "Landroid/graphics/Bitmap$Config;");
    if (argb8888Field == nullptr) {
        env->DeleteLocalRef(bitmapClass);
        env->DeleteLocalRef(configClass);
        return nullptr;
    }

    jobject config = env->GetStaticObjectField(configClass, argb8888Field);
    jobject bitmap = env->CallStaticObjectMethod(bitmapClass, createMethod,
            data.width, data.height, config);

    env->DeleteLocalRef(configClass);
    env->DeleteLocalRef(bitmapClass);

    if (bitmap == nullptr) return nullptr;

    void* pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        env->DeleteLocalRef(bitmap);
        return nullptr;
    }

    memcpy(pixels, data.pixels.data(), data.pixels.size() * sizeof(uint32_t));
    AndroidBitmap_unlockPixels(env, bitmap);

    return bitmap;
}

} // namespace wallpaper