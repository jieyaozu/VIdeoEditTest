//
// Created by jieyaozu on 2019/6/2.
//

#ifndef VIDEOEDITTEST_OESFILTER_H
#define VIDEOEDITTEST_OESFILTER_H

#include <GLES3/gl3.h>
#include <string>

using namespace std;

class OESFilter {
public:
    OESFilter();

    virtual ~OESFilter();

    virtual int createShader(string vertex, int type);

    virtual void initProgram();

    // 初始化program
    virtual void generateProgram(const char *vertexShader, const char *fragmentShader);

    virtual void onSurfaceChanged(int width, int height);

    virtual void setVideoSize(int videoWidth, int videoHeight);

    virtual void drawFrame(int textureId);

    virtual int drawFrameBuffer(int textureId);

    virtual void setMSTMatrix(float matrix[]);

    void initFrameBuffer();

protected:

    virtual void destroyFrameBuffer();

    virtual void genFrameTexturesBuffer();

protected:
    virtual void updateViewPort(int width, int height);

    virtual void setMatrix();

    virtual void bindTexture(GLuint texture);

    virtual void bindAttributes(const float *vertices, const float *textureVertices);

    virtual void onDrawFrame();

    virtual void unbindAttributes();

    virtual void unbindTextures();

protected:
    int program;
    int aPositionLocation;     // 顶点坐标句柄
    int aTextureCoordLocation;// 纹理坐标句柄
    int uMatrixLocation;
    int uSTMMatrixHandle;
    int uTextureSamplerLocation;
    int surfaceWidth;
    int surfaceHeight;
    // 顶点坐标
    GLfloat vertices[8] = {
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, 1.0f,
            1.0f, -1.0f,
    };
// 纹理坐标
    GLfloat textureVetrices[8] = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };

    float projectionMatrix[16] = {
            1,0,0,0,
            0,1,0,0,
            0,0,1,0,
            0,0,0,1
    };
    float mSTMatrix[16];

    //离屏
    unsigned int FBUFFERS[1];
    unsigned int FBUFFERTEXTURE[1];
};

#endif //VIDEOEDITTEST_OESFILTER_H
