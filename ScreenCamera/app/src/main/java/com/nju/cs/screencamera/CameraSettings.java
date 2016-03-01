package com.nju.cs.screencamera;

import android.hardware.Camera;
import android.util.Log;


/**
 * 获取和设置相机参数
 */
public class CameraSettings {
    private static final String TAG = "CameraSettings";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log
    public static int previewWidth;//相机预览宽度
    public static int previewHeight;//相机预览高度
    public static Camera.Parameters parameters;//相机参数

    public static void init(){
        Camera mCamera;
        mCamera = Camera.open();
        parameters = mCamera.getParameters();
        getPreviewSize();
        parameters.setPreviewSize(previewWidth, previewHeight);
        //parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        parameters.setRecordingHint(true);
        parameters.setExposureCompensation(getMinExposureCompensation());
        mCamera.release();
        mCamera = null;
    }

    /**
     * 获取和设置预览大小,即分辨率
     * 注意这个方法目前只是确认相机是否支持1280*720分辨率
     * 若不支持则很可能出错
     */
    public static void getPreviewSize() {
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width == 1280 && size.height == 720) {
                previewWidth = size.width;
                previewHeight = size.height;
                break;
            }
        }
    }
    public static int previewWidth(){
        assert parameters!=null;
        return previewWidth;
    }
    public static int previewHeight(){
        assert parameters!=null;
        return previewHeight;
    }

    /**
     * 获取相机支持的对焦方式
     */
    public static void getFocusMode() {
        for (String i : parameters.getSupportedFocusModes()) {
            if (VERBOSE) {
                Log.e(TAG, "focus mode supported are = " + i);
            }
        }
    }

    /**
     * 获取相机支持的预览帧格式,如NV21
     */
    public static void getPreviewFormat() {
        for (int i : parameters.getSupportedPreviewFormats()) {
            if (VERBOSE) {
                Log.e(TAG, "preview format supported are = " + i);
            }
        }
    }

    /**
     * 获取相机支持的预览帧速度范围
     * 注意获取和设置精确帧速率的API已经废除
     */
    public static void getPreviewFpsRange() {
        for (int[] i : parameters.getSupportedPreviewFpsRange()) {
            if (VERBOSE) {
                Log.i(TAG, "preview fps range = ");
            }
            for (int j : i) {
                if (VERBOSE) {
                    Log.i(TAG, j + " ");
                }
            }
        }
    }

    /**
     * 获取相机支持的最低曝光度
     * 测试发现曝光度越低,对识别二维码越有利
     *
     * @return 返回相机支持的最低曝光度
     */
    public static int getMinExposureCompensation() {
        int min = parameters.getMinExposureCompensation();
        if (VERBOSE) {
            Log.i(TAG, "MinExposureCompensation:" + min);
        }
        return min;
    }
}
