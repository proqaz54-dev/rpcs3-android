# Android entry point for RPCS3.
# Currently builds the JNI bridge and Android callbacks only.
# The emulator core (rpcs3_emu) will be pulled in from the parent
# RPCS3 CMake tree once the toolchain setup is verified.

cmake_minimum_required(VERSION 3.22.1)
project(rpcs3_android LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(rpcs3_android SHARED
    rpcs3_android_jni.cpp
    android_callbacks.cpp
)

target_include_directories(rpcs3_android PRIVATE
    ${CMAKE_SOURCE_DIR}/../../../../rpcs3
)

target_compile_definitions(rpcs3_android PRIVATE
    ANDROID
    _CRT_SECURE_NO_WARNINGS
)

find_library(log-lib log)
target_link_libraries(rpcs3_android PRIVATE
    ${log-lib}
    android
)