package com.nju.cs.screencamera;

import android.hardware.Camera;
import android.util.Log;

import java.util.List;

/**
 * Created by zhantong on 15/12/10.
 */
public class CameraSettings {
    private static final String TAG = "CameraSettings";
    private static final boolean VERBOSE = false;
    public static int previewWidth;
    public static int previeHeight;
    public static Camera.Parameters parameters;
    public CameraSettings(){
        Camera mCamera;
        mCamera=Camera.open();
        parameters = mCamera.getParameters();
        getPreviewSize();
        parameters.setPreviewSize(previewWidth, previeHeight);
        //parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        parameters.setRecordingHint(true);
        parameters.setExposureCompensation(getMinExposureCompensation());
        mCamera.release();
        mCamera=null;
    }
    public static void getPreviewSize(){
        for(Camera.Size size:parameters.getSupportedPreviewSizes()){
            if(size.width==1280 && size.height==720){
                previewWidth=size.width;
                previeHeight=size.height;
                break;
            }
        }
    }
    public static void getFocusMode(){
        for(String i:parameters.getSupportedFocusModes()){
            if(VERBOSE){Log.e(TAG, "focus mode supported are = " + i);}
        }
    }
    public static void getPreviewFormat(){
        for(int i: parameters.getSupportedPreviewFormats()) {
            if(VERBOSE){Log.e(TAG, "preview format supported are = " + i);}
        }
    }
    public static void getPreviewFpsRange(){
        for(int[] i: parameters.getSupportedPreviewFpsRange()) {
            if(VERBOSE){Log.i(TAG, "preview fps range = ");}
            for(int j:i) {
                if(VERBOSE){Log.i(TAG,j+" ");}
            }
        }
    }
    public static int getMinExposureCompensation(){
        int min=parameters.getMinExposureCompensation();
        if(VERBOSE){Log.i(TAG,"MinExposureCompensation:"+min);}
        return min;
    }
}
