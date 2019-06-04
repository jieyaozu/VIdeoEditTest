package com.yaozu.videoedittest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.Surface;

import com.yaozu.videoedittest.filter.NoFilter;
import com.yaozu.videoedittest.filter.ShowFilter;
import com.yaozu.videoedittest.filter.WaterMarkFilter;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class VideoPreviewView extends GLSurfaceView implements GLSurfaceView.Renderer {
    private String TAG = getClass().getSimpleName().toString();
    private SurfaceTexture surfaceTexture;

    private int textureId;

    private MediaPlayer mediaPlayer;

    private OESFilter oesFilter;
    private BlurFilter2 blurFilter;
    private ShowFilter showFilter;
    private WaterMarkFilter waterFilter;
    private float[] mSTMatrix = new float[16];

    private int screenWidth, screenHeight;
    private int videoWidth, videoHeight;

    private NoFilter mShow;

    static {
        System.loadLibrary("native-lib");
    }

    public VideoPreviewView(Context context) {
        super(context);
        init(context);
    }

    public VideoPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        setPreserveEGLContextOnPause(false);
        setCameraDistance(100);

        oesFilter = new OESFilter();
        blurFilter = new BlurFilter2();
        showFilter = new ShowFilter();
        waterFilter = new WaterMarkFilter(context.getResources());
        mShow = new NoFilter(context.getResources());
        Bitmap mBitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.watermark);
        waterFilter.setWaterMark(mBitmap);
    }

    public void setVideoPath(String paths) {
        try {
            MediaMetadataRetriever retr = new MediaMetadataRetriever();
            retr.setDataSource(paths);
            String width = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            videoWidth = Integer.parseInt(width);
            videoHeight = Integer.parseInt(height);
            oesFilter.setVideoSize(videoWidth, videoHeight);
            blurFilter.setVideoSize(videoWidth, videoHeight);
            showFilter.setVideoSize(videoWidth, videoHeight);
            waterFilter.setSize(screenWidth, screenHeight, videoWidth, videoHeight);

            if (mediaPlayer != null) {
                release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(paths);
            Surface surface = new Surface(surfaceTexture);
            mediaPlayer.setSurface(surface);
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        onNativeCreate();
        oesFilter.create(getContext());
        blurFilter.create(getContext());
        showFilter.create(getContext());
        waterFilter.create();
        mShow.create();
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
        textureId = texture[0];
        surfaceTexture = new SurfaceTexture(texture[0]);
        surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                requestRender();
            }
        });
        GLES30.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        onNativeSurfaceChanged(width, height);
        screenWidth = width;
        screenHeight = height;
        waterFilter.setSize(width, height);
        oesFilter.onSizeChange(width, height);
        blurFilter.onSizeChange(width, height);
        showFilter.onSizeChange(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        onNativeDraw();
        drawFrameCount();
        if (onFpsCallback != null) {
            onFpsCallback.onFpsCallback(getFPS());
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        surfaceTexture.updateTexImage();
        surfaceTexture.getTransformMatrix(mSTMatrix);

        oesFilter.setmSTMatrix(mSTMatrix);
        int nextTextureId = oesFilter.drawFrameBuffer(textureId);

        blurFilter.setmSTMatrix(mSTMatrix);
        nextTextureId = blurFilter.drawFrameBuffer(nextTextureId);

        showFilter.setmSTMatrix(mSTMatrix);
        showFilter.setCenterTextureId(textureId);
        showFilter.drawFrame(nextTextureId);


       /* GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fFrame[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, fTexture[0], 0);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                GLES20.GL_RENDERBUFFER, fRender[0]);
        blurFilter.setmSTMatrix(mSTMatrix);
        blurFilter.draw();
        unBindFrame();

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fFrame[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, fTexture[1], 0);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
                GLES20.GL_RENDERBUFFER, fRender[0]);
        waterFilter.setTextureId(fTexture[0]);
        waterFilter.draw();
        unBindFrame();
        */
        //waterFilter.setTextureId(textureId);
        //waterFilter.draw();

        //mShow.setTextureId(textureId);
        //mShow.draw();
    }

    public static native int onNativeCreate();

    public static native void onNativeDraw();

    public static native void onNativeSurfaceChanged(int width, int height);

    //计算帧率

    /**
     * 计算绘制帧数据
     */
    public void drawFrameCount() {
        long currentTime = System.currentTimeMillis();
        if (mUpdateTime == 0) {
            mUpdateTime = currentTime;
        }
        if ((currentTime - mUpdateTime) > TIMETRAVEL_MS) {
            mCurrentFps = ((float) mTimes / (currentTime - mUpdateTime)) * 1000.0f;
            mUpdateTime = currentTime;
            mTimes = 0;
        }
        mTimes++;
    }

    private static final long TIMETRAVEL = 1;
    private static final long TIMETRAVEL_MS = TIMETRAVEL * 1000;
    private static final long TIMETRAVEL_MAX_DIVIDE = 2 * TIMETRAVEL_MS;
    private int mTimes;
    private float mCurrentFps;
    private long mUpdateTime;

    /**
     * 获取FPS
     *
     * @return
     */
    public float getFPS() {
        if ((System.currentTimeMillis() - mUpdateTime) > TIMETRAVEL_MAX_DIVIDE) {
            return 0;
        } else {
            return mCurrentFps;
        }
    }

    private OnFpsCallback onFpsCallback;

    public OnFpsCallback getOnFpsCallback() {
        return onFpsCallback;
    }

    public void setOnFpsCallback(OnFpsCallback onFpsCallback) {
        this.onFpsCallback = onFpsCallback;
    }

    public interface OnFpsCallback {
        void onFpsCallback(float fps);
    }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    public void start() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }
}
