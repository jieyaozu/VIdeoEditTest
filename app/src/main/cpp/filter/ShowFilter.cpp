//
// Created by jieyaozu on 2019/6/2.
//

#include "ShowFilter.h"

// 将s转成字符串
#define SHADER_TO_STRING(s) #s

const std::string kGaussianPassVertexShader = SHADER_TO_STRING(

        attribute vec4 aPosition;
        attribute vec4 aTexCoord;
        uniform mat4 uMatrix;
        uniform mat4 uSTMatrix;

        varying vec2 vTexCoord;

        void main(){
            gl_Position = uMatrix*aPosition;
            vTexCoord = (uSTMatrix*aTexCoord).xy;
        }
);


const std::string kGaussianPassFragmentShader = "#extension GL_OES_EGL_image_external : require\n"\
        "precision mediump float;\n"\
        "varying vec2 vTexCoord;\n"\
        "uniform sampler2D sTexture;\n"\
        "void main() {\n"\
        "gl_FragColor=texture2D(sTexture, vTexCoord);\n"\
        "}";




ShowFilter::ShowFilter(){
    OESFilter();
}

ShowFilter::~ShowFilter(){

}

void ShowFilter::initProgram(){
    generateProgram(kGaussianPassVertexShader.c_str(),kGaussianPassFragmentShader.c_str());
}

void ShowFilter::setVideoSize(int videoWidth, int videoHeight){
    surfaceWidth = videoWidth;
    surfaceHeight = videoHeight;
}