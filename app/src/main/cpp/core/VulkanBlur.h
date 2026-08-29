#ifndef VULKAN_BLUR_H
#define VULKAN_BLUR_H

#include <cstdint>
#include <vector>

namespace wallpaper {

// GPU 加速的 RGBA_8888 栈模糊 + 双线性缩放。
// 内部使用 Vulkan 计算管线；若设备不支持 Vulkan 或分配失败，调用方应回退到 CPU 路径。
class VulkanBlur {
public:
    VulkanBlur();
    ~VulkanBlur();

    // 对 RGBA_8888 像素做两遍栈模糊（水平 + 垂直），radius 为模糊半径。
    // 返回 true 表示成功（pixels 已被就地修改），false 表示需要回退 CPU。
    bool blur(uint32_t* pixels, int width, int height, int radius);

    // 将 src 缩放到 dstW x dstH（双线性），用于延展带的降采样模糊再上采样。
    // 返回 true 表示成功。
    bool resize(const uint32_t* src, int srcW, int srcH,
                uint32_t* dst, int dstW, int dstH);

    bool isAvailable() const { return available; }

private:
    bool init();
    void destroy();
    bool available = false;
    bool initialized = false;
};

} // namespace wallpaper

#endif // VULKAN_BLUR_H
