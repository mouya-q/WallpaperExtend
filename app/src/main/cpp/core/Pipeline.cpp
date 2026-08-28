#include "Pipeline.h"

namespace wallpaper {

Pipeline::Pipeline() : processor(std::make_unique<Processor>()) {}

Pipeline::~Pipeline() = default;

Image Pipeline::process(const Image& input, int targetW, int targetH) {
    return processor->process(input, targetW, targetH);
}

void Pipeline::setConfig(const ProcessorConfig& config) {
    processor->setConfig(config);
}

ProcessorConfig Pipeline::getConfig() const {
    return processor->getConfig();
}

void Pipeline::setProgressCallback(ProgressCallback cb) {
    progressCb = std::move(cb);
    processor->setProgressCallback(progressCb ? [](float p) {
        // 内部回调
    } : nullptr);
}

Pipeline& Pipeline::getInstance() {
    static Pipeline instance;
    return instance;
}

} // namespace wallpaper