# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- C++ Native layer with JNI bridge for image processing
- AI-powered image extension support (experimental)
- Liquid glass visual effect (GlassSurface component)
- ROM detection utilities (MIUI, ColorOS, FuntouchOS)
- Wallpaper setter fallback for Chinese ROMs
- ImageLoader error handling with Result type
- Internal storage save for wallpaper fallback
- Recommended wallpaper size calculation

### Changed

- WallpaperProcessor now uses `suspend` with `Dispatchers.Default`
- Removed unnecessary Bitmap.recycle() calls (GC managed)
- ImageLoader.loadFromUri() now returns `LoadResult` sealed class
- Build system now includes NDK/CMake configuration
- ABI filters limited to arm64-v8a and armeabi-v7a

### Improved

- True async processing with coroutines
- Better error handling throughout the pipeline
- 国产 ROM wallpaper setting compatibility
- Memory management (reduced manual recycling)

## [2.3.0] - 2025-01-22

### Added

- Authentic liquid glass rewrite of `GlassSurface`, ported from the `com.kyant.backdrop` (AndroidLiquidGlass-kmp) reference: layered architecture with a frosted material layer, an AGSL SDF refraction layer (iOS-style edge lens distortion, API >= 33), and a crisp foreground content layer so card text never blurs
- SDF highlight stroke (AGSL `DefaultHighlightShader`-equivalent) along the rounded-rect normal, plus an internal specular gradient, top sheen and bottom ambient shade for true glass depth
- iOS-like motion: spring entrance scale/alpha, animated refraction ramp-in, and press-driven refraction amplification with a focal offset toward the touch point

### Changed

- Glass material layer now chains `RenderEffect.createBlurEffect` with an AGSL `RuntimeShader` SDF refraction effect (API >= 33); API 31-32 keeps blur only; API < 31 uses a solid white fallback
- Replaced the single blurred `Box` with a three-layer `Box` stack (material + highlight + content) for correct z-ordering and crisp text

### Notes

- RuntimeShader SDF effects require API 33; lower APIs degrade gracefully to blur / solid without crashing

## [2.1.0] - 2025-01-15

### Added

- True liquid glass surface via `RenderEffect.createBlurEffect` (GlassSurface, API >= S) with semi-transparent fallback (API < S)
- White MIUIX-style light theme across Home and About screens
- Staggered card layout using GlassSurface with blueprint highlight stroke

### Changed

- All cards (ImageInfo, Parameters, OriginalPreview, ResultPreview, AppInfo, Developers, License, SpecialThanks) migrated from dark `Surface` to white `GlassSurface`
- Light color tokens aligned to MIUIX system palette: surface `0xFFF2F2F7`, primary text `0xFF1C1C1E`, secondary text `0xFF8E8E93`, accent `0xFF0A84FF`
- Removed MIUIX / backdrop dependencies (compileSdk 37 requirement incompatible with current AGP 8.7)
- Bumped versionCode to 3

### Fixed

- Liquid glass text blurred: `GlassSurface` now splits into a blurred material background layer and a separate sharp foreground content layer, so card text stays crisp while only the backdrop is frosted
- Startup crash caused by `LazyColumn` nested with infinite height scroll constraint (IllegalStateException)
- Restored `lifecycle-viewmodel-compose` dependency for Compose ViewModel
- Cloud build: rewrite Gradle files as valid Groovy DSL (`abiFilters`, `minifyEnabled`, `buildFeatures`, `packaging`); drop conflicting `.kts` files
- Cloud build: fix GLSL shader `float4` -> `vec4` in `gaussian_blur.comp`
- Cloud build: fix C++ `NativeProcessor` singleton access (avoid `new`/`delete` on private ctor)
- Cloud build: fix `ProgressCallback` as `std::function` and a brace error in `stackBlurV`
- Cloud build: link `jnigraphics` for `AndroidBitmap_*` symbols

## [2.2.0] - 2025-01-20

### Added

- Phase 1 C++ pipeline integration: `WallpaperProcessor` now loads `libwallpaperextend` via JNI and routes processing through the native `Pipeline` (`core/Image` -> `core/Processor` -> `core/Pipeline`) with `nativeInit` / `nativeProcess` / `nativeRelease`
- Kotlin processing path retained as automatic fallback when the native library fails to load, preserving incremental Kotlin -> C++ migration
- Removed conflicting `app/build.gradle.kts`; single Groovy `build.gradle` is the source of truth

### Changed

- `WallpaperProcessor.processAsync` tries native engine first, falls back to Kotlin `processKotlin` on any failure (no behavior regression)
- Native pipeline performs Stack Blur extension on CPU with tiled downscale (`maxDimension = 1024`) matching the original Kotlin visual output

### Notes

- Phase 2 (OpenCV + Vulkan) and Phase 3 (TFLite AI segmentation) directories are scaffolded but empty; target device Snapdragon 865 / Adreno 650

## [2.0.0] - 2025-01-01

### Changed

- Complete UI rewrite using Jetpack Compose and MIUIX
- Migrated from View system to Compose
- Updated build system to Kotlin DSL
- Bumped minSdk to 26 (Android 8.0)
- Bumped targetSdk to 35
- Updated Kotlin to 2.0.21
- Updated Gradle to 8.6
- Updated AGP to 8.5.0

### Added

- MVVM architecture with ViewModel
- About screen with developer information
- GitHub avatars for developers
- Material 3 design language
- Smooth screen transitions
- Compose-based parameter sliders

### Removed

- ViewBinding dependency
- PhotoView dependency (using Compose image display)
- AppCompat dependency
- Material Components dependency
- Unused dominant color extraction code

## [1.0.0] - 2024-01-01

### Added

- Initial release
- iOS 17 style wallpaper extension
- Stack Blur algorithm
- Real-time parameter adjustment
- Save to gallery functionality
- Share intent support