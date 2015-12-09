package com.nju.cs.screencamera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;

/**
 * Created by zhantong on 15/12/9.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback,Camera.PreviewCallback {
    private static final String TAG = "main";
    private SurfaceHolder mHolder;
    private Camera mCamera;

    public CameraPreview(Context context) {
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
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mCamera=Camera.open();
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
        //parameters.setPreviewFormat(PixelFormat.JPEG);
        parameters.setPreviewSize(1920, 1080);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(90);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.release();
        mCamera=null;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        mCamera.startPreview();
    }
    public void onPreviewFrame(byte[] data, Camera camera) {

    }
}
