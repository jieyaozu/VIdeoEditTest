package com.yaozu.videoedittest;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;

import com.yaozu.videoedittest.mode.BlurLevel;

public class BlurFilter2 extends OESFilter {
    private int pixoffSetXPosition;
    private int pixoffSetYPosition;
    private int isVerticalPosition;
    private int blurLevelPosition;

    protected boolean isVertical = false;

    protected int[] fbuffer = new int[2];
    protected int[] ftextbuffer = new int[2];

    private BlurLevel blurLevel = new BlurLevel();

    //用来计算模糊半径的宽高
    private int radiusWidth;
    private int radiusHeight;

    public BlurFilter2() {
        super();
    }

    public void onCreateProgram(Context context) {
        isVertical = false;
        String vertexShader = ShaderUtils.readRawTextFile(context, R.raw.blur_vetext_sharder);
        String fragmentShader = ShaderUtils.readRawTextFile(context, R.raw.blur_fragment_sharder);
        programId = ShaderUtils.uCreateGlProgram(vertexShader, fragmentShader);
        pixoffSetXPosition = GLES20.glGetUniformLocation(programId, "pixoffSetX");
        pixoffSetYPosition = GLES20.glGetUniformLocation(programId, "pixoffSetY");
        isVerticalPosition = GLES20.glGetUniformLocation(programId, "isVertical");
        blurLevelPosition = GLES20.glGetUniformLocation(programId, "blurLevel");
    }

    public void setVideoSize(int videoWidth, int videoHeight) {
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        calculateRaidusWH();
    }

    private void calculateRaidusWH() {
        float scale = (float) viewWidth / (float) viewHeight;
        float videoScale = (float) videoWidth / (float) videoHeight;
        if (videoScale >= scale) {
            radiusHeight = videoHeight;
            radiusWidth = (int) (radiusHeight * scale);
        } else if (videoScale < scale) {
            radiusWidth = videoWidth;
            radiusHeight = (int) (radiusWidth / scale);
        }
    }

    public void onSizeChange(final int width, final int height) {
        super.onSizeChange(width, height);
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
            GLES30.glDeleteTextures(FBUFFERTEXTURE.length, FBUFFERTEXTURE, 0);
            FBUFFERTEXTURE = null;
        }

        if (FBUFFERS != null) {
            GLES30.glDeleteFramebuffers(FBUFFERS.length, FBUFFERS, 0);
            FBUFFERS = null;
        }
    }

    //生成Textures
    private void genFrameTexturesBuffer() {
        FBUFFERS = new int[1];
        FBUFFERTEXTURE = new int[2];
        GLES20.glGenFramebuffers(FBUFFERS.length, FBUFFERS, 0);
        GLES20.glGenTextures(FBUFFERTEXTURE.length, FBUFFERTEXTURE, 0);
        for (int i = 0; i < FBUFFERTEXTURE.length; i++) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, FBUFFERS[0]);
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

    private int index = 0;

    public int drawFrameBuffer(int textureId) {
        // 绑定FBO
        GLES30.glViewport(0, 0, viewWidth, viewHeight);
        // 使用当前的program
        GLES30.glUseProgram(programId);
        isVertical = false;
        boolean first = true;
        for (int i = 0; i < 4; i++) {
            index = i;
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, FBUFFERS[0]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, FBUFFERTEXTURE[index % 2], 0);
            if (isVertical) {
                GLES20.glUniform1i(isVerticalPosition, 1);
                GLES20.glUniform1f(pixoffSetXPosition, 0);
                GLES20.glUniform1f(pixoffSetYPosition, blurLevel.getRadius() / radiusHeight);
                GLES20.glUniform1f(blurLevelPosition, blurLevel.getLevel());
            } else {
                GLES20.glUniform1i(isVerticalPosition, 0);
                GLES20.glUniform1f(pixoffSetXPosition, blurLevel.getRadius() / radiusWidth);
                GLES20.glUniform1f(pixoffSetYPosition, 0);
                GLES20.glUniform1f(blurLevelPosition, blurLevel.getLevel());
            }
            if (first) {
                onDrawBlurTextTure(textureId, vertexBuffer, textureVertexBuffer);
            } else {
                onDrawBlurTextTure(FBUFFERTEXTURE[(index - 1) % 2], vertexBuffer, textureVertexBuffer);
            }
            first = false;
            // 运行延时任务，这个要放在glUseProgram之后，要不然某些设置项会不生效
            runPendingOnDrawTasks();
            isVertical = !isVertical;
        }
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);

        return FBUFFERTEXTURE[index % 2];
    }

    public void setBlurLevel(BlurLevel level) {
        this.blurLevel = level;
    }
}
