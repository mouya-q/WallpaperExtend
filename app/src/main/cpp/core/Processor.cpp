#include "Processor.h"
#include <algorithm>
#include <cstring>

namespace wallpaper {

Processor::Processor() {}
Processor::~Processor() {}

void Processor::setConfig(const ProcessorConfig& cfg) {
    config = cfg;
}

Image Processor::process(const Image& input, int targetW, int targetH) {
    if (progressCb) progressCb(0.0f);
    const int srcW = input.width();
    const int srcH = input.height();

    const int extendH = config.topOnly
        ? static_cast<int>(targetH * std::clamp(config.extendRatio, 0.0f, 0.6f))
        : 0;
    const int outH = extendH + srcH;

    Image output(targetW, outH);
    std::memset(output.pixels(), 0, output.imageData.size() * sizeof(uint32_t));

    if (progressCb) progressCb(0.1f);

    if (extendH > 0) {
        extendTop(output, input, extendH);
        if (progressCb) progressCb(0.5f);
        applyFeather(output, extendH);
        if (progressCb) progressCb(0.7f);
    }

    const int drawX = std::max(0, (targetW - srcW) / 2);
    for (int y = 0; y < srcH; y++) {
        for (int x = 0; x < srcW; x++) {
            output.at(x + drawX, y + extendH) = input.at(x, y);
        }
    }

    if (progressCb) progressCb(1.0f);
    return output;
}

void Processor::extendTop(Image& output, const Image& input, int extendH) {
    const int srcW = input.width();
    const int srcH = input.height();
    const int outW = output.width();

    const int stripH = std::max(8, srcH / 40);
    const int copyH = std::min(stripH, srcH);

    std::vector<uint32_t> stretched(extendH * outW);
    for (int y = 0; y < extendH; y++) {
        const float srcY = (static_cast<float>(y) / extendH) * copyH;
        const int srcYInt = std::min(static_cast<int>(srcY), copyH - 1);
        for (int x = 0; x < outW; x++) {
            const int srcX = std::min(x, srcW - 1);
            stretched[y * outW + x] = input.at(srcX, srcYInt);
        }
    }

    const int blurRad = std::clamp(config.blurRadius, 1, 120);
    if (blurRad > 0) {
        const int procW = std::min(outW, config.maxDimension);
        const int procH = std::min(extendH, config.maxDimension);
        const float scaleX = static_cast<float>(procW) / outW;
        const float scaleY = static_cast<float>(procH) / extendH;

        std::vector<uint32_t> small(procW * procH);
        for (int y = 0; y < procH; y++) {
            for (int x = 0; x < procW; x++) {
                const int sx = std::min(static_cast<int>(x / scaleX), outW - 1);
                const int sy = std::min(static_cast<int>(y / scaleY), extendH - 1);
                small[y * procW + x] = stretched[sy * outW + sx];
            }
        }

        const int smallRadius = std::max(1, static_cast<int>(blurRad * scaleX));
        stackBlurH(small.data(), procW, procH, smallRadius);
        stackBlurV(small.data(), procW, procH, smallRadius);

        for (int y = 0; y < extendH; y++) {
            for (int x = 0; x < outW; x++) {
                const int sx = std::min(static_cast<int>(x * scaleX), procW - 1);
                const int sy = std::min(static_cast<int>(y * scaleY), procH - 1);
                stretched[y * outW + x] = small[sy * procW + sx];
            }
        }
    }

    for (int y = 0; y < extendH; y++) {
        for (int x = 0; x < outW; x++) {
            output.at(x, y) = stretched[y * outW + x];
        }
    }

    const uint32_t topColor = sampleTopColor(input, 0.2f);
    const uint8_t r = (topColor >> 16) & 0xFF;
    const uint8_t g = (topColor >> 8) & 0xFF;
    const uint8_t b = topColor & 0xFF;

    for (int y = 0; y < extendH; y++) {
        for (int x = 0; x < outW; x++) {
            uint32_t& pixel = output.at(x, y);
            const uint8_t pr = (pixel >> 16) & 0xFF;
            const uint8_t pg = (pixel >> 8) & 0xFF;
            const uint8_t pb = pixel & 0xFF;
            const uint8_t nr = static_cast<uint8_t>((pr * 0.85f) + (r * 0.15f));
            const uint8_t ng = static_cast<uint8_t>((pg * 0.85f) + (g * 0.15f));
            const uint8_t nb = static_cast<uint8_t>((pb * 0.85f) + (b * 0.15f));
            pixel = (pixel & 0xFF000000) | (nr << 16) | (ng << 8) | nb;
        }
    }
}

void Processor::applyFeather(Image& output, int extendH) {
    const int feather = std::clamp(config.featherWidth, 0, extendH);
    if (feather <= 0) return;
    const int startY = extendH - feather;
    const int endY = extendH;
    const int outW = output.width();

    for (int y = startY; y < endY; y++) {
        const float alpha = static_cast<float>(y - startY) / feather;
        for (int x = 0; x < outW; x++) {
            uint32_t& pixel = output.at(x, y);
            const uint8_t currentAlpha = (pixel >> 24) & 0xFF;
            const uint8_t newAlpha = static_cast<uint8_t>(currentAlpha * alpha);
            pixel = (newAlpha << 24) | (pixel & 0x00FFFFFF);
        }
    }
}

void Processor::applyBlur(Image& image, int radius) {
    if (radius <= 0) return;
    stackBlurH(image.pixels(), image.width(), image.height(), radius);
    stackBlurV(image.pixels(), image.width(), image.height(), radius);
}

void Processor::stackBlurH(uint32_t* pixels, int w, int h, int radius) {
    const int div = 2 * radius + 1;
    const int divSize = 256 * div;
    std::vector<uint8_t> dv(divSize);
    for (int i = 0; i < divSize; i++) dv[i] = i / div;

    for (int y = 0; y < h; y++) {
        int sumR = 0, sumG = 0, sumB = 0, sumA = 0;
        for (int i = -radius; i <= radius; i++) {
            const int xi = ((i % w) + w) % w;
            const uint32_t p = pixels[y * w + xi];
            sumR += (p >> 16) & 0xFF;
            sumG += (p >> 8) & 0xFF;
            sumB += p & 0xFF;
            sumA += (p >> 24) & 0xFF;
        }
        for (int x = 0; x < w; x++) {
            const int idx = y * w + x;
            pixels[idx] = (dv[std::clamp(sumA, 0, 255 * div)] << 24) |
                         (dv[std::clamp(sumR, 0, 255 * div)] << 16) |
                         (dv[std::clamp(sumG, 0, 255 * div)] << 8) |
                         dv[std::clamp(sumB, 0, 255 * div)];
            const int xiOut = ((x - radius) % w + w) % w;
            const int xiIn = ((x + radius + 1) % w);
            const uint32_t pOut = pixels[y * w + xiOut];
            const uint32_t pIn = pixels[y * w + xiIn];
            sumR += ((pIn >> 16) & 0xFF) - ((pOut >> 16) & 0xFF);
            sumG += ((pIn >> 8) & 0xFF) - ((pOut >> 8) & 0xFF);
            sumB += (pIn & 0xFF) - (pOut & 0xFF);
            sumA += ((pIn >> 24) & 0xFF) - ((pOut >> 24) & 0xFF);
        }
    }
}

void Processor::stackBlurV(uint32_t* pixels, int w, int h, int radius) {
    const int div = 2 * radius + 1;
    const int divSize = 256 * div;
    std::vector<uint8_t> dv(divSize);
    for (int i = 0; i < divSize; i++) dv[i] = i / div;

    for (int x = 0; x < w; x++) {
        int sumR = 0, sumG = 0, sumB = 0, sumA = 0;
        for (int i = -radius; i <= radius; i++) {
            const int yi = (((i % h) + h) % h) * w + x;
            const uint32_t p = pixels[yi];
            sumR += (p >> 16) & 0xFF;
            sumG += (p >> 8) & 0xFF;
            sumB += p & 0xFF;
            sumA += (p >> 24) & 0xFF;
        }
        for (int y = 0; y < h; y++) {
            const int idx = y * w + x;
            pixels[idx] = (dv[std::clamp(sumA, 0, 255 * div)] << 24) |
                         (dv[std::clamp(sumR, 0, 255 * div)] << 16) |
                         (dv[std::clamp(sumG, 0, 255 * div)] << 8) |
                         dv[std::clamp(sumB, 0, 255 * div)];
            const int yiOut = (((y - radius) % h) + h) % h * w + x;
            const int yiIn = (((y + radius + 1) % h)) * w + x;
            const uint32_t pOut = pixels[yiOut];
            const uint32_t pIn = pixels[yiIn];
            sumR += ((pIn >> 16) & 0xFF) - ((pOut >> 16) & 0xFF);
            sumG += ((pIn >> 8) & 0xFF) - ((pOut >> 8) & 0xFF);
            sumB += (pIn & 0xFF) - (pOut & 0xFF);
            sumA += ((pIn >> 24) & 0xFF) - ((pOut >> 24) & 0xFF);
        }
    }
}

uint32_t Processor::sampleTopColor(const Image& image, float ratio) {
    const int h = image.height();
    const int sampleH = std::max(1, static_cast<int>(h * ratio));
    const int w = std::min(image.width(), 32);
    int64_t r = 0, g = 0, b = 0, count = 0;

    for (int y = 0; y < sampleH; y++) {
        for (int x = 0; x < w; x++) {
            const uint32_t pixel = image.at(x, y);
            const uint8_t alpha = (pixel >> 24) & 0xFF;
            if (alpha < 128) continue;
            r += (pixel >> 16) & 0xFF;
            g += (pixel >> 8) & 0xFF;
            b += pixel & 0xFF;
            count++;
        }
    }
    if (count == 0) return 0xFFFFFFFF;
    return (0xFF << 24) | ((r / count) << 16) | ((g / count) << 8) | (b / count);
}

} // namespace wallpaper