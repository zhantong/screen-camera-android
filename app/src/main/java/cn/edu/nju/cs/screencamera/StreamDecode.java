package cn.edu.nju.cs.screencamera;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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
    private LinkedBlockingQueue<RawImage> queue;
    private String videoFilePath;

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
                RawImage rawImage=new RawImage(null,0,0,0,0);
                queue.add(rawImage);
            }
        }
        afterStream();
    }
    protected void beforeStream(){
    }
    protected void processFrame(RawImage frame){
    }
    protected void afterStream(){
    }
    public void start(){
        if(isVideo){
            try {
                videoToFrames.decode(videoFilePath);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }else if(isCamera){
            cameraPreview.start(queue);
        }
        try {
            stream(queue);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private void stopVideoDecoding(){
        videoToFrames.stopDecode();
    }
    private void stopCamera(){
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
}
