package com.nju.cs.screencamera;

import android.hardware.Camera;

import java.util.List;

/**
 * Created by zhantong on 15/12/10.
 */
public class CameraSettings {
    public static int previewWidth;
    public static int previeHeight;
    public CameraSettings(){
        Camera mCamera;
        mCamera=Camera.open();
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> previewSizes=parameters.getSupportedPreviewSizes();
        for(Camera.Size size:previewSizes){
            if(size.width==1280 && size.height==720){
                previewWidth=size.width;
                previeHeight=size.height;
                break;
            }
        }
        mCamera.release();
        mCamera=null;
    }
}
