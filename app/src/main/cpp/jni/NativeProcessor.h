#ifndef NATIVE_PROCESSOR_H
#define NATIVE_PROCESSOR_H

#include <jni.h>
#include "core/Pipeline.h"

namespace wallpaper {
namespace jni {

class NativeProcessor {
public:
    static NativeProcessor& getInstance();

    // 初始化
    void init(JNIEnv* env);
    void release();

    // 处理图片
    jobject process(JNIEnv* env, jobject srcBitmap,
                    jint targetW, jint targetH,
                    jint blurRadius, jfloat extendRatio,
                    jint featherWidth);

    // 获取版本信息
    const char* getVersion() const { return "2.0.0-native"; }

private:
    NativeProcessor() = default;
    ~NativeProcessor() = default;
    NativeProcessor(const NativeProcessor&) = delete;
    NativeProcessor& operator=(const NativeProcessor&) = delete;

    bool initialized = false;
    Pipeline pipeline;
};

} // namespace jni
} // namespace wallpaper

#endif // NATIVE_PROCESSOR_H