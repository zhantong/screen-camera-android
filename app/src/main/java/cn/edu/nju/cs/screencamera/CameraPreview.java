package cn.edu.nju.cs.screencamera;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 系统相机操作,主要是对一些继承方法的重载.
 * 在这里控制相机对焦,捕获预览帧并加入队列等.
 * 注意需要与CameraSettings配合.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private static final String TAG = "CameraPreview";//log tag
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private LinkedBlockingQueue<RawImage> frameQueue;
    private boolean pause;
    Camera.Size previewSize;

    public CameraPreview(Context context) {
        super(context);
        pause = true;
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public void start(LinkedBlockingQueue<RawImage> frames) {
        previewSize = mCamera.getParameters().getPreviewSize();
        this.frameQueue = frames;
        pause = false;
    }

    public Camera.Size getPreviewSize() {
        return mCamera.getParameters().getPreviewSize();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        getCameraInstance();
        mCamera.setPreviewCallback(this);
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public Camera getCameraInstance() {
        if (mCamera == null) {
            try {
                mCamera = Camera.open();
            } catch (Exception e) {
                Log.d(TAG, "camera is not available");
            }
        }
        return mCamera;
    }

    /**
     * surface销毁时释放相机
     *
     * @param holder SurfaceHolder
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHolder.removeCallback(this);
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    /**
     * 执行此方法将会使相机对焦,且线程睡眠2秒,避免重复对焦过快.
     * 注意线程睡眠时,onPreviewFrame仍会正常进行,需要用额外变量控制
     */
    public void focus() {
        pause = true;
        mCamera.autoFocus(null);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        pause = false;
    }

    /**
     * 最初时,触发surfaceCreated也会随即触发surfaceChanged.
     * 这里控制在新的线程中开始预览
     *
     * @param holder 默认参数
     * @param format 默认参数
     * @param w      默认参数
     * @param h      默认参数
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    }

    /**
     * 每到达一个预览帧时,即将此帧数据加入队列.
     * 帧数据一般为NV21格式
     *
     * @param data   预览帧数据
     * @param camera 默认参数
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (!pause) {
            try {
                frameQueue.put(new RawImage(data, previewSize.width, previewSize.height, RawImage.COLOR_TYPE_YUV));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 主动控制释放相机资源
     */
    public void stop() {
        ((ViewGroup) getParent()).removeView(this);
    }

    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed) {
            ViewGroup parent = ((ViewGroup) getParent());
            Rect rect = new Rect();

            parent.getLocalVisibleRect(rect);
            int width = rect.width();
            int height = rect.height();

            Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
            int previewWidth = previewSize.width;
            int previewHeight = previewSize.height;

            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height / previewHeight;

                layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width / previewWidth;
                layout(0, (height - scaledChildHeight) / 2,
                        width, (height + scaledChildHeight) / 2);
            }
        }
    }

}
