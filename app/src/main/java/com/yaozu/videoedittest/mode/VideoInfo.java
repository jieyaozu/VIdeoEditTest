package com.yaozu.videoedittest.mode;

import java.io.Serializable;

/**
 * Created by Administrator on 2017/6/29 0029.
 * 视频的信息bean
 */

public class VideoInfo implements Serializable {
    public String path;//路径
    public int rotation;//旋转角度
    public int width;//宽
    public int height;//高
    public int bitRate;//比特率
    public int frameRate;//帧率
    public int frameInterval;//关键帧间隔
    public int duration;//时长

    public int outputWidth;//期望输出宽度
    public int outputHeight;//期望输出高度
}
