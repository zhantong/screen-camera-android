package cn.edu.nju.cs.screencamera;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by zhantong on 2016/11/28.
 */

public class ShiftCodeCamera extends ShiftCodeStream implements ShiftCodeStream.Callback {
    private static final String TAG="ShiftCode Camera";
    private CameraPreview mPreview;
    public ShiftCodeCamera(CameraPreview mPreview,Map<DecodeHintType, ?> hints) {
        super(hints);
        setCallback(this);
        this.mPreview=mPreview;
        LinkedBlockingQueue<RawImage> frameQueue = new LinkedBlockingQueue<>();
        mPreview.start(frameQueue);
        try {
            stream(frameQueue);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBarcodeNotFound() {
        Log.d(TAG, "camera focusing");
        mPreview.focus();
    }

    @Override
    public void onCRCCheckFailed() {
        Log.d(TAG, "camera focusing");
        mPreview.focus();
    }
    @Override
    public void onBeforeDataDecoded() {
        Runnable task=new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    mPreview.stop();
                    notify();
                }
            }
        };
        new Handler(Looper.getMainLooper()).post(task);
        synchronized (task){
            try {
                task.wait();
            } catch (InterruptedException e) {
                Log.w(TAG, "wait was interrupted");
            }
        }
    }
}
