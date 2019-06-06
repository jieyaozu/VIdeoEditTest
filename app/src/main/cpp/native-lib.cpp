#include <jni.h>
#include <string>
#include <android/log.h>

#define TAG    "myhello-jni-test" // 这个是自定义的LOG的标识
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,TAG,__VA_ARGS__) // 定义LOGD类型

extern "C"
{

JNIEXPORT jint JNICALL
Java_com_yaozu_videoedittest_VideoPreviewView_onNativeCreate(JNIEnv *env, jobject type) {

    LOGD("##########====> %s", "onNativeCreate");
    return 0;
}


JNIEXPORT void JNICALL
Java_com_yaozu_videoedittest_VideoPreviewView_onNativeDraw(JNIEnv *env, jobject type) {
    //LOGD("##########====> %s", "onNativeDraw");
}

JNIEXPORT void JNICALL
Java_com_yaozu_videoedittest_VideoPreviewView_onNativeSurfaceChanged(JNIEnv *env, jobject type,
                                                                     jint width, jint height) {

    LOGD("##########====> %s%d%d", "onNativeSurfaceChanged", width, height);

}

}