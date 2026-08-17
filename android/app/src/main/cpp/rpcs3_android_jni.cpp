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

	JavaVM* g_jvm = nullptr;
}

extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* /*reserved*/)
{
	g_jvm = vm;
	return JNI_VERSION_1_6;
}

// The NDK has no import for JNI_GetCreatedJavaVMs from ART; expose the
// app's single JavaVM instead (rtmidi's Android backend queries it).
extern "C" jint JNI_GetCreatedJavaVMs(JavaVM** vmBuf, jsize bufLen, jsize* nVMs)
{
	if (!vmBuf || bufLen < 1 || !nVMs)
		return JNI_ERR;

	*nVMs = g_jvm ? 1 : 0;
	vmBuf[0] = g_jvm;
	return JNI_OK;
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