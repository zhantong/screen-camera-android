package cn.edu.nju.cs.screencamera;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import cn.edu.nju.cs.screencamera.Logback.ConfigureLogback;

/**
 * Created by zhantong on 2016/12/4.
 */

public class StreamDecode {
    private static final int QUEUE_TIME_OUT = 4;
    private boolean stopQueue = false;
    private VideoToFrames videoToFrames;
    private CameraPreview cameraPreview;
    private boolean isVideo = false;
    private boolean isCamera = false;
    private boolean isImage = false;
    private boolean isJsonFile = false;
    private LinkedBlockingQueue<RawImage> queue;
    private String videoFilePath;
    protected JsonObject inputJsonRoot;
    Logger LOG;
    Activity activity;

    public StreamDecode() {
        queue = new LinkedBlockingQueue<>(4);
    }


    public void setVideo(String videoFilePath) {
        videoToFrames = new VideoToFrames();
        videoToFrames.setEnqueue(queue);
        this.videoFilePath = videoFilePath;
        isVideo = true;
    }

    public void setCamera(CameraPreview cameraPreview) {
        this.cameraPreview = cameraPreview;
        isCamera = true;
    }

    public void setImage(String imageFilePath) {
        RawImage rawImage = getRawImageYuv(imageFilePath);
        queue.add(rawImage);
        isImage = true;
    }

    public void setJsonFile(String jsonFilePath) {
        try {
            JsonParser parser = new JsonParser();
            inputJsonRoot = (JsonObject) parser.parse(new FileReader(jsonFilePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        isJsonFile = true;
    }

    public void setStopQueue() {
        stopQueue = true;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public LinkedBlockingQueue<RawImage> getQueue() {
        return queue;
    }

    public boolean getIsVideo() {
        return isVideo;
    }

    public boolean getIsCamera() {
        return isCamera;
    }

    void beforeStream() {
    }

    void processFrame(RawImage frame) {
    }

    void processFrame(JsonElement frameData) {
    }

    File restoreFile() {
        return null;
    }

    void afterStream() {
    }

    public void stream(LinkedBlockingQueue<RawImage> frames) throws InterruptedException {
        beforeStream();
        for (RawImage frame; ((frame = frames.poll(QUEUE_TIME_OUT, TimeUnit.SECONDS)) != null) && (frame.getPixels() != null); ) {
            processFrame(frame);
            if (stopQueue) {
                if (isVideo) {
                    stopVideoDecoding();
                }
                if (isCamera) {
                    stopCamera();
                }
                queue.clear();
                RawImage rawImage = new RawImage();
                queue.add(rawImage);
            }
        }
        final File file = restoreFile();
        if (file != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle("文件传输完成")
                            .setMessage(file.getAbsolutePath())
                            .setPositiveButton("打开", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String mimeType = URLConnection.guessContentTypeFromName(file.getName());
                                    if (mimeType != null) {
                                        Intent newIntent = new Intent(Intent.ACTION_VIEW);
                                        newIntent.setDataAndType(Uri.fromFile(file), mimeType);
                                        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        activity.startActivity(newIntent);
                                    } else {
                                        new AlertDialog.Builder(activity).setTitle("未识别的文件类型")
                                                .setMessage("未识别的文件后缀名或文件内容")
                                                .setPositiveButton("确定", null)
                                                .show();
                                    }
                                }
                            })
                            .setNegativeButton("取消", null);
                    builder.create().show();
                }
            });
        }
        afterStream();
    }

    public void stream(JsonArray framesData) {
        beforeStream();
        for (JsonElement frameData : framesData) {
            processFrame(frameData);
            if (stopQueue) {
                break;
            }
        }
        afterStream();
    }

    public void start() {
        initLogging();
        if (isJsonFile) {
            JsonArray data = inputJsonRoot.getAsJsonArray("values");
            stream(data);
        } else {
            if (isVideo) {
                try {
                    videoToFrames.decode(videoFilePath);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            } else if (isCamera) {
                cameraPreview.start(queue);
            } else if (isImage) {
            }
            try {
                stream(queue);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected void stopVideoDecoding() {
        videoToFrames.stopDecode();
    }

    protected void stopCamera() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    cameraPreview.stop();
                    notify();
                }
            }
        };
        new Handler(Looper.getMainLooper()).post(task);
        synchronized (task) {
            try {
                task.wait();
            } catch (InterruptedException e) {
                Log.w("CameraPreview", "wait was interrupted");
            }
        }
    }

    protected void focusCamera() {
        cameraPreview.focus();
    }

    private static RawImage getRawImageYuv(String imageFilePath) {
        String fileName = Files.getNameWithoutExtension(imageFilePath);
        int[] widthAndHeight = Utils.extractResolution(fileName);
        if (widthAndHeight == null) {
            throw new IllegalArgumentException("cannot infer resolution from file name " + fileName);
        }
        byte[] data;
        try {
            data = Files.toByteArray(new File(imageFilePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        RawImage rawImage = new RawImage(data, widthAndHeight[0], widthAndHeight[1], RawImage.COLOR_TYPE_YUV);
        return rawImage;
    }

    void initLogging() {
        boolean enableLogging = PreferenceManager.getDefaultSharedPreferences(App.getContext()).getBoolean(App.getContext().getString(R.string.logging), false);
        if (enableLogging) {
            ConfigureLogback.configureLogbackDirectly(Utils.combinePaths(Environment.getExternalStorageDirectory().getAbsolutePath(), (new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date())) + ".log"));
            LOG = LoggerFactory.getLogger(StreamDecode.class);
        } else {
            LOG = null;
        }
    }
}
