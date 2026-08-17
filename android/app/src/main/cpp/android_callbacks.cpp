#include "android_callbacks.hpp"

#include "Emu/System.h"
#include "Emu/system_utils.hpp"
#include "Emu/Audio/audio_device_enumerator.h"
#include "Emu/Audio/Cubeb/CubebBackend.h"
#include "Emu/Audio/Cubeb/cubeb_enumerator.h"
#include "Emu/RSX/GSRender.h"
#include "Emu/RSX/GSFrameBase.h"
#include "Emu/Cell/Modules/cellMsgDialog.h"
#include "Emu/Cell/Modules/cellOskDialog.h"
#include "Emu/Cell/Modules/cellSaveData.h"
#include "Emu/Cell/Modules/sceNp.h"
#include "Emu/Cell/Modules/sceNpTrophy.h"
#include "util/video_source.h"

#include <android/log.h>
#include <android/native_window.h>
#include <atomic>
#include <chrono>
#include <mutex>
#include <thread>

#define LOG_TAG "RPCS3-ANDROID"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace
{
	std::mutex g_window_mutex;
	ANativeWindow* g_native_window = nullptr;
	std::atomic<int> g_window_width{1280};
	std::atomic<int> g_window_height{720};
	std::atomic<f64> g_window_rate{60.0};
}

namespace android
{
	void set_native_window(ANativeWindow* window, int width, int height, f64 refresh_rate)
	{
		std::lock_guard lock(g_window_mutex);
		if (g_native_window && g_native_window != window)
		{
			ANativeWindow_release(g_native_window);
		}

		g_native_window = window;
		if (g_native_window)
		{
			ANativeWindow_acquire(g_native_window);
			if (width > 0 && height > 0)
			{
				g_window_width = width;
				g_window_height = height;
			}
			else
			{
				g_window_width = ANativeWindow_getWidth(g_native_window);
				g_window_height = ANativeWindow_getHeight(g_native_window);
			}
			g_window_rate = refresh_rate;
			LOGI("Native window set: %dx%d @ %.1f Hz", g_window_width.load(), g_window_height.load(), refresh_rate);
		}
	}

	void release_native_window()
	{
		std::lock_guard lock(g_window_mutex);
		if (g_native_window)
		{
			ANativeWindow_release(g_native_window);
			g_native_window = nullptr;
			LOGI("Native window released");
		}
	}

	ANativeWindow* get_native_window()
	{
		std::lock_guard lock(g_window_mutex);
		return g_native_window;
	}

	void set_window_size(int width, int height)
	{
		if (width > 0 && height > 0)
		{
			g_window_width = width;
			g_window_height = height;
		}
	}

	void send_pad_data(int /*digital1*/, int /*digital2*/, int /*lsX*/, int /*lsY*/, int /*rsX*/, int /*rsY*/, int /*l2Axis*/, int /*r2Axis*/)
	{
		// Routed through SDL or pad_thread
	}
}

// Qt-free replacements for functions normally provided by the Qt layer.
[[noreturn]] void report_fatal_error(std::string_view text, bool /*is_html*/, bool /*include_help_text*/)
{
	LOGE("%.*s", static_cast<int>(text.size()), text.data());
	std::abort();
}

void qt_events_aware_op(int repeat_duration_ms, std::function<bool()> wrapped_op)
{
	ensure(wrapped_op);

	while (!wrapped_op())
	{
		if (repeat_duration_ms == 0)
		{
			std::this_thread::yield();
		}
		else
		{
			std::this_thread::sleep_for(std::chrono::milliseconds(repeat_duration_ms));
		}
	}
}

namespace
{
	namespace frame
	{
		class android_gs_frame : public GSFrameBase
		{
		public:
			android_gs_frame() = default;

			~android_gs_frame() override = default;

			void close() override
			{
				android::release_native_window();
			}

			void reset() override {}

			bool shown() override
			{
				return android::get_native_window() != nullptr;
			}

			void hide() override {}

			void show() override {}

			void toggle_fullscreen() override {}

			void delete_context(draw_context_t /*ctx*/) override {}

			draw_context_t make_context() override { return nullptr; }

			void set_current(draw_context_t /*ctx*/) override {}

			void flip(draw_context_t /*ctx*/, bool /*skip_frame*/) override
			{
				// Rendering happens via Vulkan swapchain presentation
			}

			int client_width() override
			{
				return g_window_width.load();
			}

			int client_height() override
			{
				return g_window_height.load();
			}

			f64 client_display_rate() override
			{
				return g_window_rate.load();
			}

			bool has_alpha() override { return false; }

			display_handle_t handle() const override
			{
				// Wait briefly for native window to become available during startup
				for (int i = 0; i < 50; ++i)
				{
					ANativeWindow* win = android::get_native_window();
					if (win)
					{
						return std::get<ANativeWindow*>(display_handle_t{win});
					}
					if (Emu.IsStopped())
					{
						break;
					}
					std::this_thread::sleep_for(std::chrono::milliseconds(50));
				}

				ANativeWindow* win = android::get_native_window();
				if (!win)
				{
					LOGW("android_gs_frame::handle(): native window is null");
					return {};
				}

				return std::get<ANativeWindow*>(display_handle_t{win});
			}

			bool can_consume_frame() const override { return false; }

			void present_frame(std::vector<u8>&& /*data*/, u32 /*pitch*/, u32 /*width*/, u32 /*height*/, bool /*is_bgra*/) const override {}

			void take_screenshot(std::vector<u8>&& /*sshot_data*/, u32 /*sshot_width*/, u32 /*sshot_height*/, bool /*is_bgra*/) override {}

			void update_title(double /*fps*/) override {}
		};
	}
}

EmuCallbacks android::create_android_callbacks()
{
	EmuCallbacks cb;

	cb.call_from_main_thread = [](std::function<void()> func, atomic_t<u32>*)
	{
		if (func)
		{
			func();
		}
	};

	cb.on_run = [](bool) { LOGI("RPCS3 core: on_run"); };
	cb.on_pause = [] { LOGI("RPCS3 core: on_pause"); };
	cb.on_resume = [] { LOGI("RPCS3 core: on_resume"); };
	cb.on_stop = [] { LOGI("RPCS3 core: on_stop"); };
	cb.on_ready = [] { LOGI("RPCS3 core: on_ready"); };
	cb.on_missing_fw = [] { LOGW("RPCS3 core: firmware missing"); };

	cb.get_gs_frame = []() -> std::unique_ptr<GSFrameBase>
	{
		return std::make_unique<frame::android_gs_frame>();
	};

	cb.get_audio = []() -> std::shared_ptr<AudioBackend>
	{
		return std::make_shared<CubebBackend>();
	};

	cb.get_audio_enumerator = [](u64) -> std::shared_ptr<audio_device_enumerator>
	{
		return std::make_shared<cubeb_enumerator>();
	};

	cb.get_msg_dialog = []() -> std::shared_ptr<MsgDialogBase>
	{
		return nullptr;
	};

	cb.get_osk_dialog = []() -> std::shared_ptr<OskDialogBase>
	{
		return nullptr;
	};

	cb.get_save_dialog = []() -> std::unique_ptr<SaveDialogBase>
	{
		return nullptr;
	};

	cb.get_sendmessage_dialog = []() -> std::shared_ptr<SendMessageDialogBase>
	{
		return nullptr;
	};

	cb.get_recvmessage_dialog = []() -> std::shared_ptr<RecvMessageDialogBase>
	{
		return nullptr;
	};

	cb.get_trophy_notification_dialog = []() -> std::unique_ptr<TrophyNotificationBase>
	{
		return nullptr;
	};

	cb.init_kb_handler = [] {};
	cb.init_mouse_handler = [] {};
	cb.init_pad_handler = [](std::string_view) {};

	cb.get_camera_handler = []() -> std::shared_ptr<camera_handler_base>
	{
		return nullptr;
	};

	cb.get_music_handler = []() -> std::shared_ptr<music_handler_base>
	{
		return nullptr;
	};

	cb.play_sound = [](const std::string&, std::optional<f32>) {};
	cb.get_image_info = [](const std::string&, std::string&, s32&, s32&, s32&) { return false; };
	cb.get_scaled_image = [](const std::string&, s32, s32, s32&, s32&, u8*, bool) { return false; };
	cb.resolve_path = [](std::string_view path) { return std::string{path}; };
	cb.resolve_path_may_not_exist = [](std::string_view path) { return std::string{path}; };
	cb.get_font_dirs = [] { return std::vector<std::string>{}; };

	cb.display_sleep_control_supported = [] { return false; };
	cb.enable_display_sleep = [](bool) {};
	cb.enable_gamemode = [](bool) {};
	cb.try_to_quit = [](bool, std::function<void()>) { return true; };
	cb.handle_taskbar_progress = [](s32, s32) {};
	cb.check_microphone_permissions = [] {};
	cb.make_video_source = []() -> std::unique_ptr<video_source> { return nullptr; };
	cb.get_database_config = [](const std::string&) { return std::string{}; };

	cb.update_emu_settings = [] {};
	cb.save_emu_settings = [] {};
	cb.close_gs_frame = []
	{
		android::release_native_window();
	};
	cb.on_emulation_stop_no_response = [](std::shared_ptr<atomic_t<bool>>, int) {};
	cb.on_save_state_progress = [](std::shared_ptr<atomic_t<bool>>, stx::shared_ptr<utils::serial>, stx::atomic_ptr<std::string>*, std::shared_ptr<void>) {};
	cb.enable_disc_eject = [](bool) {};
	cb.enable_disc_insert = [](bool) {};
	cb.on_install_pkgs = [](const std::vector<std::string>& pkgs)
	{
		bool ok = true;
		for (const std::string& pkg : pkgs)
		{
			LOGI("Installing package: %s", pkg.c_str());
			if (!rpcs3::utils::install_pkg(pkg))
			{
				LOGE("Failed to install package: %s", pkg.c_str());
				ok = false;
			}
		}
		return ok;
	};
	cb.add_breakpoint = [](u32) {};
	cb.init_gs_render = [](utils::serial*) {};
	cb.get_localized_string = [](localized_string_id, const char*) { return std::string{}; };
	cb.get_localized_u32string = [](localized_string_id, const char*) { return std::u32string{}; };
	cb.get_localized_setting = [](const cfg::_base*, u32) { return std::string{}; };
	cb.get_photo_path = [](std::string_view) { return std::string{}; };

	return cb;
}