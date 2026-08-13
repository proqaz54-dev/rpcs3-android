// Android implementation of EmuCallbacks - the interface between the
// Qt-free emulator core (rpcs3_emu) and the Android platform layer.
//
// This replaces the desktop implementations in rpcs3/main_application.cpp
// and rpcs3qt/ for the Android build.

#pragma once

#include "Emu/System.h"

#include <memory>

namespace android
{
	// Fills the EmuCallbacks structure with Android-compatible implementations.
	// All Qt-based facilities (dialogs, gs_frame, audio, input) are replaced
	// with Android equivalents:
	//   - gs_frame      -> ANativeWindow-based frame (see gs_frame_android)
	//   - dialogs       -> native no-op / Java-mediated implementations
	//   - audio         -> cubeb backend (supports AAudio/OpenSL ES on Android)
	//   - input         -> to be driven from Kotlin/Java touch and gamepad events
	EmuCallbacks create_android_callbacks();
}