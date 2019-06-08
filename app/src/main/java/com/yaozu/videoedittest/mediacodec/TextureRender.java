package com.yaozu.videoedittest.mediacodec;

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.yaozu.videoedittest.BlurFilter2;
import com.yaozu.videoedittest.OESFilter;
import com.yaozu.videoedittest.filter.ShowFilter;
import com.yaozu.videoedittest.mode.BlurLevel;
import com.yaozu.videoedittest.mode.VideoInfo;

import javax.microedition.khronos.opengles.GL10;

/**
 * Code for rendering a texture onto a surface using OpenGL ES 2.0.
 */
class TextureRender {
    private int mTextureID = -12345;
    private Context mContext;
    //======================clip========================
    boolean isClipMode;
    int curMode;

    //======================zoom========================
    private OESFilter oesFilter;
    private BlurFilter2 blurFilter;
    private ShowFilter showFilter;
    private float[] mSTMatrix = new float[16];

    boolean videoChanged = false;
    //第一段视频信息
    VideoInfo info;

    public TextureRender(Context context, VideoInfo info) {
        mContext = context;
        this.info = info;
        oesFilter = new OESFilter();
        blurFilter = new BlurFilter2();
        showFilter = new ShowFilter();
    }

    public int getTextureId() {
        return mTextureID;
    }

    public void drawFrame(SurfaceTexture st) {
        zoomDraw(st);
    }

    public void zoomDraw(SurfaceTexture st) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        /*surfaceTexture.updateTexImage();
         */

        oesFilter.setmSTMatrix(mSTMatrix);
        int nextTextureId = oesFilter.drawFrameBuffer(mTextureID);

        blurFilter.setmSTMatrix(mSTMatrix);
        nextTextureId = blurFilter.drawFrameBuffer(nextTextureId);

        showFilter.setmSTMatrix(mSTMatrix);
        showFilter.setCenterTextureId(mTextureID);
        showFilter.drawFrame(nextTextureId);
        GLES20.glFinish();
    }

    /**
     * Initializes GL state.  Call this after the EGL surface has been created and made current.
     */
    public void surfaceCreated() {
        oesFilter.create(mContext);
        blurFilter.create(mContext);
        showFilter.create(mContext);
        //启用透明
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int texture[] = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        mTextureID = texture[0];
    }

    public void onVideoSizeChanged(VideoInfo info) {
        videoChanged = true;
        oesFilter.onSizeChange(info.outputWidth, info.outputHeight);
        blurFilter.onSizeChange(info.outputWidth, info.outputHeight);
        showFilter.onSizeChange(info.outputWidth, info.outputHeight);

        oesFilter.setVideoSize(info.width, info.height);
        blurFilter.setVideoSize(info.width, info.height);
        showFilter.setVideoSize(info.width, info.height);
    }

    /**
     * just for clip video
     *
     * @param curMode
     */
    public void setClipMode(int curMode) {
        isClipMode = true;
        this.curMode = curMode;
    }

    public void setmSTMatrix(float[] mSTMatrix) {
        this.mSTMatrix = mSTMatrix;
    }

    public void setBlurLevel(BlurLevel level) {
        if (blurFilter != null) {
            blurFilter.setBlurLevel(level);
        }
    }
}