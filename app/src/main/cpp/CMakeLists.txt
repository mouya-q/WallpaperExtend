cmake_minimum_required(VERSION 3.18.1)
project("wallpaperextend")

find_library(log-lib log)
find_library(android-lib android)

# Core library files
set(CORE_SOURCES
    core/Image.cpp
    core/Processor.cpp
    core/Pipeline.cpp
)

# JNI bridge
set(JNI_SOURCES
    jni/NativeProcessor.cpp
)

add_library(wallpaperextend SHARED
    ${CORE_SOURCES}
    ${JNI_SOURCES}
)

target_include_directories(wallpaperextend PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_link_libraries(wallpaperextend
    android
    ${log-lib}
)

# C++17 standard
set_target_properties(wallpaperextend PROPERTIES
    CXX_STANDARD 17
    CXX_STANDARD_REQUIRED ON
)