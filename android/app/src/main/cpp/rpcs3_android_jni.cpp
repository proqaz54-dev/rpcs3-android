#include "rpcs3_android_jni.hpp"
#include "android_callbacks.hpp"

#include "Emu/System.h"
#include "Emu/system_config.h"
#include "Emu/system_utils.hpp"
#include "Loader/PSF.h"
#include "Loader/ISO.h"
#include "Utilities/File.h"
#include "util/sysinfo.hpp"
#include "util/logs.hpp"

#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <unistd.h>
#include <cstdio>
#include <cstring>
#include <filesystem>
#include <functional>
#include <memory>
#include <string>
#include <string_view>
#include <vector>

#include "SDL3/SDL.h"

#define LOG_TAG "RPCS3-JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern std::string g_android_executable_dir;
extern std::string g_android_config_dir;
extern std::string g_android_cache_dir;

namespace
{
	std::string get_jstring(JNIEnv* env, jstring str)
	{
		if (!str)
			return {};
		const char* chars = env->GetStringUTFChars(str, nullptr);
		if (!chars)
			return {};
		std::string result(chars);
		env->ReleaseStringUTFChars(str, chars);
		return result;
	}

	jstring make_jstring(JNIEnv* env, const std::string& str)
	{
		return env->NewStringUTF(str.c_str());
	}

	std::string escape_json(const std::string& s)
	{
		std::string out;
		out.reserve(s.size() + 16);
		for (char c : s)
		{
			switch (c)
			{
			case '"': out += "\\\""; break;
			case '\\': out += "\\\\"; break;
			case '\b': out += "\\b"; break;
			case '\f': out += "\\f"; break;
			case '\n': out += "\\n"; break;
			case '\r': out += "\\r"; break;
			case '\t': out += "\\t"; break;
			default:
				if (static_cast<unsigned char>(c) < 0x20)
				{
					char buf[8];
					snprintf(buf, sizeof(buf), "\\u%04x", c);
					out += buf;
				}
				else
				{
					out += c;
				}
				break;
			}
		}
		return out;
	}

	bool g_initialized = false;

	// Recursively locate a file by name inside an ISO image and return its
	// path (without a leading slash, compatible with iso_archive::retrieve).
	void find_iso_file(const iso_fs_node* node, const std::string& cur, std::string& out, const std::string& name)
	{
		if (!out.empty())
			return;

		if (!node->metadata.is_directory && node->metadata.name == name)
		{
			out = cur.empty() ? node->metadata.name : cur + "/" + node->metadata.name;
			return;
		}

		for (const auto& child : node->children)
		{
			const std::string child_cur = cur.empty() ? child->metadata.name : cur + "/" + child->metadata.name;
			find_iso_file(child.get(), child_cur, out, name);
		}
	}

	bool extract_iso_file(iso_archive& archive, const std::string& iso_path, const std::string& out_path)
	{
		auto file = archive.open(iso_path);
		if (!file)
			return false;

		const u64 sz = file->size();
		if (sz == 0 || sz > 16 * 1024 * 1024)
			return false;

		std::vector<u8> buf(sz);
		if (file->read(buf.data(), sz) != sz)
			return false;

		FILE* f = std::fopen(out_path.c_str(), "wb");
		if (!f)
			return false;

		const size_t written = std::fwrite(buf.data(), 1, static_cast<size_t>(sz), f);
		std::fclose(f);
		return written == static_cast<size_t>(sz);
	}
}

// Android log listener forwarding RPCS3 internal core logs to logcat
namespace
{
	class android_log_listener : public logs::listener
	{
	public:
		android_log_listener()
		{
			logs::listener::add(this);
		}

		void log(u64 /*stamp*/, const logs::message& msg, std::string_view /*prefix*/, std::string_view text) override
		{
			int prio = ANDROID_LOG_DEBUG;
			switch (static_cast<logs::level>(msg))
			{
			case logs::level::always:  prio = ANDROID_LOG_INFO; break;
			case logs::level::fatal:   prio = ANDROID_LOG_FATAL; break;
			case logs::level::error:   prio = ANDROID_LOG_ERROR; break;
			case logs::level::todo:    prio = ANDROID_LOG_WARN; break;
			case logs::level::success: prio = ANDROID_LOG_INFO; break;
			case logs::level::warning: prio = ANDROID_LOG_WARN; break;
			case logs::level::notice:  prio = ANDROID_LOG_DEBUG; break;
			case logs::level::trace:   prio = ANDROID_LOG_VERBOSE; break;
			}
			__android_log_write(prio, "RPCS3-CORE", std::string(text).c_str());
		}
	};
}

// JavaVM helper for rtmidi / SDL
extern "C" jint JNI_GetCreatedJavaVMs(JavaVM** vmBuf, jsize bufLen, jsize* nVMs)
{
	if (!vmBuf || bufLen < 1 || !nVMs)
		return JNI_ERR;

	JNIEnv* env = static_cast<JNIEnv*>(SDL_GetAndroidJNIEnv());
	if (!env)
	{
		*nVMs = 0;
		return JNI_OK;
	}

	JavaVM* vm = nullptr;
	if (env->GetJavaVM(&vm) != JNI_OK || !vm)
	{
		*nVMs = 0;
		return JNI_OK;
	}

	*nVMs = 1;
	vmBuf[0] = vm;
	return JNI_OK;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_net_rpcs3_android_RPCS3_initialize(JNIEnv* env, jclass /*clazz*/, jstring rootDir, jstring cacheDir)
{
	std::string root = get_jstring(env, rootDir);
	std::string cache = get_jstring(env, cacheDir);

	if (root.empty())
	{
		LOGE("initialize: root directory is empty");
		return JNI_FALSE;
	}

	if (!root.ends_with('/')) root += '/';
	if (cache.empty()) cache = root;
	if (!cache.ends_with('/')) cache += '/';

	g_android_executable_dir = root;
	g_android_config_dir = root + "config/";
	g_android_cache_dir = cache + "cache/";

	std::error_code ec;
	std::filesystem::create_directories(g_android_config_dir, ec);
	std::filesystem::create_directories(g_android_cache_dir, ec);
	std::filesystem::create_directories(root + "games/", ec);
	std::filesystem::create_directories(root + "dev_hdd0/game/", ec);
	std::filesystem::create_directories(root + "dev_hdd0/home/00000001/exdata/", ec);
	std::filesystem::create_directories(root + "dev_flash/", ec);

	if (!g_initialized)
	{
		g_initialized = true;

		// Attach log listener to capture RPCS3 logs in Android Logcat
		static android_log_listener s_log_listener;

		// Initialize callbacks
		Emu.SetCallbacks(android::create_android_callbacks());

		// Initialize default Vulkan settings for Android mobile
		g_cfg.video.renderer.set(video_renderer::vulkan);
		g_cfg.audio.renderer.set(audio_renderer::cubeb);

		LOGI("RPCS3 initialized successfully. Config dir: %s, Cache dir: %s",
			g_android_config_dir.c_str(), g_android_cache_dir.c_str());
	}

	return JNI_TRUE;
}

extern "C" JNIEXPORT jint JNICALL
Java_net_rpcs3_android_RPCS3_boot(JNIEnv* env, jclass /*clazz*/, jstring bootPath)
{
	std::string path = get_jstring(env, bootPath);
	while (path.ends_with('/') || path.ends_with('\\'))
	{
		path.pop_back();
	}

	if (path.empty())
	{
		LOGE("nativeBoot: path is empty");
		return -1;
	}

	LOGI("RPCS3 booting path: %s", path.c_str());
	Emu.SetForceBoot(true);

	const auto result = Emu.BootGame(path, "", false, cfg_mode::custom);
	LOGI("RPCS3 boot result: %d", static_cast<int>(result));
	return static_cast<jint>(result);
}

extern "C" JNIEXPORT jint JNICALL
Java_net_rpcs3_android_MainActivity_nativeBoot(JNIEnv* env, jobject /*thiz*/, jstring bootPath)
{
	return Java_net_rpcs3_android_RPCS3_boot(env, nullptr, bootPath);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_net_rpcs3_android_RPCS3_surfaceEvent(JNIEnv* env, jclass /*clazz*/, jobject surface, jint event)
{
	LOGI("surfaceEvent: event=%d, surface=%p", event, surface);

	if (event == 2) // Surface Destroyed
	{
		android::release_native_window();
		if (!Emu.IsStopped() && !Emu.IsPaused())
		{
			Emu.Pause();
		}
		return JNI_TRUE;
	}

	if (!surface)
	{
		LOGE("surfaceEvent: surface object is null");
		return JNI_FALSE;
	}

	ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
	if (!window)
	{
		LOGE("surfaceEvent: failed to obtain ANativeWindow from Surface");
		return JNI_FALSE;
	}

	const int width = ANativeWindow_getWidth(window);
	const int height = ANativeWindow_getHeight(window);

	android::set_native_window(window, width, height);

	if (event == 0 && Emu.IsPaused()) // Surface Created
	{
		Emu.Resume();
	}

	return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_net_rpcs3_android_RPCS3_pause(JNIEnv* /*env*/, jclass /*clazz*/)
{
	LOGI("RPCS3 pause requested");
	Emu.Pause();
}

extern "C" JNIEXPORT void JNICALL
Java_net_rpcs3_android_RPCS3_resume(JNIEnv* /*env*/, jclass /*clazz*/)
{
	LOGI("RPCS3 resume requested");
	Emu.Resume();
}

extern "C" JNIEXPORT void JNICALL
Java_net_rpcs3_android_RPCS3_stop(JNIEnv* /*env*/, jclass /*clazz*/)
{
	LOGI("RPCS3 stop / kill requested");
	Emu.Kill();
}

extern "C" JNIEXPORT jint JNICALL
Java_net_rpcs3_android_RPCS3_getState(JNIEnv* /*env*/, jclass /*clazz*/)
{
	return static_cast<jint>(Emu.GetStatus(false));
}

extern "C" JNIEXPORT jstring JNICALL
Java_net_rpcs3_android_RPCS3_getTitleId(JNIEnv* env, jclass /*clazz*/)
{
	return make_jstring(env, Emu.GetTitleID());
}

extern "C" JNIEXPORT jstring JNICALL
Java_net_rpcs3_android_RPCS3_getTitle(JNIEnv* env, jclass /*clazz*/)
{
	return make_jstring(env, Emu.GetTitle());
}

extern "C" JNIEXPORT jstring JNICALL
Java_net_rpcs3_android_RPCS3_scanGame(JNIEnv* env, jclass /*clazz*/, jstring gamePath)
{
	std::string path = get_jstring(env, gamePath);
	if (path.empty())
	{
		return make_jstring(env, "{\"valid\":false}");
	}

	std::error_code ec;
	std::filesystem::path fsPath(path);
	if (!std::filesystem::exists(fsPath, ec))
	{
		return make_jstring(env, "{\"valid\":false,\"error\":\"not_found\"}");
	}

	std::string sfo_path;
	std::string game_root = path;
	bool is_disc = false;

	if (std::filesystem::is_directory(fsPath, ec))
	{
		if (std::filesystem::exists(fsPath / "PARAM.SFO", ec))
		{
			sfo_path = (fsPath / "PARAM.SFO").string();
		}
		else if (std::filesystem::exists(fsPath / "PS3_GAME" / "PARAM.SFO", ec))
		{
			sfo_path = (fsPath / "PS3_GAME" / "PARAM.SFO").string();
			is_disc = true;
		}
		else if (std::filesystem::exists(fsPath / "USRDIR" / "PARAM.SFO", ec))
		{
			sfo_path = (fsPath / "USRDIR" / "PARAM.SFO").string();
		}
	}
	else if (path.ends_with(".sfo") || path.ends_with(".SFO"))
	{
		sfo_path = path;
	}
	else if (path.ends_with(".iso") || path.ends_with(".ISO") ||
	         path.ends_with(".mdf") || path.ends_with(".MDF") ||
	         path.ends_with(".img") || path.ends_with(".IMG"))
	{
		if (!is_iso_file(path))
		{
			return make_jstring(env, "{\"valid\":false,\"error\":\"not_iso\"}");
		}

		iso_archive archive(path);
		if (!archive.is_valid())
		{
			return make_jstring(env, "{\"valid\":false,\"error\":\"iso_parse_failed\"}");
		}

		std::string iso_sfo;
		find_iso_file(&archive.root(), "", iso_sfo, "PARAM.SFO");
		if (iso_sfo.empty())
		{
			return make_jstring(env, "{\"valid\":false,\"error\":\"no_sfo\"}");
		}

		const psf::registry sfo = archive.open_psf(iso_sfo);
		if (sfo.empty())
		{
			return make_jstring(env, "{\"valid\":false,\"error\":\"sfo_parse_failed\"}");
		}

		const std::string title_id = std::string(psf::get_string(sfo, "TITLE_ID"));
		const std::string title = std::string(psf::get_string(sfo, "TITLE"));
		const std::string category = std::string(psf::get_string(sfo, "CATEGORY"));
		const std::string app_ver = std::string(psf::get_string(sfo, "APP_VER"));

		// Extract the disc icon so it shows up in the game library
		std::string icon_path;
		std::string iso_icon;
		find_iso_file(&archive.root(), "", iso_icon, "ICON0.PNG");
		if (!iso_icon.empty())
		{
			std::string icon_dir = g_android_cache_dir + "icons/";
			std::error_code ec;
			std::filesystem::create_directories(icon_dir, ec);
			const std::string out = icon_dir + (title_id.empty() ? std::to_string(std::hash<std::string>{}(path)) : title_id) + ".png";
			if (extract_iso_file(archive, iso_icon, out))
			{
				icon_path = out;
			}
		}

		std::string json = "{"
			"\"valid\":true,"
			"\"path\":\"" + escape_json(path) + "\","
			"\"titleId\":\"" + escape_json(title_id) + "\","
			"\"title\":\"" + escape_json(title) + "\","
			"\"category\":\"" + escape_json(category) + "\","
			"\"appVersion\":\"" + escape_json(app_ver) + "\","
			"\"iconPath\":\"" + escape_json(icon_path) + "\","
			"\"isDisc\":true"
		"}";

		return make_jstring(env, json);
	}

	if (sfo_path.empty())
	{
		return make_jstring(env, "{\"valid\":false,\"error\":\"no_sfo\"}");
	}

	try
	{
		const psf::registry sfo = psf::load_object(sfo_path);
		const std::string title_id = std::string(psf::get_string(sfo, "TITLE_ID"));
		const std::string title = std::string(psf::get_string(sfo, "TITLE"));
		const std::string category = std::string(psf::get_string(sfo, "CATEGORY"));
		const std::string app_ver = std::string(psf::get_string(sfo, "APP_VER"));

		std::string icon_path;
		auto sfo_dir = std::filesystem::path(sfo_path).parent_path();
		if (std::filesystem::exists(sfo_dir / "ICON0.PNG", ec))
		{
			icon_path = (sfo_dir / "ICON0.PNG").string();
		}
		else if (std::filesystem::exists(fsPath / "ICON0.PNG", ec))
		{
			icon_path = (fsPath / "ICON0.PNG").string();
		}
		else if (std::filesystem::exists(fsPath / "PS3_GAME" / "ICON0.PNG", ec))
		{
			icon_path = (fsPath / "PS3_GAME" / "ICON0.PNG").string();
		}

		std::string json = "{"
			"\"valid\":true,"
			"\"path\":\"" + escape_json(path) + "\","
			"\"titleId\":\"" + escape_json(title_id) + "\","
			"\"title\":\"" + escape_json(title) + "\","
			"\"category\":\"" + escape_json(category) + "\","
			"\"appVersion\":\"" + escape_json(app_ver) + "\","
			"\"iconPath\":\"" + escape_json(icon_path) + "\","
			"\"isDisc\":" + (is_disc ? "true" : "false") +
		"}";

		return make_jstring(env, json);
	}
	catch (...)
	{
		return make_jstring(env, "{\"valid\":false,\"error\":\"parse_failed\"}");
	}
}

extern "C" JNIEXPORT jstring JNICALL
Java_net_rpcs3_android_RPCS3_systemInfo(JNIEnv* env, jclass /*clazz*/)
{
	std::string info;
	info += "RPCS3 Android\n";
	info += "Architecture: " + std::string(utils::get_architecture()) + "\n";
	info += "CPU: " + utils::get_system_info() + "\n";
	info += "OS: " + utils::get_OS_version_string() + "\n";
	info += "Adreno Custom Driver Support: ";
	info += (access("/dev/kgsl-3d0", F_OK) == 0) ? "Supported (Qualcomm Adreno GPU detected)" : "Not supported (Non-Qualcomm GPU)";
	return make_jstring(env, info);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_net_rpcs3_android_RPCS3_supportsCustomDriver(JNIEnv* /*env*/, jclass /*clazz*/)
{
	return access("/dev/kgsl-3d0", F_OK) == 0 ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_net_rpcs3_android_RPCS3_sendPadData(JNIEnv* /*env*/, jclass /*clazz*/,
    jint digital1, jint digital2, jint lsX, jint lsY, jint rsX, jint rsY, jint l2Axis, jint r2Axis)
{
	android::send_pad_data(digital1, digital2, lsX, lsY, rsX, rsY, l2Axis, r2Axis);
	return JNI_TRUE;
}