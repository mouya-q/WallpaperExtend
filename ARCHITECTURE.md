# WallpaperExtend - Architecture

## Architecture Overview

The app follows a **hybrid architecture** with Kotlin/Compose UI layer and C++ Native processing engine.

```
┌─────────────────────────────────────────────────────────────────┐
│                     Kotlin/Compose UI Layer                      │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────────┐│
│  │  MainActivity│  │  WallpaperVM │  │  GlassSurface (Effect)  ││
│  └──────┬──────┘  └──────┬───────┘  └─────────────────────────┘│
└─────────┼────────────────┼──────────────────────────────────────┘
          │                │
          │           JNI Bridge
          │                │
┌─────────▼────────────────▼──────────────────────────────────────┐
│                   C++ Image Processing Engine                    │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────────┐│
│  │   Image     │  │  Processor   │  │      Pipeline           ││
│  │  (Bitmap)   │  │  (Algorithm) │  │   (Orchestration)       ││
│  └─────────────┘  └──────────────┘  └─────────────────────────┘│
│                                                              │
│  Phase 2: OpenCV Integration                                 │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────────┐│
│  │ EdgeDetect  │  │ SkySegment   │  │    TextureFill          ││
│  └─────────────┘  └──────────────┘  └─────────────────────────┘│
│                                                              │
│  Phase 2: Vulkan Compute                                     │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────────────────────┐│
│  │ BlurCompute │  │ ResizeCompute│  │    BlendCompute         ││
│  └─────────────┘  └──────────────┘  └─────────────────────────┘│
└──────────────────────────────────────────────────────────────────┘
```

## Module Structure

```
com.wallpaperextend
├── ui
│   └── MainActivity.kt              # Entry point, Compose UI
│   └── WallpaperViewModel.kt        # Business logic, state management
│   └── GlassSurface.kt              # Liquid glass visual effect
│   └── AIViewModel.kt               # AI extension (experimental)
│   └── AIScreen.kt                  # AI extension UI
├── processor
│   └── WallpaperProcessor.kt        # Kotlin wrapper for native processing
├── util
│   ├── ImageLoader.kt               # Bitmap loading with Result type
│   ├── ImageSaver.kt                # MediaStore save utility
│   └── WallpaperSetter.kt           # ROM-compatible wallpaper setter
└── ai
    └── AIApi.kt                     # AI extension API (experimental)

cpp/ (Native Layer)
├── core
│   ├── Image.h / Image.cpp          # Bitmap wrapper with JNI bridge
│   ├── Processor.h / Processor.cpp  # Core extension algorithm
│   └── Pipeline.h / Pipeline.cpp    # Processing pipeline orchestration
├── jni
│   └── NativeProcessor.h / .cpp     # JNI bridge to Kotlin
├── opencv/                          # Phase 2: OpenCV integration
├── vulkan/                          # Phase 2: Vulkan compute shaders
└── shaders/                         # GLSL compute shaders
    ├── gaussian_blur.comp           # Horizontal blur pass
    ├── gaussian_blur_v.comp         # Vertical blur pass
    ├── resize.comp                  # Bilinear resize
    └── blend.comp                   # Alpha blending
```

## Image Processing Pipeline

### Phase 1 (Current - C++ Native)

```
1. Load Image (Kotlin ImageLoader)
   └── Sample scaling + EXIF rotation
   └── Returns LoadResult<Bitmap> (sealed class)

2. JNI Bridge (NativeProcessor)
   └── AndroidBitmap_lockPixels() for zero-copy access
   └── Direct pixel data transfer

3. C++ Processing (Processor)
   ├── Extend top region (stretch + blur)
   ├── Apply feather mask (alpha gradient)
   └── Composite original image

4. Output (JNI)
   └── CreateBitmap from processed pixels
```

### Phase 2 (Planned - OpenCV + Vulkan)

```
1. Image Analysis (OpenCV DNN)
   ├── Sky segmentation
   ├── Building detection
   ├── Person/subject detection
   └── Texture classification

2. Smart Extension
   ├── Sky: Gradient extrapolation + noise synthesis
   ├── Building: cv::inpaint() (Telea/Navier-Stokes)
   └── Texture: PatchMatch fill

3. GPU Processing (Vulkan Compute)
   ├── Resize: Bilinear interpolation shader
   ├── Blur: Separable Gaussian (H + V passes)
   └── Blend: Gradient mask composition

4. Output via AHardwareBuffer
   └── Zero-copy GPU to Bitmap
```

## Key Design Decisions

### C++ Native Engine
- Zero-copy pixel access via AndroidBitmap_lockPixels
- Stack blur algorithm optimized for ARM NEON
- Memory-efficient tiling for large images

### JNI Design
- No byte[] marshalling overhead
- Direct Bitmap pointer passing
- Single-pass processing

### Kotlin Safety
- Result types instead of exceptions
- Sealed class for load states
- Coroutines for true async processing

## Performance Considerations

1. **Memory**: Tiled processing for images > 1024px
2. **CPU**: Stack blur with running sum optimization
3. **GPU (Phase 2)**: Vulkan compute shaders for blur/resize
4. **Zero-copy**: AHardwareBuffer for GPU pipeline

## Build System

```gradle
android {
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    ndk {
        abiFilters += listOf("arm64-v8a", "armeabi-v7a")
    }
}
```

## Future Work

### Phase 2
- [ ] OpenCV DNN integration for segmentation
- [ ] Vulkan compute pipeline
- [ ] AHardwareBuffer zero-copy
- [ ] NEON optimization for blur

### Phase 3
- [ ] AI-powered sky synthesis
- [ ] Depth effect for subjects
- [ ] Dynamic wallpaper support

### Phase 4 (HyperOS)
- [ ] Status bar color sampling
- [ ] Always-on display support
- [ ] Super wallpaper integration