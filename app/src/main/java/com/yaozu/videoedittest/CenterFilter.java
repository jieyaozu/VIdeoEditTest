package com.yaozu.videoedittest;

import android.content.Context;
import android.opengl.Matrix;

import com.yaozu.videoedittest.utils.MatrixUtils;

public class CenterFilter extends OESFilter {

    public CenterFilter() {
        super();
    }

    public void onCreateProgram(Context context) {
        String vertexShader = ShaderUtils.readRawTextFile(context, R.raw.base_vertex);
        String fragmentShader = ShaderUtils.readRawTextFile(context, R.raw.base_fragment);
        programId = ShaderUtils.uCreateGlProgram(vertexShader, fragmentShader);
    }

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
}
