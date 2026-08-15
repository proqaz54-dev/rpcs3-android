#ifdef ANDROID
#include "stdafx.h"
#include "Input/product_info.h"

// Android stub: no gamepad product database; Android gamepads are
// handled natively.

namespace input
{
	std::vector<product_info> get_products_by_class(int /*class_id*/)
	{
		return {};
	}
}
#endif