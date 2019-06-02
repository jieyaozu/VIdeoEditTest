//
// Created by jieyaozu on 2019/6/2.
//
#include "OESFilter.h"
#include "../common/AndroidLog.h"
#include "../Matrix.h"

// 将s转成字符串
#define SHADER_TO_STRING(s) #s

const std::string kGaussianPassVertexShader = SHADER_TO_STRING(

        attribute vec4 aPosition;//顶点位置
        attribute vec4 aTexCoord;//S T 纹理坐标
        varying vec2 vTexCoord;
        uniform mat4 uMatrix;
        uniform mat4 uSTMatrix;
        void main() {
            gl_Position = uMatrix*aPosition;
            vTexCoord = (uSTMatrix * aTexCoord).xy;

        }
);


const std::string kGaussianPassFragmentShader = "#extension GL_OES_EGL_image_external : require\n"\
        "precision mediump float;\n"\
        "varying vec2 vTexCoord;\n"\
        "uniform samplerExternalOES sTexture;\n"\
        "void main() {\n"\
        "gl_FragColor=texture2D(sTexture, vTexCoord);\n"\
        "}";

OESFilter::OESFilter() : program(-1), aPositionLocation(-1), aTextureCoordLocation(-1),
                         surfaceWidth(0), surfaceHeight(0) {

}

OESFilter::~OESFilter() {

}

int OESFilter::createShader(string vertex, int type) {
    const char *shaderSourceP = vertex.c_str();
    int shader = glCreateShader(type);
    if (!shader) {
        ALOGE("create shader failed\n");
    }
    glShaderSource(shader, 1, &shaderSourceP, nullptr);
    glCompileShader(shader);
    int success = 0;
    GLchar infoLog[512];
    glGetShaderiv(shader, GL_COMPILE_STATUS, &success);
    {
        if (!success) {
            glGetShaderInfoLog(shader, 512, nullptr, infoLog);
            ALOGE("compile shader failed %s \n", infoLog);
        }
    }
    return shader;
}

void OESFilter::initProgram(){
    generateProgram(kGaussianPassVertexShader.c_str(),kGaussianPassFragmentShader.c_str());
}

void OESFilter::generateProgram(const char *vertex, const char *fragment) {
    int vertexShader = createShader(vertex, GL_VERTEX_SHADER);
    int fragmentShader = createShader(fragment, GL_FRAGMENT_SHADER);

    program = glCreateProgram();
    if (!program) {
        ALOGE("create program failed\n");
    }
    glAttachShader(program, vertexShader);
    glAttachShader(program, fragmentShader);
    glLinkProgram(program);
    int success = 0;
    char info[512];
    glGetProgramiv(program, GL_LINK_STATUS, &success);
    if (!success) {
        glGetProgramInfoLog(program, 512, nullptr, info);
        ALOGE("create program failed %s \n", info);
    }

    aPositionLocation = glGetAttribLocation(program, "aPosition");
    aTextureCoordLocation = glGetAttribLocation(program, "aTexCoord");
    uMatrixLocation = glGetUniformLocation(program, "uMatrix");
    uSTMMatrixHandle = glGetUniformLocation(program, "uSTMatrix");
    uTextureSamplerLocation = glGetUniformLocation(program, "sTexture");
}

void OESFilter::updateViewPort(int width, int height) {
    glViewport(0, 0, width, height);
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT);
}

void OESFilter::setMatrix() {
    glUniformMatrix4fv(uMatrixLocation, 1, GL_FALSE, projectionMatrix);
    glUniformMatrix4fv(uSTMMatrixHandle, 1, GL_FALSE, mSTMatrix);
}

void OESFilter::bindTexture(GLuint texture) {
// 绑定纹理
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, texture);
    glUniform1i(uTextureSamplerLocation, 0);
}

void OESFilter::bindAttributes(const float *vertices, const float *textureVertices) {
    // 绑定顶点坐标
    glVertexAttribPointer(aPositionLocation, 2, GL_FLOAT, GL_FALSE, 0, vertices);
    glEnableVertexAttribArray(aPositionLocation);

    // 绑定纹理坐标
    glVertexAttribPointer(aTextureCoordLocation, 2, GL_FLOAT, GL_FALSE, 0, textureVertices);
    glEnableVertexAttribArray(aTextureCoordLocation);
}

void OESFilter::onDrawFrame() {
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
}

void OESFilter::unbindAttributes() {
    glDisableVertexAttribArray(aPositionLocation);
    glDisableVertexAttribArray(aTextureCoordLocation);
}

void OESFilter::unbindTextures() {
    glBindTexture(GL_TEXTURE_2D, 0);
}

void OESFilter::onSurfaceChanged(int width, int height) {
    surfaceWidth = width;
    surfaceHeight = height;
}

void OESFilter::setVideoSize(int videoWidth, int videoHeight) {
    // TODO
    float screenRatio = (float) surfaceWidth / surfaceHeight;
    float videoRatio = (float) videoWidth / videoHeight;
    if (videoRatio < screenRatio) {
        //fitcenter
        orthoM(projectionMatrix, 0, -1.0, 1.0, -videoRatio / screenRatio, videoRatio / screenRatio,
               -1.0, 1.0);
    } else {
        //铺满屏幕
        orthoM(projectionMatrix, 0, -screenRatio / videoRatio, screenRatio / videoRatio, -1.0, 1.0,
               -1.0, 1.0);
    }
    flip(projectionMatrix, false, true);//矩阵上下翻转
}

void OESFilter::setMSTMatrix(float matrix[]) {
    for (int i = 0; i < 16; i++) {
        mSTMatrix[i] = matrix[i];
    }
}

void OESFilter::initFrameBuffer() {
    destroyFrameBuffer();
    genFrameTexturesBuffer();
}

void OESFilter::destroyFrameBuffer() {
    glDeleteTextures(1, FBUFFERTEXTURE);
    glDeleteFramebuffers(1, FBUFFERS);
}

void OESFilter::genFrameTexturesBuffer() {
    glGenFramebuffers(1, FBUFFERS);
    glGenTextures(1, FBUFFERTEXTURE);
    for (int i = 0; i < 1; i++) {
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
        ALOGE("====native OESFilter==> %s", "Failed to create framebuffer!!!");
    } else {
        ALOGE("====native OESFilter==> %s", "create framebuffer success!!!");
    }
}

void OESFilter::drawFrame(int textureId) {
    updateViewPort(surfaceWidth, surfaceHeight);
    // 绑定program
    glUseProgram(program);
    setMatrix();
    bindTexture(textureId);
    bindAttributes(vertices, textureVetrices);
    onDrawFrame();
    unbindAttributes();
    unbindTextures();
}

int OESFilter::drawFrameBuffer(int textureId) {
    updateViewPort(surfaceWidth, surfaceHeight);
    glBindFramebuffer(GL_FRAMEBUFFER, FBUFFERS[0]);
    // 绑定program
    glUseProgram(program);
    setMatrix();
    bindTexture(textureId);
    bindAttributes(vertices, textureVetrices);
    onDrawFrame();
    unbindAttributes();
    unbindTextures();
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    return FBUFFERTEXTURE[0];
}

