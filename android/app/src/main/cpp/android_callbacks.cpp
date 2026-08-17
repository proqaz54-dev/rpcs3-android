#include "android_callbacks.hpp"

#include "Emu/System.h"
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
#include <thread>
#include <chrono>

#define LOG_TAG "RPCS3-ANDROID"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

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
			ANativeWindow* m_window = nullptr;
			int m_width = 720;
			int m_height = 1280;
			f64 m_display_rate = 60.0;

		public:
			android_gs_frame() = default;

			void set_native_window(ANativeWindow* window, int width, int height, f64 rate)
			{
				if (m_window)
				{
					ANativeWindow_release(m_window);
				}

				m_window = window;
				m_width = width;
				m_height = height;
				m_display_rate = rate;
			}

			void close() override
			{
				if (m_window)
				{
					ANativeWindow_release(m_window);
					m_window = nullptr;
				}
			}

			void reset() override {}

			bool shown() override { return m_window != nullptr; }

			void hide() override {}

			void show() override {}

			void toggle_fullscreen() override {}

			void delete_context(draw_context_t /*ctx*/) override {}

			draw_context_t make_context() override { return nullptr; }

			void set_current(draw_context_t /*ctx*/) override {}

			void flip(draw_context_t /*ctx*/, bool /*skip_frame*/) override
			{
				// Rendering happens through the Vulkan command stream;
				// the frame is presented by the swapchain on the window.
			}

			int client_width() override { return m_width; }

			int client_height() override { return m_height; }

			f64 client_display_rate() override { return m_display_rate; }

			bool has_alpha() override { return false; }

			display_handle_t handle() const override
			{
				if (!m_window)
				{
					return {};
				}

				return std::get<ANativeWindow*>(display_handle_t{m_window});
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
		// TODO: marshal to the Android main thread via a Handler.
		func();
	};

	cb.on_run = [](bool) { LOGI("on_run"); };
	cb.on_pause = [] { LOGI("on_pause"); };
	cb.on_resume = [] { LOGI("on_resume"); };
	cb.on_stop = [] { LOGI("on_stop"); };
	cb.on_ready = [] { LOGI("on_ready"); };
	cb.on_missing_fw = [] { LOGI("on_missing_fw"); };

	cb.get_gs_frame = []() -> std::unique_ptr<GSFrameBase>
	{
		return std::make_unique<frame::android_gs_frame>();
	};

	cb.get_audio = []() -> std::shared_ptr<AudioBackend>
	{
		// cubeb supports AAudio/OpenSL ES on Android.
		return std::make_shared<CubebBackend>();
	};

	cb.get_audio_enumerator = [](u64) -> std::shared_ptr<audio_device_enumerator>
	{
		return std::make_shared<cubeb_enumerator>();
	};

	cb.get_msg_dialog = []() -> std::shared_ptr<MsgDialogBase>
	{
		return nullptr; // TODO: Java dialog bridge
	};

	cb.get_osk_dialog = []() -> std::shared_ptr<OskDialogBase>
	{
		return nullptr; // TODO: Java dialog bridge
	};

	cb.get_save_dialog = []() -> std::unique_ptr<SaveDialogBase>
	{
		return nullptr; // TODO: Java dialog bridge
	};

	cb.get_sendmessage_dialog = []() -> std::shared_ptr<SendMessageDialogBase>
	{
		return nullptr; // TODO: not supported on Android
	};

	cb.get_recvmessage_dialog = []() -> std::shared_ptr<RecvMessageDialogBase>
	{
		return nullptr; // TODO: not supported on Android
	};

	cb.get_trophy_notification_dialog = []() -> std::unique_ptr<TrophyNotificationBase>
	{
		return nullptr; // TODO: not supported on Android
	};

	cb.init_kb_handler = [] {
		// No keyboard handler yet.
	};

	cb.init_mouse_handler = [] {
		// No mouse handler yet.
	};

	cb.init_pad_handler = [](std::string_view) {
		// TODO: SDL3 or native gamepad support later.
	};

	cb.get_camera_handler = []() -> std::shared_ptr<camera_handler_base>
	{
		return nullptr;
	};

	cb.get_music_handler = []() -> std::shared_ptr<music_handler_base>
	{
		return nullptr;
	};

	cb.play_sound = [](const std::string&, std::optional<f32>) {};
	cb.get_image_info = [](const std::string&, std::string&, s32&, s32&, s32&)
	{
		return false;
	};
	cb.get_scaled_image = [](const std::string&, s32, s32, s32&, s32&, u8*, bool)
	{
		return false;
	};
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
	cb.close_gs_frame = [] {};
	cb.on_emulation_stop_no_response = [](std::shared_ptr<atomic_t<bool>>, int) {};
	cb.on_save_state_progress = [](std::shared_ptr<atomic_t<bool>>, stx::shared_ptr<utils::serial>, stx::atomic_ptr<std::string>*, std::shared_ptr<void>) {};
	cb.enable_disc_eject = [](bool) {};
	cb.enable_disc_insert = [](bool) {};
	cb.on_install_pkgs = [](const std::vector<std::string>&) { return false; };
	cb.add_breakpoint = [](u32) {};
	cb.init_gs_render = [](utils::serial*) {};
	cb.get_localized_string = [](localized_string_id, const char*) { return std::string{}; };
	cb.get_localized_u32string = [](localized_string_id, const char*) { return std::u32string{}; };
	cb.get_localized_setting = [](const cfg::_base*, u32) { return std::string{}; };
	cb.get_photo_path = [](std::string_view) { return std::string{}; };

	return cb;
}