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
import android.util.Log;
import android.view.Surface;

import com.yaozu.videoedittest.mode.BlurLevel;
import com.yaozu.videoedittest.mode.VideoInfo;

/**
 * Holds state associated with a Surface used for MediaCodec decoder output.
 * <p>
 * The (width,height) constructor for this class will prepare GL, create a SurfaceTexture,
 * and then create a Surface for that SurfaceTexture.  The Surface can be passed to
 * MediaCodec.configure() to receive decoder output.  When a frame arrives, we latch the
 * texture with updateTexImage, then render the texture with GL to a pbuffer.
 * <p>
 * The no-arg constructor skips the GL preparation step and doesn't allocate a pbuffer.
 * Instead, it just creates the Surface and SurfaceTexture, and when a frame arrives
 * we just draw it on whatever surface is current.
 * <p>
 * By default, the Surface will be using a BufferQueue in asynchronous mode, so we
 * can potentially drop frames.
 */
class OutputSurface implements SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "OutputSurface";
    private static final boolean VERBOSE = false;
    private static final int EGL_OPENGL_ES2_BIT = 4;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private Object mFrameSyncObject = new Object();     // guards mFrameAvailable
    private boolean mFrameAvailable;
    private TextureRender mTextureRender;
    //    private VideoDrawer mDrawer;
    private float[] mSTMatrix = new float[16];

    /**
     * Creates an OutputSurface using the current EGL context.  Creates a Surface that can be
     * passed to MediaCodec.configure().
     */
    public OutputSurface(Context context, VideoInfo info) {
        if (info.width <= 0 || info.height <= 0) {
            throw new IllegalArgumentException();
        }
        setup(context, info);
    }

    /**
     * Creates instances of TextureRender and SurfaceTexture, and a Surface associated
     * with the SurfaceTexture.
     */
    private void setup(Context context, VideoInfo info) {
        mTextureRender = new TextureRender(context, info);
        mTextureRender.surfaceCreated();
        mTextureRender.onVideoSizeChanged(info);
        mSurfaceTexture = new SurfaceTexture(mTextureRender.getTextureId());

        mSurfaceTexture.setOnFrameAvailableListener(this);
        mSurface = new Surface(mSurfaceTexture);
    }

    /**
     * Discard all resources held by this class, notably the EGL context.
     */
    public void release() {
        mSurface.release();
        mTextureRender = null;
        mSurface = null;
        mSurfaceTexture = null;
    }

    /**
     * Returns the Surface that we draw onto.
     */
    public Surface getSurface() {
        return mSurface;
    }

    /**
     * Latches the next buffer into the texture.  Must be called from the thread that created
     * the OutputSurface object, after the onFrameAvailable callback has signaled that new
     * data is available.
     */
    public void awaitNewImage() {
        final int TIMEOUT_MS = 500;
        synchronized (mFrameSyncObject) {
            while (!mFrameAvailable) {
                try {
                    // Wait for onFrameAvailable() to signal us.  Use a timeout to avoid
                    // stalling the test if it doesn't arrive.
                    mFrameSyncObject.wait(TIMEOUT_MS);
                    if (!mFrameAvailable) {
                        // TODO: if "spurious wakeup", continue while loop
                        throw new RuntimeException("Surface frame wait timed out");
                    }
                } catch (InterruptedException ie) {
                    // shouldn't happen
                    throw new RuntimeException(ie);
                }
            }
            mFrameAvailable = false;
        }
        // Latch the data.
//        mTextureRender.checkGlError("before updateTexImage");
//        mDrawer.checkGlError("before updateTexImage");
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mSTMatrix);
    }

    /**
     * Draws the data from SurfaceTexture onto the current EGL surface.
     */
    public void drawImage() {
        mTextureRender.setmSTMatrix(mSTMatrix);
        mTextureRender.drawFrame(mSurfaceTexture);
//        mDrawer.onDrawFrame(null);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture st) {

        if (VERBOSE) Log.d(TAG, "new frame available");
        synchronized (mFrameSyncObject) {
            if (mFrameAvailable) {
                throw new RuntimeException("mFrameAvailable already set, frame could be dropped");
            }
            mFrameAvailable = true;
            mFrameSyncObject.notifyAll();
        }
    }

    public void isBeauty(boolean isBeauty) {
//        mDrawer.isOpenBeauty(isBeauty);
//        mTextureRender.isOpenBeauty(isBeauty);
    }

    public void onVideoSizeChanged(VideoInfo info) {
        mTextureRender.onVideoSizeChanged(info);
    }

    public void setBlurLevel(BlurLevel level) {
        mTextureRender.setBlurLevel(level);
    }
}