#pragma once

#include "Emu/System.h"
#include <android/native_window.h>
#include <memory>
#include <string_view>

namespace android
{
	// Sets the active ANativeWindow for rendering
	void set_native_window(ANativeWindow* window, int width, int height, f64 refresh_rate = 60.0);

	// Releases the current ANativeWindow reference
	void release_native_window();

	// Gets the current ANativeWindow (acquired reference)
	ANativeWindow* get_native_window();

	// Updates window dimensions
	void set_window_size(int width, int height);

	// Injects gamepad / on-screen virtual pad inputs
	void send_pad_data(int digital1, int digital2, int lsX, int lsY, int rsX, int rsY, int l2Axis, int r2Axis);

	// Creates the EmuCallbacks structure wired to Android platform implementations
	EmuCallbacks create_android_callbacks();
}