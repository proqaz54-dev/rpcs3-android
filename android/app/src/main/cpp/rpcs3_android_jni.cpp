#include "rpcs3_android_jni.hpp"

#include <android/log.h>
#include <cstring>
#include <string>

#define LOG_TAG "RPCS3"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

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
}

extern "C" JNIEXPORT jint JNICALL
Java_net_rpcs3_android_MainActivity_nativeBoot(JNIEnv* env, jobject /*thiz*/, jstring bootPath)
{
	const std::string path = get_jstring(env, bootPath);
	LOGI("nativeBoot: %s", path.c_str());

	// TODO: forward boot path into the RPCS3 emulator core once
	// the Android entry point (android_callbacks / gs_frame) is wired up.
	// Emulator initialization happens here.
	return 0;
}