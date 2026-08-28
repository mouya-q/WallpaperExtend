# WallpaperExtend

将任意图片智能延展为适配手机屏幕的壁纸，模拟 iOS 17 的 **Extend Wallpaper** 效果。

## 特性

- **智能延展** — 自动识别图片方向，顶部模糊延展，自然过渡
- **实时调节** — 模糊半径、延展比例、羽化宽度，所见即所得
- **保存到相册** — 一键导出 PNG，支持分享
- **分享接入** — 从其他应用直接「分享到」本 App 处理
- **AI 扩展** — 支持 AI 驱动的图像扩展（实验性）
- **液态玻璃效果** — 基于 AndroidLiquidGlass 的视觉效果
- **ROM 兼容** — 支持 MIUI、ColorOS、FuntouchOS 等

## 技术栈

- **UI**: Jetpack Compose + MIUIX
- **架构**: MVVM + ViewModel
- **图像处理**: 原生 Bitmap 操作 + Stack Blur 算法
- **Native 加速**: C++ JNI 层（实验性）
- **最低版本**: Android 8.0 (API 26)


## 开发者

- **海葉なっふ** ([@Nafutsu]) — 项目发起
- **もうや** ([@mouya-q]) —核心开发，重构

## 开源协议

Apache 2.0

## 致谢

- [MIUIX](https://github.com/Yukonga/miuix) — Yukonga
- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) — Kyant