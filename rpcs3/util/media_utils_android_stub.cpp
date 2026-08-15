#ifdef ANDROID
#include "stdafx.h"
#include "media_utils.h"

// Android stub for ffmpeg-dependent media utilities.
// PS3 firmware media metadata is not needed on Android.

namespace utils
{
	std::pair<bool, media_info> get_media_info(const std::string& path, s32 av_media_type)
	{
		media_info info{};
		info.path = path;
		return { false, std::move(info) };
	}

	template <>
	std::string media_info::get_metadata(const std::string& key, const std::string& def) const
	{
		if (metadata.contains(key))
		{
			return ::at32(metadata, key);
		}

		return def;
	}

	template <>
	s64 media_info::get_metadata(const std::string& key, const s64& def) const
	{
		return def;
	}
}
#endif