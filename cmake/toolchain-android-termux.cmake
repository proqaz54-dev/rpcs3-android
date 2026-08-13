# Custom Android toolchain for running from an aarch64 host (Termux)
# that has clang installed natively, using the NDK's sysroot for
# the aarch64-linux-android target.

set(CMAKE_SYSTEM_NAME Android)
set(CMAKE_SYSTEM_VERSION 29)
set(CMAKE_SYSTEM_PROCESSOR aarch64)
set(CMAKE_ANDROID_ARCH_ABI arm64-v8a)

if(NOT DEFINED NDK_ROOT)
    set(NDK_ROOT "$ENV{HOME}/android-ndk-r27c")
endif()

set(CMAKE_SYSROOT "${NDK_ROOT}/toolchains/llvm/prebuilt/linux-x86_64/sysroot")
set(CMAKE_C_COMPILER clang)
set(CMAKE_CXX_COMPILER clang++)
set(CMAKE_C_COMPILER_TARGET aarch64-linux-android29)
set(CMAKE_CXX_COMPILER_TARGET aarch64-linux-android29)

add_compile_definitions(ANDROID)

# NDK r27+ does not bundle libunwind in the sysroot; fall back to libgcc
# unwinding when linking with a host compiler.
set(CMAKE_EXE_LINKER_FLAGS_INIT "-unwindlib=libgcc")
set(CMAKE_SHARED_LINKER_FLAGS_INIT "-unwindlib=libgcc")

set(CMAKE_FIND_ROOT_PATH "${CMAKE_SYSROOT}")
set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_PACKAGE ONLY)

set(ANDROID TRUE)
set(ANDROID_ABI arm64-v8a)
set(ANDROID_PLATFORM android-29)
set(ANDROID_PLATFORM_LEVEL 29)
