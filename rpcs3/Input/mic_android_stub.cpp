#ifdef ANDROID
#include "stdafx.h"
#include "Emu/Cell/Modules/cellMic.h"

// Android stub: microphone support requires OpenAL; not available.
// All mic calls are no-ops so cellMic modules still link.

microphone_device::microphone_device(microphone_handler type)
{
	device_type = type;
}

error_code microphone_device::open_microphone(const u8 /*type*/, const u32 /*dsp_r*/, const u32 /*raw_r*/, const u8 /*channels*/)
{
	return CELL_OK;
}

error_code microphone_device::start_microphone()
{
	return CELL_OK;
}

void mic_context::operator()()
{
}

void mic_context::load_config_and_init()
{
}

bool mic_context::check_device(u32 /*dev_num*/)
{
	return false;
}

error_code microphone_device::stop_microphone()
{
	return CELL_OK;
}

error_code microphone_device::close_microphone()
{
	return CELL_OK;
}
#endif