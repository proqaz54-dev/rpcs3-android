#pragma once

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

// Legacy / Direct MainActivity entry point
JNIEXPORT jint JNICALL
Java_net_rpcs3_android_MainActivity_nativeBoot(JNIEnv* env, jobject thiz, jstring bootPath);

// Core RPCS3 JNI API
JNIEXPORT jboolean JNICALL
Java_net_rpcs3_android_RPCS3_initialize(JNIEnv* env, jclass clazz, jstring rootDir, jstring cacheDir);

JNIEXPORT jint JNICALL
Java_net_rpcs3_android_RPCS3_boot(JNIEnv* env, jclass clazz, jstring bootPath);

JNIEXPORT jboolean JNICALL
Java_net_rpcs3_android_RPCS3_surfaceEvent(JNIEnv* env, jclass clazz, jobject surface, jint event);

JNIEXPORT void JNICALL
Java_net_rpcs3_android_RPCS3_pause(JNIEnv* env, jclass clazz);

JNIEXPORT void JNICALL
Java_net_rpcs3_android_RPCS3_resume(JNIEnv* env, jclass clazz);

JNIEXPORT void JNICALL
Java_net_rpcs3_android_RPCS3_stop(JNIEnv* env, jclass clazz);

JNIEXPORT jint JNICALL
Java_net_rpcs3_android_RPCS3_getState(JNIEnv* env, jclass clazz);

JNIEXPORT jstring JNICALL
Java_net_rpcs3_android_RPCS3_getTitleId(JNIEnv* env, jclass clazz);

JNIEXPORT jstring JNICALL
Java_net_rpcs3_android_RPCS3_getTitle(JNIEnv* env, jclass clazz);

JNIEXPORT jstring JNICALL
Java_net_rpcs3_android_RPCS3_scanGame(JNIEnv* env, jclass clazz, jstring gamePath);

JNIEXPORT jstring JNICALL
Java_net_rpcs3_android_RPCS3_systemInfo(JNIEnv* env, jclass clazz);

JNIEXPORT jboolean JNICALL
Java_net_rpcs3_android_RPCS3_supportsCustomDriver(JNIEnv* env, jclass clazz);

JNIEXPORT jboolean JNICALL
Java_net_rpcs3_android_RPCS3_sendPadData(JNIEnv* env, jclass clazz,
    jint digital1, jint digital2, jint lsX, jint lsY, jint rsX, jint rsY, jint l2Axis, jint r2Axis);

#ifdef __cplusplus
}
#endif