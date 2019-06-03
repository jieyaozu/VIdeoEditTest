//
// Created by jieyaozu on 2019/6/3.
//

#include "BlurFilter.h"
#include "../common/AndroidLog.h"

// 将s转成字符串
#define SHADER_TO_STRING(s) #s

const std::string kGaussianPassVertexShader = SHADER_TO_STRING(

        attribute
        vec4 aPosition;//顶点位置
        attribute
        vec4 aTexCoord;//S T 纹理坐标
        varying
        vec2 vTexCoord;
        uniform
        mat4 uMatrix;
        uniform
        mat4 uSTMatrix;

        // 高斯算子左右偏移值，当偏移值为2时，高斯算子为5 x 5
        const int SHIFT_SIZE = 15;
        uniform
        highp float texelWidthOffset;
        uniform
        highp float texelHeightOffset;

        varying
        vec4 blurShiftCoordinates[SHIFT_SIZE];

        void main() {
            gl_Position = uMatrix * aPosition;
            vTexCoord = (uSTMatrix * aTexCoord).xy;

            // 偏移步距
            vec2 singleStepOffset = vec2(texelWidthOffset, texelHeightOffset);
            // 记录偏移坐标
            for (int i = 0; i < SHIFT_SIZE; i++) {
                blurShiftCoordinates[i] = vec4(vTexCoord.xy - float(i + 1) * singleStepOffset,
                                               vTexCoord.xy + float(i + 1) * singleStepOffset);
            }
        }
);

const std::string kGaussianPassFragmentShader = "#extension GL_OES_EGL_image_external : require\n"
        "precision mediump float;\n"
        "varying vec2 vTexCoord;\n"
        "uniform sampler2D sTexture;\n"
        "uniform bool isVertical;\n"
        "// 高斯算子左右偏移值，当偏移值为2时，高斯算子为5 x 5\n"
        "const int SHIFT_SIZE = 15;\n"
        "varying vec4 blurShiftCoordinates[SHIFT_SIZE];\n"
        "void main() {\n"
        "\n"
        "        vec4 centralColor = texture2D(sTexture, vTexCoord);\n"
        "        mediump vec3 sum = centralColor.rgb;\n"
        "        // 计算偏移坐标的颜色值总和\n"
        "        for (int i = 0; i < SHIFT_SIZE; i++) {\n"
        "            sum += texture2D(sTexture, blurShiftCoordinates[i].xy).rgb;\n"
        "            sum += texture2D(sTexture, blurShiftCoordinates[i].zw).rgb;\n"
        "        }\n"
        "\n"
        "        // 求出平均值\n"
        "        gl_FragColor = vec4(sum * 1.0 / float(2 * SHIFT_SIZE + 1), centralColor.a);\n"
        "}";


BlurFilter::BlurFilter() {
    OESFilter();
}

BlurFilter::~BlurFilter() {

}

void BlurFilter::initProgram() {
    generateProgram(kGaussianPassVertexShader.c_str(), kGaussianPassFragmentShader.c_str());
    texelWidthOffsetHandle = glGetUniformLocation(program, "texelWidthOffset");
    texelHeightOffsetHandle = glGetUniformLocation(program, "texelHeightOffset");
}

void BlurFilter::setVideoSize(int videoWidth, int videoHeight) {
    surfaceWidth = videoWidth;
    surfaceHeight = videoHeight;
}

void BlurFilter::destroyFrameBuffer() {
    glDeleteTextures(2, FBUFFERTEXTURE);
    glDeleteFramebuffers(1, FBUFFERS);
}

void BlurFilter::genFrameTexturesBuffer() {
    glGenFramebuffers(1, FBUFFERS);
    glGenTextures(2, FBUFFERTEXTURE);
    for (int i = 0; i < 2; i++) {
        glBindFramebuffer(GL_FRAMEBUFFER, FBUFFERS[0]);
        glBindTexture(GL_TEXTURE_2D, FBUFFERTEXTURE[i]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, surfaceWidth, surfaceHeight,
                     0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                               GL_TEXTURE_2D, FBUFFERTEXTURE[i], 0);
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
    if (status != GL_FRAMEBUFFER_COMPLETE) {
        ALOGE("====BlurFilter OESFilter==> %s", "Failed to create framebuffer!!!");
    } else {
        ALOGE("====BlurFilter OESFilter==> %s", "create framebuffer success!!!");
    }
}

int BlurFilter::drawFrameBuffer(int textureId) {
    updateViewPort(surfaceWidth, surfaceHeight);
    // 绑定program
    glUseProgram(program);
    isVertical = true;
    bool first = true;
    for (int i = 0; i < 8; i++) {
        index = i;
        glBindFramebuffer(GL_FRAMEBUFFER, FBUFFERS[0]);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,
                               GL_TEXTURE_2D, FBUFFERTEXTURE[index % 2], 0);
        if (isVertical) {
            glUniform1f(texelWidthOffsetHandle, 0);
            glUniform1f(texelHeightOffsetHandle, 18.0 / surfaceHeight);
        } else {
            glUniform1f(texelWidthOffsetHandle, 18.0 / surfaceWidth);
            glUniform1f(texelHeightOffsetHandle, 0);
        }

        if (first) {
            bindTexture(textureId);
        } else {
            bindTexture(FBUFFERTEXTURE[(index - 1) % 2]);
        }
        setMatrix();
        bindAttributes(vertices, textureVetrices);
        onDrawFrame();
        unbindAttributes();
        unbindTextures();
        first = false;
        isVertical = !isVertical;
    }
    glBindFramebuffer(GL_FRAMEBUFFER, 0);

    return FBUFFERTEXTURE[index % 2];
}

