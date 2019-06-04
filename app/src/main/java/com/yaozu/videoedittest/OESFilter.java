package com.yaozu.videoedittest;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import com.yaozu.videoedittest.utils.MatrixUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;

public class OESFilter {
    protected int programId;
    protected FloatBuffer vertexBuffer;
    protected FloatBuffer textureVertexBuffer;
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

    protected final float[] projectionMatrix = MatrixUtils.getOriginalMatrix();
    protected float[] mSTMatrix = new float[16];

    private int uTextureSamplerLocation;
    /**
     * 纹理坐标句柄
     */
    protected int aTextureCoordLocation;
    protected int uSTMMatrixHandle;
    protected int aPositionLocation;
    protected int uMatrixLocation;

    protected int viewWidth;
    protected int viewHeight;

    protected int videoWidth, videoHeight;
    protected int[] FBUFFERS = new int[1];
    protected int[] FBUFFERTEXTURE = new int[1];

    public OESFilter() {
        initBuffer();
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

    public void create(Context context) {
        onCreateProgram(context);
        aPositionLocation = GLES20.glGetAttribLocation(programId, "aPosition");
        aTextureCoordLocation = GLES20.glGetAttribLocation(programId, "aTexCoord");
        uMatrixLocation = GLES20.glGetUniformLocation(programId, "uMatrix");
        uSTMMatrixHandle = GLES20.glGetUniformLocation(programId, "uSTMatrix");
        uTextureSamplerLocation = GLES20.glGetUniformLocation(programId, "sTexture");
        //unBindFrame();
    }

    public void onCreateProgram(Context context) {
        String vertexShader = ShaderUtils.readRawTextFile(context, R.raw.oes_vetext_sharder);
        String fragmentShader = ShaderUtils.readRawTextFile(context, R.raw.oes_fragment_sharder);
        programId = ShaderUtils.uCreateGlProgram(vertexShader, fragmentShader);
    }

    public void setVideoSize(int videoWidth, int videoHeight) {
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;

        float screenRatio = (float) viewWidth / viewHeight;
        float videoRatio = (float) videoWidth / videoHeight;
        if (videoRatio < screenRatio) {
            //fitcenter
            Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -videoRatio / screenRatio, videoRatio / screenRatio, -1f, 1f);
        } else {
            //铺满屏幕
            Matrix.orthoM(projectionMatrix, 0, -screenRatio / videoRatio, screenRatio / videoRatio, -1f, 1f, -1f, 1f);
        }
        MatrixUtils.flip(projectionMatrix, false, true);//矩阵上下翻转
    }

    protected void initFrameBuffer() {
        destroyFrameBuffer();
        genFrameTexturesBuffer();
    }

    /**
     * 销毁纹理
     */
    public void destroyFrameBuffer() {
        if (FBUFFERTEXTURE != null) {
            GLES30.glDeleteTextures(1, FBUFFERTEXTURE, 0);
            FBUFFERTEXTURE = null;
        }

        if (FBUFFERS != null) {
            GLES30.glDeleteFramebuffers(1, FBUFFERS, 0);
            FBUFFERS = null;
        }
    }

    //生成Textures
    private void genFrameTexturesBuffer() {
        FBUFFERS = new int[1];
        FBUFFERTEXTURE = new int[1];
        GLES20.glGenFramebuffers(FBUFFERS.length, FBUFFERS, 0);
        GLES20.glGenTextures(FBUFFERTEXTURE.length, FBUFFERTEXTURE, 0);
        for (int i = 0; i < FBUFFERTEXTURE.length; i++) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, FBUFFERS[i]);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, FBUFFERTEXTURE[i]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, viewWidth, viewHeight,
                    0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, FBUFFERTEXTURE[i], 0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        }
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e("OESFilter", "Failed to create framebuffer!!!");
        } else {
            Log.e("OESFilter", "create framebuffer success!!!");
        }
    }

    public void setmSTMatrix(float[] mSTMatrix) {
        this.mSTMatrix = mSTMatrix;
    }

    public void drawFrame(int textureId) {
        /*GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);*/
        GLES20.glViewport(0, 0, viewWidth, viewHeight);
        GLES20.glUseProgram(programId);
        runPendingOnDrawTasks();
        onDrawTexture(textureId, vertexBuffer, textureVertexBuffer);
    }

    public int drawFrameBuffer(int textureId) {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        // 绑定FBO
        GLES30.glViewport(0, 0, viewWidth, viewHeight);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, FBUFFERS[0]);
        // 使用当前的program
        GLES30.glUseProgram(programId);
        // 运行延时任务，这个要放在glUseProgram之后，要不然某些设置项会不生效
        runPendingOnDrawTasks();
        onDrawTexture(textureId, vertexBuffer, textureVertexBuffer);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        return FBUFFERTEXTURE[0];
    }

    public void onDrawTexture(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, projectionMatrix, 0);
        GLES20.glUniformMatrix4fv(uSTMMatrixHandle, 1, false, mSTMatrix, 0);
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionLocation);
        GLES20.glVertexAttribPointer(aPositionLocation, 2, GLES20.GL_FLOAT, false,
                0, vertexBuffer);
        textureBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aTextureCoordLocation);
        GLES20.glVertexAttribPointer(aTextureCoordLocation, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTextureSamplerLocation, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(aPositionLocation);
        GLES20.glDisableVertexAttribArray(aTextureCoordLocation);
        GLES30.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);
    }

    public void onDrawBlurTextTure(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, projectionMatrix, 0);
        GLES20.glUniformMatrix4fv(uSTMMatrixHandle, 1, false, mSTMatrix, 0);
        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aPositionLocation);
        GLES20.glVertexAttribPointer(aPositionLocation, 2, GLES20.GL_FLOAT, false,
                0, vertexBuffer);
        textureBuffer.position(0);
        GLES20.glEnableVertexAttribArray(aTextureCoordLocation);
        GLES20.glVertexAttribPointer(aTextureCoordLocation, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(uTextureSamplerLocation, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    public void onSizeChange(final int width, final int height) {
        viewWidth = width;
        viewHeight = height;
        initFrameBuffer();
    }

    private final LinkedList<Runnable> mRunOnDraw = new LinkedList<>();

    /**
     * 添加延时任务
     *
     * @param runnable
     */
    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }

    /**
     * 运行延时任务
     */
    protected void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }
}
