#!/usr/bin/env bash
set -euo pipefail

# Builds FFmpeg (static) for Android arm64 from source with the same
# feature set RPCS3 uses, and installs it into build-android/3rdparty/ffmpeg.

NDK="${ANDROID_NDK_HOME:?ANDROID_NDK_HOME must be set}"
OUT_DIR="${1:-build-android/3rdparty/ffmpeg}"
SOURCE_TARBALL="https://github.com/FFmpeg/FFmpeg/archive/refs/tags/n8.1.1.tar.gz"
WORK_DIR="${HOME:-/tmp}/ffmpeg-build"
JOBS="$(nproc)"

TC="$NDK/toolchains/llvm/prebuilt/linux-x86_64"
CC="$TC/bin/aarch64-linux-android29-clang"
AR="$TC/bin/llvm-ar"
RANLIB="$TC/bin/llvm-ranlib"
STRIP="$TC/bin/llvm-strip"
NM="$TC/bin/llvm-nm"

export CFLAGS="-fPIC"
export CXXFLAGS="-fPIC"

mkdir -p "$WORK_DIR"
if [ ! -d "$WORK_DIR/FFmpeg-n8.1.1" ]; then
  curl -sSL "$SOURCE_TARBALL" -o "$WORK_DIR/ffmpeg.tar.gz"
  tar xzf "$WORK_DIR/ffmpeg.tar.gz" -C "$WORK_DIR"
fi

mkdir -p "$WORK_DIR/build-arm64-pic"
cd "$WORK_DIR/build-arm64-pic"

configure_ffmpeg() {
  ../FFmpeg-n8.1.1/configure \
    --prefix="$WORK_DIR/install-arm64" \
    --target-os=android \
    --arch=aarch64 \
    --cpu=armv8-a \
    --enable-cross-compile \
    --extra-cflags=-fPIC \
    --cc="$CC" \
    --cxx="${CC%clang}clang++" \
    --ar="$AR" --ranlib="$RANLIB" --strip="$STRIP" --nm="$NM" \
    --enable-pic --enable-pthreads --enable-static --disable-shared \
    --disable-doc --disable-autodetect --disable-network \
    --disable-everything \
    --enable-decoder=aac --enable-decoder=aac_latm --enable-decoder=atrac3 --enable-decoder=atrac3p --enable-decoder=atrac9 --enable-decoder=mp3 --enable-decoder=pcm_s16le --enable-decoder=pcm_s8 \
    --enable-decoder=h264 --enable-decoder=mpeg4 --enable-decoder=mpeg2video --enable-decoder=mjpeg --enable-decoder=mjpegb \
    --enable-encoder=pcm_s16le --enable-encoder=mp3 --enable-encoder=ac3 --enable-encoder=aac \
    --enable-encoder=ffv1 --enable-encoder=mpeg4 --enable-encoder=mjpeg --enable-encoder=h264 \
    --enable-muxer=avi --enable-muxer=h264 --enable-muxer=mjpeg --enable-muxer=mp4 \
    --enable-demuxer=h264 --enable-demuxer=m4v --enable-demuxer=mp3 --enable-demuxer=mpegvideo --enable-demuxer=mpegps --enable-demuxer=mjpeg --enable-demuxer=mov \
    --enable-demuxer=avi --enable-demuxer=aac --enable-demuxer=pmp --enable-demuxer=oma --enable-demuxer=pcm_s16le --enable-demuxer=pcm_s8 --enable-demuxer=wav \
    --enable-parser=h264 --enable-parser=mpeg4video --enable-parser=mpegaudio --enable-parser=mpegvideo --enable-parser=mjpeg --enable-parser=aac --enable-parser=aac_latm \
    --enable-protocol=file \
    --enable-bsf=mjpeg2jpeg
}

if [ ! -f config.mak ]; then
  configure_ffmpeg
else
  # Cached builds may predate the -fPIC requirement; rebuild in that case.
  if ! grep -q -- "-fPIC" config.mak; then
    echo "Existing FFmpeg build lacks -fPIC, rebuilding..."
    find . -mindepth 1 -delete
    configure_ffmpeg
  fi
fi

# CFLAGS must contain -fPIC for use inside a shared library.
if ! grep -q -- "-fPIC" config.mak; then
  echo "Forcing -fPIC into config.mak CFLAGS and rebuilding objects..."
  sed -i 's/^CFLAGS=/CFLAGS=-fPIC /' config.mak
  find . -name '*.o' -delete
fi
grep "^CFLAGS=" config.mak

make -j"$JOBS"

mkdir -p "$OUT_DIR/lib"
cp -f libavformat/libavformat.a \
      libavcodec/libavcodec.a \
      libavfilter/libavfilter.a \
      libavutil/libavutil.a \
      libswscale/libswscale.a \
      libswresample/libswresample.a \
      "$OUT_DIR/lib/"

echo "FFmpeg installed to $OUT_DIR/lib"
ls -la "$OUT_DIR/lib/"