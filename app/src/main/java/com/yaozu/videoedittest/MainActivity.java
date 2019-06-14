package com.yaozu.videoedittest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.yaozu.videoedittest.mediacodec.VideoClipper;
import com.yaozu.videoedittest.mode.BlurLevel;
import com.yaozu.videoedittest.utils.Constants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private VideoPreviewView videoPreviewView;
    private TextView tvFps;
    private int REQUEST_VIDEO_CODE = 1100;
    private Spinner spinner;
    private List<BlurLevel> levelList = new ArrayList<>();
    private SpinnerAdapter spinnerAdapter;
    private Button btSave;
    private String videoPath;

    private BlurLevel currentBlurLevel;
    private String outputPath;
    static final int VIDEO_PREPARE = 0;
    static final int VIDEO_START = 1;
    static final int VIDEO_UPDATE = 2;
    static final int VIDEO_PAUSE = 3;
    static final int VIDEO_CUT_FINISH = 4;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case VIDEO_CUT_FINISH:
                    Toast.makeText(MainActivity.this, "视频保存地址   " + outputPath, Toast.LENGTH_SHORT).show();
                    updatePhotoMedia(new File(outputPath));
                    break;
            }
        }
    };

    //更新图库
    private void updatePhotoMedia(File file) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(file));
        sendBroadcast(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            } else {
                // 0 是自己定义的请求coude
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }
        }
        // Example of a call to a native method
        Button button = findViewById(R.id.sample_text);
        btSave = findViewById(R.id.sample_save);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("*/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, REQUEST_VIDEO_CODE);
            }
        });
        btSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoPreviewView.pause();
                VideoClipper clipper = new VideoClipper(MainActivity.this);
                clipper.setInputVideoPath(videoPath);
                clipper.setBlurLevel(currentBlurLevel);
                outputPath = Constants.getPath("video/clip/", System.currentTimeMillis() + ".mp4");
                clipper.setOutputVideoPath(outputPath);
                clipper.setOnVideoCutFinishListener(new VideoClipper.OnVideoCutFinishListener() {
                    @Override
                    public void onFinish() {
                        mHandler.sendEmptyMessage(VIDEO_CUT_FINISH);
                    }
                });
                try {
                    Log.e("hero", "-----PreviewActivity---clipVideo");
                    clipper.clipVideo(0, videoPreviewView.getDuration() * 1000);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        videoPreviewView = findViewById(R.id.video_view);
        tvFps = findViewById(R.id.show_fps);
        spinner = findViewById(R.id.switch_blur_level);
        videoPreviewView.setOnFpsCallback(new VideoPreviewView.OnFpsCallback() {
            @Override
            public void onFpsCallback(final float fps) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvFps.setText("fps = " + fps);
                    }
                });
            }
        });
        initSpinnerData();
    }

    private void initSpinnerData() {
        for (int i = 0; i < 4; i++) {
            BlurLevel level = new BlurLevel();
            if (i == 0) {
                level.setLevel(1);
                level.setRadius(0.7f);
                level.setLevelName("一级");
            } else if (i == 1) {
                level.setLevel(2);
                level.setRadius(1.1f);
                level.setLevelName("二级");
            } else if (i == 2) {
                level.setLevel(3);
                level.setRadius(2.5f);
                level.setLevelName("三级");
            } else if (i == 3) {
                level.setLevel(4);
                level.setRadius(4.0f);
                level.setLevelName("四级");
            }
            levelList.add(level);
        }
        currentBlurLevel = levelList.get(0);
        spinnerAdapter = new SpinnerAdapter();
        spinner.setAdapter(spinnerAdapter);
        spinner.setDropDownVerticalOffset(getResources().getDimensionPixelSize(R.dimen.dimen_40));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (videoPreviewView != null) {
                    videoPreviewView.setBlurLevel(levelList.get(position));
                    currentBlurLevel = levelList.get(position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_VIDEO_CODE) {
                Uri uri = data.getData();
                videoPath = FileUtil.getPath(this, uri);
                videoPreviewView.setVideoPath(videoPath);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoPreviewView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        videoPreviewView.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoPreviewView.release();
        videoPreviewView.releaseSurfaceTexture();
    }

    class SpinnerAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return levelList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
            View view = null;
            if (convertView != null) {
                view = convertView;
            } else {
                view = inflater.inflate(R.layout.text_line, null);
            }
            BlurLevel planType = levelList.get(position);
            TextView textView = (TextView) view.findViewById(R.id.text_line_text);
            textView.setText(planType.getLevelName());
            return view;
        }
    }
}
