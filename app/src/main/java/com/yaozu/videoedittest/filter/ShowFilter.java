package com.yaozu.videoedittest.filter;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;

import com.yaozu.videoedittest.OESFilter;
import com.yaozu.videoedittest.R;
import com.yaozu.videoedittest.ShaderUtils;
import com.yaozu.videoedittest.utils.MatrixUtils;

public class ShowFilter extends OESFilter {

    private OESFilter mCenterFilter;

    private int centerTextureId;

    public void setCenterTextureId(int centerTextureId) {
        this.centerTextureId = centerTextureId;
    }

    @Override
    public void create(Context context) {
        super.create(context);
        mCenterFilter.create(context);
    }

    public ShowFilter() {
        super();
        mCenterFilter = new OESFilter() {
            public void setVideoSize(int videoWidth, int videoHeight) {
                this.videoWidth = videoWidth;
                this.videoHeight = videoHeight;

                float screenRatio = (float) viewWidth / viewHeight;
                float videoRatio = (float) videoWidth / videoHeight;
                if (videoRatio > screenRatio) {
                    //fitcenter
                    Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -videoRatio / screenRatio, videoRatio / screenRatio, -1f, 1f);
                } else {
                    //铺满屏幕
                    Matrix.orthoM(projectionMatrix, 0, -screenRatio / videoRatio, screenRatio / videoRatio, -1f, 1f, -1f, 1f);
                }
                MatrixUtils.flip(projectionMatrix, false, true);//矩阵上下翻转
            }
        };
    }

    public void onCreateProgram(Context context) {
        String vertexShader = ShaderUtils.readRawTextFile(context, R.raw.base_vertex);
        String fragmentShader = ShaderUtils.readRawTextFile(context, R.raw.base_fragment);
        programId = ShaderUtils.uCreateGlProgram(vertexShader, fragmentShader);
    }

    public void setVideoSize(int videoWidth, int videoHeight) {
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        mCenterFilter.setVideoSize(videoWidth, videoHeight);
    }

    @Override
    public void onSizeChange(int width, int height) {
        super.onSizeChange(width, height);
        mCenterFilter.onSizeChange(width, height);
    }

    @Override
    public void setmSTMatrix(float[] mSTMatrix) {
        super.setmSTMatrix(mSTMatrix);
        mCenterFilter.setmSTMatrix(mSTMatrix);
    }

    @Override
    public void drawFrame(int textureId) {
        super.drawFrame(textureId);
        mCenterFilter.drawFrame(centerTextureId);
    }


}
