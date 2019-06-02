package com.yaozu.videoedittest;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.yalantis.ucrop.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.FileNameMap;
import java.net.URLConnection;

public class FileUtil {

    private static String TAG = FileUtil.class.getSimpleName();
    private String SDPATH;
    private String DWONLOADPATH;

    /**
     * "/wallpaper/file/"
     */
    public String getFileSDPATH() {
        return SDPATH;
    }

    /**
     * "/wallpaper/download/"
     */
    public String getDownLoadPath() {
        return DWONLOADPATH;
    }

    public FileUtil() {
        //得到当前外部存储设备的目录
        // /SDCARD
        SDPATH = getSDPath() + File.separator + "douinwallpaper" + File.separator + "file" + File.separator;
        DWONLOADPATH = getSDPath() + File.separator + "douinwallpaper" + File.separator + "download" + File.separator;
        File dir = new File(SDPATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File downdir = new File(DWONLOADPATH);
        if (!downdir.exists()) {
            downdir.mkdirs();
        }
    }

    public static String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED); //
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();//
        }
        if (sdDir == null) {
            return null;
        }
        return sdDir.toString();

    }

    /**
     * 专为Android4.4设计的从Uri获取文件绝对路径，以前的方法已不好使
     */
    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (FileUtils.isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (FileUtils.isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return FileUtils.getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (FileUtils.isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return FileUtils.getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return FileUtils.getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * 保存到本地
     *
     * @param croppedImage
     */
    public static void saveOutput(Bitmap croppedImage, String path) {
        File file = new File(path);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        OutputStream outStream;
        try {
            outStream = new FileOutputStream(file);
            croppedImage.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.flush();
            outStream.close();
            Log.i("CropImage", "bitmap saved tosd,path:" + file.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 递归方式 计算文件的大小
    public static long getTotalSizeOfFilesInDir(final File file) {
        if (file.isFile())
            return file.length();
        final File[] children = file.listFiles();
        long total = 0;
        if (children != null)
            for (final File child : children)
                total += getTotalSizeOfFilesInDir(child);
        return total;
    }

    private static String getMimeType(String fileName) {
        try {
            FileNameMap fileNameMap = URLConnection.getFileNameMap();
            String type = fileNameMap.getContentTypeFor(fileName);
            return type;
        } catch (Exception e) {
            //ToastUtil.showToast("文件名中含有特殊字符");
            Log.e("FileUtil", e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private final static String PREFIX_VIDEO = "video/";

    /**
     * 根据文件后缀名判断 文件是否是视频文件 * @param fileName 文件名 * @return 是否是视频文件
     */
    public static boolean isVideoFile(String fileName) {
        String mimeType = getMimeType(fileName);
        if (mimeType == null) {
            return false;
        }
        if (!TextUtils.isEmpty(fileName) && mimeType.contains(PREFIX_VIDEO)) {
            return true;
        }
        return false;
    }

    public static Bitmap getVideoThumbnail(String videoPath) {
        MediaMetadataRetriever media = new MediaMetadataRetriever();

        media.setDataSource(videoPath);

        return media.getFrameAtTime();
    }
}
