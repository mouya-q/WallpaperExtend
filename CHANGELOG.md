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

- Startup crash caused by `LazyColumn` nested with infinite height scroll constraint (IllegalStateException)
- Restored `lifecycle-viewmodel-compose` dependency for Compose ViewModel

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