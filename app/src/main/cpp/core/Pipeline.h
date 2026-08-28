#ifndef PIPELINE_H
#define PIPELINE_H

#include "Processor.h"
#include <functional>
#include <memory>

namespace wallpaper {

class Pipeline {
public:
    Pipeline();
    ~Pipeline();

    // 处理图片
    Image process(const Image& input, int targetW, int targetH);

    // 配置
    void setConfig(const ProcessorConfig& config);
    ProcessorConfig getConfig() const;

    // 进度回调
    using ProgressCallback = std::function<void(float)>;
    void setProgressCallback(ProgressCallback cb);

    // 单例访问
    static Pipeline& getInstance();

private:
    std::unique_ptr<Processor> processor;
    ProgressCallback progressCb;
};

} // namespace wallpaper

#endif // PIPELINE_H