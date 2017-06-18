package cn.edu.nju.cs.screencamera;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhantong on 2016/12/4.
 */

public class StreamDecode {
    private static final int QUEUE_TIME_OUT=4;
    private boolean stopQueue=false;
    private VideoToFrames videoToFrames;
    private CameraPreview cameraPreview;
    private boolean isVideo=false;
    private boolean isCamera=false;
    private boolean isImage=false;
    private boolean isJsonFile=false;
    private LinkedBlockingQueue<RawImage> queue;
    private String videoFilePath;
    protected String outputFilePath;
    protected JsonObject jsonRoot;

    public StreamDecode(){
        queue=new LinkedBlockingQueue<>(4);
    }
    public void setVideo(String videoFilePath){
        videoToFrames = new VideoToFrames();
        videoToFrames.setEnqueue(queue);
        this.videoFilePath=videoFilePath;
        isVideo=true;
    }
    public void setCamera(CameraPreview cameraPreview){
        this.cameraPreview=cameraPreview;
        isCamera=true;
    }
    public void setImage(String imageFilePath){
        RawImage rawImage=getRawImageYuv(imageFilePath);
        queue.add(rawImage);
        isImage=true;
    }
    public void setJsonFile(String jsonFilePath){
        try {
            JsonParser parser=new JsonParser();
            jsonRoot=(JsonObject) parser.parse(new FileReader(jsonFilePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        isJsonFile=true;
    }
    public void setOutputFilePath(String outputFilePath){
        this.outputFilePath=outputFilePath;
    }
    public void setStopQueue(){
        stopQueue=true;
    }
    public LinkedBlockingQueue<RawImage> getQueue(){
        return queue;
    }
    public boolean getIsVideo(){
        return isVideo;
    }
    public boolean getIsCamera(){
        return isCamera;
    }
    public void stream(LinkedBlockingQueue<RawImage> frames) throws InterruptedException{
        beforeStream();
        for(RawImage frame;((frame=frames.poll(QUEUE_TIME_OUT, TimeUnit.SECONDS))!=null)&&(frame.getPixels()!=null);){
            processFrame(frame);
            if(stopQueue){
                if(isVideo){
                    stopVideoDecoding();
                }
                if(isCamera){
                    stopCamera();
                }
                queue.clear();
                RawImage rawImage=new RawImage();
                queue.add(rawImage);
            }
        }
        afterStream();
    }
    public void stream(int[][] framesData){
        beforeStream();
        for(int[] frameData:framesData){
            processFrame(frameData);
            if(stopQueue){
                break;
            }
        }
        afterStream();
    }
    protected void beforeStream(){
    }
    protected void processFrame(RawImage frame){
    }
    protected void processFrame(int[] frameData){
    }
    protected void afterStream(){
    }
    public void start(){
        if(isJsonFile){
            int[][] data=new Gson().fromJson(jsonRoot.get("values"),int[][].class);
            stream(data);
        }else {
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
    protected void stopVideoDecoding(){
        videoToFrames.stopDecode();
    }
    protected void stopCamera(){
        Runnable task=new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    cameraPreview.stop();
                    notify();
                }
            }
        };
        new Handler(Looper.getMainLooper()).post(task);
        synchronized (task){
            try {
                task.wait();
            } catch (InterruptedException e) {
                Log.w("CameraPreview", "wait was interrupted");
            }
        }
    }
    protected void focusCamera(){
        cameraPreview.focus();
    }
    private static RawImage getRawImageYuv(String imageFilePath){
        String fileName= Files.getNameWithoutExtension(imageFilePath);
        int[] widthAndHeight=Utils.extractResolution(fileName);
        if(widthAndHeight==null){
            throw new IllegalArgumentException("cannot infer resolution from file name "+fileName);
        }
        byte[] data;
        try {
            data=Files.toByteArray(new File(imageFilePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        RawImage rawImage=new RawImage(data,widthAndHeight[0],widthAndHeight[1],RawImage.COLOR_TYPE_YUV);
        return rawImage;
    }
}
