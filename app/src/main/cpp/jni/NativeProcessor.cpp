#include "NativeProcessor.h"
#include <android/log.h>

#define LOG_TAG "WallpaperExtend"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace wallpaper {
namespace jni {

NativeProcessor& NativeProcessor::getInstance() {
    static NativeProcessor instance;
    return instance;
}

void NativeProcessor::init(JNIEnv* env) {
    if (initialized) return;
    LOGD("NativeProcessor initialized, version: %s", getVersion());
    initialized = true;
}

void NativeProcessor::release() {
    initialized = false;
}

jobject NativeProcessor::process(JNIEnv* env, jobject srcBitmap,
                                  jint targetW, jint targetH,
                                  jint blurRadius, jfloat extendRatio,
                                  jint featherWidth) {
    if (!initialized) {
        LOGE("NativeProcessor not initialized");
        return nullptr;
    }

    // 加载输入图片
    Image input;
    if (!input.loadFromBitmap(env, srcBitmap)) {
        LOGE("Failed to load bitmap");
        return nullptr;
    }

    LOGD("Processing: %dx%d -> %dx%d", input.width(), input.height(), targetW, targetH);

    // 设置配置
    ProcessorConfig config;
    config.blurRadius = blurRadius;
    config.extendRatio = extendRatio;
    config.featherWidth = featherWidth;
    config.topOnly = true;
    pipeline.setConfig(config);

    // 处理
    Image output = pipeline.process(input, targetW, targetH);

    LOGD("Processing complete: %dx%d", output.width(), output.height());

    // 创建输出 Bitmap
    return output.createBitmap(env);
}

} // namespace jni
} // namespace wallpaper

// JNI 入口函数
extern "C" {

JNIEXPORT jlong JNICALL
Java_com_wallpaperextend_processor_WallpaperProcessor_nativeInit(
        JNIEnv* env, jobject thiz) {
    wallpaper::jni::NativeProcessor::getInstance().init(env);
    return reinterpret_cast<jlong>(new wallpaper::jni::NativeProcessor());
}

JNIEXPORT void JNICALL
Java_com_wallpaperextend_processor_WallpaperProcessor_nativeRelease(
        JNIEnv* env, jobject thiz, jlong handle) {
    auto* processor = reinterpret_cast<wallpaper::jni::NativeProcessor*>(handle);
    if (processor) {
        processor->release();
        delete processor;
    }
}

JNIEXPORT jobject JNICALL
Java_com_wallpaperextend_processor_WallpaperProcessor_nativeProcess(
        JNIEnv* env, jobject thiz,
        jobject srcBitmap, jint targetW, jint targetH,
        jint blurRadius, jfloat extendRatio, jint featherWidth) {
    return wallpaper::jni::NativeProcessor::getInstance().process(
            env, srcBitmap, targetW, targetH, blurRadius, extendRatio, featherWidth);
}

}