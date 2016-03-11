package cn.edu.nju.cs.screencamera;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

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
    private LinkedBlockingQueue<byte[]> frames;
    private boolean pause = false;//对焦需要一定时间,防止队列溢出

    /**
     * 构造函数,必要的环境设置
     *
     * @param context 上下文
     * @param frames  队列,捕捉到的帧加入此队列
     */
    public CameraPreview(Context context, LinkedBlockingQueue<byte[]> frames) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
        this.frames = frames;
    }

    /**
     * surface创建时即获得相机,设置环境和相机参数.
     * 相机参数由CameraSetting类提前设定好.
     *
     * @param holder SurfaceHolder
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();
        mCamera.setPreviewCallback(this);
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            Log.d(TAG, "preview failed");
        }
        mCamera.setParameters(CameraSettings.parameters);
    }

    /**
     * surface销毁时释放相机
     *
     * @param holder SurfaceHolder
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
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
        Thread preview_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                mCamera.startPreview();
            }
        }, "preview_thread");
        preview_thread.start();
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
                frames.put(data);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 主动控制释放相机资源
     */
    public void stop() {
        mHolder.removeCallback(this);
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }
}
