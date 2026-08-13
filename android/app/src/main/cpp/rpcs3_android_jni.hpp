#ifndef RPCS3_ANDROID_MAIN_HPP
#define RPCS3_ANDROID_MAIN_HPP

#include <jni.h>

extern "C"
{
    JNIEXPORT jint JNICALL
    Java_net_rpcs3_android_MainActivity_nativeBoot(JNIEnv* env, jobject thiz, jstring bootPath);
}

#endif