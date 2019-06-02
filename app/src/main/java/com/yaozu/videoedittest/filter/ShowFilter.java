package com.yaozu.videoedittest.filter;

import android.content.Context;

import com.yaozu.videoedittest.OESFilter;
import com.yaozu.videoedittest.R;
import com.yaozu.videoedittest.ShaderUtils;

public class ShowFilter extends OESFilter {
    public ShowFilter() {
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
    }
}
