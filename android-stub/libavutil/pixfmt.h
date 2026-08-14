#pragma once

// Minimal stub of libavutil/pixfmt.h for Android builds without FFmpeg.
// Only the pixel formats used by the RSX blit engine are defined.

extern "C"
{
	enum AVPixelFormat
	{
		AV_PIX_FMT_NONE = -1,
		AV_PIX_FMT_ARGB = 25,
		AV_PIX_FMT_RGBA = 26,
		AV_PIX_FMT_ABGR = 29,
		AV_PIX_FMT_BGRA = 28,
		AV_PIX_FMT_RGB565BE = 31,
		AV_PIX_FMT_UYVY422 = 41,
		AV_PIX_FMT_YUV420P = 0,
		AV_PIX_FMT_RGB24 = 2,
	};
}
