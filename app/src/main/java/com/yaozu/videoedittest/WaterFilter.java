package com.yaozu.videoedittest;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class WaterFilter {
    private int programId;
    private FloatBuffer vertexBuffer;
    private FloatBuffer textureVertexBuffer;
    //顶点坐标
    private float vertexData[] = {
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, 1.0f,
            1.0f, -1.0f,
    };

    //纹理坐标
    private float[] textureVertexData = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };

    private final float[] projectionMatrix = new float[16];
    private float[] matrix = new float[16];
    private float[] mViewMatrix = new float[16];

    private int textureId;

    private int uTextureSamplerLocation;
    /**
     * 纹理坐标句柄
     */
    private int aTextureCoordLocation;
    private int uSTMMatrixHandle;
    private int aPositionLocation;
    private int uMatrixLocation;

    private int viewWidth;
    private int viewHeight;

    private int videoWidth, videoHeight;

    public WaterFilter() {
        initBuffer();
    }

    public float[] getMatrix() {
        return matrix;
    }

    /**
     * Buffer初始化
     */
    protected void initBuffer() {
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);

        textureVertexBuffer = ByteBuffer.allocateDirect(textureVertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureVertexData);
        textureVertexBuffer.position(0);
    }

    public final void create(Context context) {
        String vertexShader = ShaderUtils.readRawTextFile(context, R.raw.base_vertex);
        String fragmentShader = ShaderUtils.readRawTextFile(context, R.raw.base_fragment);
        programId = ShaderUtils.uCreateGlProgram(vertexShader, fragmentShader);
        aPositionLocation = GLES20.glGetAttribLocation(programId, "aPosition");
        aTextureCoordLocation = GLES20.glGetAttribLocation(programId, "aTexCoord");
        uMatrixLocation = GLES20.glGetUniformLocation(programId, "vMatrix");
        uTextureSamplerLocation = GLES20.glGetUniformLocation(programId, "vTexture");
    }

    public void draw() {
        createTexture();
        GLES20.glViewport(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        //GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(programId);
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glUniform1i(uTextureSamplerLocation, 0);
        GLES20.glEnableVertexAttribArray(aPositionLocation);
        GLES20.glVertexAttribPointer(aPositionLocation, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(aTextureCoordLocation);
        GLES20.glVertexAttribPointer(aTextureCoordLocation, 2, GLES20.GL_FLOAT, false, 0, textureVertexBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(aPositionLocation);
        GLES20.glDisableVertexAttribArray(aTextureCoordLocation);
    }

    public void setWaterMark(Bitmap bitmap) {
        if (this.mBitmap != null) {
            this.mBitmap.recycle();
        }
        this.mBitmap = bitmap;
    }

    /**
     * 水印图片的bitmap
     */
    private Bitmap mBitmap;
    private int[] textures = new int[1];

    public void createTexture() {
        if (mBitmap != null) {
            //生成纹理
            GLES20.glGenTextures(1, textures, 0);
            //生成纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
            //对画面进行矩阵旋转
            //MatrixUtils.flip(matrix, false, true);
        }
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
/*
        int w = mBitmap.getWidth();
        int h = mBitmap.getHeight();
        float sWH = w / (float) h;
        float sWidthHeight = width / (float) height;
        if (width > height) {
            if (sWH > sWidthHeight) {
                System.out.println("==========width > height sWidthHeight==========>" + sWidthHeight);
                Matrix.orthoM(projectionMatrix, 0, -sWidthHeight * sWH, sWidthHeight * sWH, -1, 1, 3, 7);
            } else {
                System.out.println("==========width > height sWH==========>" + sWH);
                Matrix.orthoM(projectionMatrix, 0, -sWidthHeight / sWH, sWidthHeight / sWH, -1, 1, 3, 7);
            }
        } else {
            if (sWH > sWidthHeight) {
                System.out.println("==========width < height sWidthHeight==========>" + sWidthHeight + " sWH==>" + sWH);
                Matrix.orthoM(projectionMatrix, 0, -1, 1, -1 / sWidthHeight * sWH, 1 / sWidthHeight * sWH, 3, 7);
            } else {
                System.out.println("==========width < height sWH==========>" + sWH);
                Matrix.orthoM(projectionMatrix, 0, -1, 1, -sWH / sWidthHeight, sWH / sWidthHeight, 3, 7);
            }
        }*/
        Matrix.orthoM(projectionMatrix, 0, -1, 1, -1, 1, 3, 7);
        //设置相机位置
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 7.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        //计算变换矩阵
        Matrix.multiplyMM(matrix, 0, projectionMatrix, 0, mViewMatrix, 0);
    }
}
