#include "android_callbacks.hpp"

#include "Emu/System.h"
#include "Emu/Audio/audio_device_enumerator.h"
#include "Emu/GSRender.h"
#include "Emu/RSX/GSFrameBase.h"
#include "Input/pad_thread.h"

#include <android/log.h>

#define LOG_TAG "RPCS3-ANDROID"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace
{
	// The RSX frame is created over the native window provided by the
	// Kotlin/Java layer (see NativeSurfaceBridge / MainActivity).
	// Until the container is set, gameplay cannot be presented.
	namespace frame
	{
		class android_gs_frame : public GSFrameBase
		{
		public:
			android_gs_frame() = default;

			display_handle_t handle() const override
			{
				// TODO: return the ANativeWindow* supplied from Java side.
				return {};
			}

			// All rendering happens on the Vulkan command stream; these
			// hooks are only meaningful on desktop.
			void present_frame() override {}
			void flip(int /*buffer*/) override {}
			bool shown() override { return true; }
			bool visible() override { return true; }
			void hide() override {}
			void show() override {}
			void close() override {}
			void move(int /*x*/, int /*y*/) override {}
			void resize(int, int) override {}
			void swap_interval() override {}
			void set_backbuffer_scale(int, int) override {}
			void set_swapchain_size(unsigned /*w*/, unsigned /*h*/) override {}
			void take_screenshot() override {}
			void use_fences() override {}
			int get_client_width() override { return 720; }
			int get_client_height() override { return 1280; }
			u8 get_client_type() override { return 0; }
		};
	}
}

EmuCallbacks android::create_android_callbacks()
{
	EmuCallbacks cb;

	cb.call_from_main_thread = [](std::function<void()> func, void*, bool)
	{
		// TODO: marshal to the Android main thread via a Handler.
		func();
	};

	cb.on_run = [] { LOGI("on_run"); };
	cb.on_pause = [] { LOGI("on_pause"); };
	cb.on_resume = [] { LOGI("on_resume"); };
	cb.on_stop = [] { LOGI("on_stop"); };
	cb.on_ready = [] { LOGI("on_ready"); };
	cb.on_missing_fw = [] { LOGI("on_missing_fw"); };

	cb.get_gs_frame = []() -> std::shared_ptr<GSFrameBase>
	{
		return std::make_shared<frame::android_gs_frame>();
	};

	cb.get_audio = []() -> std::shared_ptr<AudioBackend>
	{
		// cubeb supports AAudio/OpenSL ES on Android.
		return std::make_shared<CubebBackend>();
	};

	cb.get_audio_enumerator = []() -> std::shared_ptr<audio_device_enumerator>
	{
		return std::make_shared<cubeb_audio_device_enumerator>();
	};

	cb.get_msg_dialog = []() -> std::shared_ptr<MsgDialogBase>
	{
		return nullptr; // TODO: Java dialog bridge
	};

	cb.get_osk_dialog = []() -> std::shared_ptr<OskDialogBase>
	{
		return nullptr; // TODO: Java dialog bridge
	};

	cb.get_save_dialog = []() -> std::shared_ptr<SaveDialogBase>
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

	cb.get_trophy_notification_dialog = []() -> std::shared_ptr<TrophyNotificationBase>
	{
		return nullptr; // TODO: not supported on Android
	};

	cb.init_kb_handler = [](std::shared_ptr<KeyboardHandlerBase>& handler, u32 max_connect)
	{
		// SDL-based keyboard or Java IME bridge comes later.
		handler = nullptr;
	};

	cb.init_mouse_handler = [](std::shared_ptr<MouseHandlerBase>& handler)
	{
		handler = nullptr;
	};

	cb.init_pad_handler = [](std::shared_ptr<PadHandlerBase>& handler)
	{
		// SDL3 supports Android gamepads/touch natively.
		handler = std::make_shared<sdl_pad_handler>();
	};

	cb.get_camera_handler = []() -> std::shared_ptr<CameraHandlerBase>
	{
		return nullptr;
	};

	cb.get_music_handler = []() -> std::shared_ptr<MusicHandlerBase>
	{
		return nullptr;
	};

	cb.play_sound = [](u32 /*channel*/, const char* /*name*/, u32 /*volume*/) {};
	cb.get_image_info = [](const std::string& /*path*/, std::string& /*name*/, std::string& /*serial*/)
	{
		return false;
	};
	cb.resolve_path = [](const std::string& path) { return path; };
	cb.get_font_dirs = [] { return std::vector<std::string>{}; };

	cb.display_sleep_control_supported = [] { return false; };
	cb.enable_display_sleep = [](bool) {};
	cb.enable_gamemode = [](bool) {};
	cb.try_to_quit = [] { LOGI("try_to_quit"); };
	cb.handle_taskbar_progress = [](s32, s32, u32) {};

	return cb;
}