# Android entry point for RPCS3.
# Builds the JNI bridge and Android callbacks, linked against the
# core RPCS3 static library built by the parent CMake tree.

cmake_minimum_required(VERSION 3.22.1)
project(rpcs3_android LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(NOT DEFINED RPCS3_BUILD_DIR)
    set(RPCS3_BUILD_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../build-android")
endif()

add_library(rpcs3_android SHARED
    rpcs3_android_jni.cpp
    android_callbacks.cpp
)

target_include_directories(rpcs3_android PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../rpcs3
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../..
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../3rdparty
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../3rdparty/ffmpeg/android-stub
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../android-stub
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../3rdparty/cubeb/cubeb/include
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../../../3rdparty/cubeb/extra
)

target_compile_definitions(rpcs3_android PRIVATE
    ANDROID
    _CRT_SECURE_NO_WARNINGS
)

file(GLOB_RECURSE RPCS3_CORE_LIBS
    "${RPCS3_BUILD_DIR}/rpcs3/Emu/librpcs3_emu.a"
    "${RPCS3_BUILD_DIR}/3rdparty/*/*.a"
    "${RPCS3_BUILD_DIR}/3rdparty/*/*/*.a"
    "${RPCS3_BUILD_DIR}/3rdparty/*/*/*/*.a"
    "${RPCS3_BUILD_DIR}/_deps/*/*/*.a"
    "${RPCS3_BUILD_DIR}/_deps/*/*/*/*.a"
    "${RPCS3_BUILD_DIR}/_deps/*/*/*/*/*.a"
)

target_link_libraries(rpcs3_android PRIVATE
    ${RPCS3_CORE_LIBS}
    log
    android
)