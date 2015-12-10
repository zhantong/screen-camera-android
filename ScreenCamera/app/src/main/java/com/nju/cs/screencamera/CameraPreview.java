package com.nju.cs.screencamera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;
import java.util.concurrent.BlockingDeque;

/**
 * Created by zhantong on 15/12/9.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback,Camera.PreviewCallback {
    private static final String TAG = "main";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private BlockingDeque<byte[]> frames;

    public CameraPreview(Context context,BlockingDeque<byte[]> frames) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        this.frames=frames;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        CameraSettings cameraSettings=new CameraSettings();
        mCamera=Camera.open();
        //newOpenCamera();
        mCamera.setPreviewCallback(this);
        // 当Surface被创建之后，开始Camera的预览
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            Log.d(TAG, "预览失败");
        }
        /*
        Camera.Parameters parameters = mCamera.getParameters();
        List<String> allFocus = parameters.getSupportedFocusModes();
        for(String s:allFocus){
            System.out.println(s);
        }
        */
        Camera.Parameters parameters=mCamera.getParameters();
        //parameters.setFlashMode("off");
        //parameters.setPreviewFormat(ImageFormat.RGB_565);
        /*
        Camera.Parameters params = mCamera.getParameters();
        for(int i: params.getSupportedPreviewFormats()) {
            Log.e(TAG, "preview format supported are = "+i);
        }
        */
        parameters.setPreviewSize(CameraSettings.previewWidth, CameraSettings.previeHeight);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        parameters.setRecordingHint(true);
        /*
        Camera.Parameters params = mCamera.getParameters();
        for(int[] i: params.getSupportedPreviewFpsRange()) {
            Log.i(TAG, "preview fps range = ");
            for(int j:i) {
                Log.i(TAG,j+" ");
            }
        }
        */
        mCamera.setParameters(parameters);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.release();
        mCamera=null;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Thread preview_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                mCamera.startPreview();
            }
        }, "preview_thread");
        preview_thread.start();

    }
    public void onPreviewFrame(byte[] data, Camera camera) {
        frames.add(data);
        //Log.d("queue length:", Integer.toString(frames.size()));
    }
    public void stop(){
        mHolder.removeCallback(this);
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera=null;
    }
}
