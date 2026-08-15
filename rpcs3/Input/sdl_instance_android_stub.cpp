#ifdef ANDROID
#include "stdafx.h"
#include "Input/sdl_instance.h"

// Android stub: SDL3 events are not pumped on Android; the JNI layer
// forwards input directly to pad handlers.

sdl_instance::~sdl_instance()
{
}

sdl_instance& sdl_instance::get_instance()
{
	static sdl_instance instance;
	return instance;
}

bool sdl_instance::initialize()
{
	return false;
}

void sdl_instance::pump_events()
{
}
#endif