#ifndef PROCESSOR_H
#define PROCESSOR_H

#include "Image.h"
#include <memory>
#include <functional>

namespace wallpaper {

struct ProcessorConfig {
    int blurRadius = 30;
    float extendRatio = 0.25f;
    int featherWidth = 40;
    bool topOnly = true;
    int maxDimension = 1024;
};

class Processor {
public:
    Processor();
    ~Processor();

    void setConfig(const ProcessorConfig& config);
    const ProcessorConfig& getConfig() const { return config; }

    Image process(const Image& input, int targetW, int targetH);

    using ProgressCallback = std::function<void(float)>;
    void setProgressCallback(const ProgressCallback& cb) { progressCb = cb; }

private:
    ProcessorConfig config;
    ProgressCallback progressCb = nullptr;

    VulkanBlur vulkanBlur;

    void extendTop(Image& output, const Image& input, int extendH);
    void applyFeather(Image& output, int extendH);
    void applyBlur(Image& image, int radius);

    void stackBlurH(uint32_t* pixels, int w, int h, int radius);
    void stackBlurV(uint32_t* pixels, int w, int h, int radius);
    uint32_t sampleTopColor(const Image& image, float ratio = 0.2f);
};

} // namespace wallpaper

#endif // PROCESSOR_H