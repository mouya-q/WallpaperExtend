#ifndef IMAGE_H
#define IMAGE_H
#include <android/bitmap.h>
#include <cstdint>
#include <vector>

namespace wallpaper {

struct ImageData {
    int width = 0;
    int height = 0;
    std::vector<uint32_t> pixels;
    ImageData() = default;
    ImageData(int w, int h) : width(w), height(h), pixels(w * h, 0) {}
    uint32_t& at(int x, int y) { return pixels[y * width + x]; }
    const uint32_t& at(int x, int y) const { return pixels[y * width + x]; }
    bool empty() const { return pixels.empty(); }
    size_t size() const { return pixels.size(); }
};

class Image {
public:
    Image() = default;
    explicit Image(int w, int h);

    bool loadFromBitmap(JNIEnv* env, jobject bitmap);
    jobject createBitmap(JNIEnv* env) const;
    int width() const { return imageData.width; }
    int height() const { return imageData.height; }
    uint32_t* pixels() { return imageData.pixels.data(); }
    const uint32_t* pixels() const { return imageData.pixels.data(); }
    uint32_t& at(int x, int y) { return imageData.at(x, y); }
    const uint32_t& at(int x, int y) const { return imageData.at(x, y); }
    ImageData imageData;

private:
    bool valid = false;
};

} // namespace wallpaper

#endif
