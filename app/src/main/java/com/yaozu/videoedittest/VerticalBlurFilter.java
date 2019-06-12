package com.yaozu.videoedittest;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.util.Log;

import com.yaozu.videoedittest.mode.BlurLevel;

public class VerticalBlurFilter extends OESFilter {
    private int pixoffSetXPosition;
    private int pixoffSetYPosition;
    private int blurLevelPosition;

    protected int[] fbuffer = new int[2];
    protected int[] ftextbuffer = new int[2];

    private BlurLevel blurLevel = new BlurLevel();

    //用来计算模糊半径的宽高
    private int radiusWidth;
    private int radiusHeight;

    public VerticalBlurFilter() {
        super();
    }

    public void onCreateProgram(Context context) {
        String vertexShader = ShaderUtils.readRawTextFile(context, R.raw.blur_vetext_sharder);
        String fragmentShader = ShaderUtils.readRawTextFile(context, R.raw.blur_vertical_fragment_sharder);
        programId = ShaderUtils.uCreateGlProgram(vertexShader, fragmentShader);
        pixoffSetXPosition = GLES20.glGetUniformLocation(programId, "pixoffSetX");
        pixoffSetYPosition = GLES20.glGetUniformLocation(programId, "pixoffSetY");
        blurLevelPosition = GLES20.glGetUniformLocation(programId, "blurLevel");
    }

    public void setVideoSize(int videoWidth, int videoHeight) {
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        calculateRaidusWH();
    }

    //计算用来求模糊半径的宽高
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
        //宽高保持在一定的比例之中
        if (radiusWidth * radiusHeight > 200000) {
            float s = 200000.0f / (radiusWidth * radiusHeight);
            s = (float) Math.sqrt(s);
            radiusHeight = (int) (s * radiusHeight);
            radiusWidth = (int) (s * radiusWidth);
        }
    }

    public void onSizeChange(final int width, final int height) {
        super.onSizeChange(width, height);
    }

    public int drawFrameBuffer(int textureId) {
        // 绑定FBO
        GLES30.glViewport(0, 0, viewWidth, viewHeight);
        // 使用当前的program
        GLES30.glUseProgram(programId);

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, FBUFFERS[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, FBUFFERTEXTURE[0], 0);
        GLES20.glUniform1f(pixoffSetXPosition, 0);
        GLES20.glUniform1f(pixoffSetYPosition, blurLevel.getRadius() / radiusHeight);
        GLES20.glUniform1i(blurLevelPosition, (int) blurLevel.getLevel());
        onDrawBlurTextTure(textureId, vertexBuffer, textureVertexBuffer);
        // 运行延时任务，这个要放在glUseProgram之后，要不然某些设置项会不生效
        runPendingOnDrawTasks();
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);

        return FBUFFERTEXTURE[0];
    }

    public void setBlurLevel(BlurLevel level) {
        this.blurLevel = level;
    }
}
