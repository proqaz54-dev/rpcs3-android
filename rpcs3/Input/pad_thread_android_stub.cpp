#ifdef ANDROID
#include "stdafx.h"
#include "Input/pad_thread.h"

// Android stub: no native pad thread. Returns null handle like an
// uninitalized host; games without PAD requirement still boot.

namespace pad
{
	atomic_t<pad_thread*> g_pad_thread;
	shared_mutex g_pad_mutex;
	std::string g_title_id;
	atomic_t<bool> g_enabled;
	atomic_t<bool> g_reset;
	atomic_t<bool> g_started;
	atomic_t<bool> g_home_menu_requested;
}
#endif