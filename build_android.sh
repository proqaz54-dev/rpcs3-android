#!/bin/bash
# Cross-compile RPCS3 emulator core (rpcs3_emu) for Android arm64-v8a.
# Usage: ./build_android.sh [--clean]
set -e

NDK_ROOT="${NDK_ROOT:-$HOME/android-ndk-r27c}"
BUILD_DIR="${BUILD_DIR:-build-android}"
TOOLCHAIN="$NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64"

if [ ! -d "$TOOLCHAIN" ]; then
    echo "ERROR: NDK toolchain not found at $TOOLCHAIN" >&2
    exit 1
fi

cd "$(dirname "$0")"

if [ "$1" == "--clean" ]; then
    rm -rf "$BUILD_DIR"
fi

# Detect whether we run on a native x86_64 host (use NDK toolchain)
# or on an ARM64 host such as Termux (use host clang + NDK sysroot).
if [ "$(uname -m)" = "aarch64" ]; then
    TOOLCHAIN_FILE="cmake/toolchain-android-termux.cmake"
else
    TOOLCHAIN_FILE="$TOOLCHAIN/../..//build/cmake/android.toolchain.cmake"
fi

echo "Using toolchain: $TOOLCHAIN_FILE"

cmake -S . -B "$BUILD_DIR" \
    -DCMAKE_TOOLCHAIN_FILE="$TOOLCHAIN_FILE" \
    -DNDK_ROOT="$NDK_ROOT" \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-29 \
    -DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_CXX_STANDARD=23 \
    -DUSE_NATIVE_INSTRUCTIONS=OFF \
    -DUSE_PRECOMPILED_HEADERS=OFF \
    -DUSE_SYSTEM_FFMPEG=OFF \
    -DUSE_SYSTEM_LIBS=OFF \
    -DUSE_SYSTEM_SDL=OFF \
    -DUSE_SYSTEM_CURL=OFF \
    -DUSE_FAUDIO=OFF \
    -DBUILD_LLVM=ON \
    "$@"

cmake --build "$BUILD_DIR" -j"$(nproc)"
