#include <jni.h>
#include <string>
#include <android/log.h>
#include <GLES3/gl3.h>
#include <GLES2/gl2ext.h>
#include <GLES2/gl2platform.h>
#include "Matrix.h"
#include "filter/OESFilter.h"
#include "filter/ShowFilter.h"
#include "filter/BlurFilter.h"

#define TAG    "myhello-jni-test" // 这个是自定义的LOG的标识
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,TAG,__VA_ARGS__) // 定义LOGD类型
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__) // 定义LOGD类型
using namespace std;
extern "C"
{

OESFilter *oesFilter;
ShowFilter *showFilter;
BlurFilter *blurFilter;

unsigned int texture;

JNIEXPORT void JNICALL
Java_com_yaozu_videoedittest_VideoPreviewView_nativeInit(JNIEnv *env, jobject type) {

    oesFilter = new OESFilter();
    showFilter = new ShowFilter();
    blurFilter = new BlurFilter();
}

JNIEXPORT jint JNICALL
Java_com_yaozu_videoedittest_VideoPreviewView_onNativeCreate(JNIEnv *env, jobject type,
                                                             jstring vertex, jstring fragment) {

    glGenTextures(1, &texture);
    glBindTexture(GL_TEXTURE_EXTERNAL_OES, texture);
    glTexParameterf(GL_TEXTURE_EXTERNAL_OES,
                    GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameterf(GL_TEXTURE_EXTERNAL_OES,
                    GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glBindTexture(GL_TEXTURE_2D, 0);
    LOGD("##########====> %s texture=%d", "onNativeCreate", texture);
    const char *cVertex = env->GetStringUTFChars(vertex, nullptr);
    const char *cFragment = env->GetStringUTFChars(fragment, nullptr);
    oesFilter->initProgram();
    showFilter->initProgram();
    blurFilter->initProgram();
    return texture;
}

JNIEXPORT void JNICALL
Java_com_yaozu_videoedittest_VideoPreviewView_onNativeDraw(JNIEnv *env, jobject type) {
    //LOGD("##########====> %s", "onNativeDraw");
    int nextTextureid = oesFilter->drawFrameBuffer(texture);
    nextTextureid = blurFilter->drawFrameBuffer(nextTextureid);
    showFilter->drawFrame(nextTextureid);
    //oesFilter->drawFrame(texture);
}

JNIEXPORT void JNICALL
Java_com_yaozu_videoedittest_VideoPreviewView_onNativeSurfaceChanged(JNIEnv *env, jobject type,
                                                                     jint width, jint height) {

    //LOGD("##########====> %s%d%d", "onNativeSurfaceChanged", width, height);
    //width = width/2;
    //height = height/2;
    oesFilter->onSurfaceChanged(width, height);
    oesFilter->initFrameBuffer();
    showFilter->onSurfaceChanged(width, height);
    showFilter->initFrameBuffer();
    blurFilter->onSurfaceChanged(width, height);
    blurFilter->initFrameBuffer();
}

JNIEXPORT void JNICALL
Java_com_yaozu_videoedittest_VideoPreviewView_setVideoSize(JNIEnv *env, jobject type,
                                                           jint videoWidth, jint videoHeight) {

    // TODO
    oesFilter->setVideoSize(videoWidth, videoHeight);
    showFilter->setVideoSize(videoWidth, videoHeight);
    blurFilter->setVideoSize(videoWidth, videoHeight);
}

JNIEXPORT void JNICALL
Java_com_yaozu_videoedittest_VideoPreviewView_setMSTMatrix(JNIEnv *env, jobject type,
                                                           jfloatArray mSTMatrix_) {
    jfloat *sTMatrix = env->GetFloatArrayElements(mSTMatrix_, NULL);
    oesFilter->setMSTMatrix(sTMatrix);
    showFilter->setMSTMatrix(sTMatrix);
    blurFilter->setMSTMatrix(sTMatrix);
    env->ReleaseFloatArrayElements(mSTMatrix_, sTMatrix, 0);
}

}