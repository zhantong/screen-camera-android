package com.nju.cs.screencamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingDeque;

/**
 * Created by zhantong on 15/12/9.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback,Camera.PreviewCallback {
    private static final String TAG = "main";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private BlockingDeque<byte[]> frames;
    private Context context;

    public CameraPreview(Context context,BlockingDeque<byte[]> frames) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        /*
        List<Camera.Size> previewSizes = mCamera.getParameters().getSupportedPreviewSizes();
        Camera.Size psize = null;
        for (int i = 0; i < previewSizes.size(); i++) {
            psize = previewSizes.get(i);
            Log.i(TAG + "initCamera", "PreviewSize,width: " + psize.width + " height" + psize.height);
        }
        */
        this.frames=frames;
        this.context=context;
    }

    public void surfaceCreated(SurfaceHolder holder) {
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
        parameters.setPreviewSize(1280, 720);
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
        //parameters.setPreviewFpsRange(14000,25000);
        //parameters.setPreviewFrameRate(30);
        mCamera.setParameters(parameters);
        //mCamera.setDisplayOrientation(90);
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

        //mCamera.startPreview();
    }
    boolean once=true;
    public void onPreviewFrame(byte[] data, Camera camera) {
        /*
        if(once) {
            int p;
            int size = 1280 * 720;
            int[] pixels = new int[size];
            for (int i = 0; i < size; i++) {
                p = data[i] & 0xff;
                pixels[i] = 0xff000000 | p << 16 | p << 8 | p;
            }
            Bitmap bmp = Bitmap.createBitmap(pixels, 1280, 720, Bitmap.Config.ARGB_8888);
            String path = Environment.getExternalStorageDirectory().toString();
            File file = new File(path, "FitnessGirl.jpg");
            try {
                FileOutputStream fOut = new FileOutputStream(file);
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, fOut);

                fOut.flush();
                fOut.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            once=false;
        }
        */
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
/*
    private void oldOpenCamera() {
        try {
            mCamera = Camera.open();
        }
        catch (RuntimeException e) {
            Log.e("camera", "failed to open front camera");
        }
    }
    private void newOpenCamera() {
        if (mThread == null) {
            mThread = new CameraHandlerThread();
        }

        synchronized (mThread) {
            mThread.openCamera();
        }
    }
    private CameraHandlerThread mThread = null;
    private class CameraHandlerThread extends HandlerThread {
        Handler mHandler = null;

        CameraHandlerThread() {
            super("CameraHandlerThread");
            start();
            mHandler = new Handler(getLooper());
        }

        synchronized void notifyCameraOpened() {
            notify();
        }

        void openCamera() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    oldOpenCamera();
                    notifyCameraOpened();
                }
            });
            try {
                wait();
            }
            catch (InterruptedException e) {
                Log.w("camera", "wait was interrupted");
            }
        }
    }
    */
}
