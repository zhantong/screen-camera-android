package cn.edu.nju.cs.screencamera;

import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;


import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by zhantong on 16/4/29.
 */
public class CameraToFile extends StreamToFile {
    private static final String TAG = "StreamToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log
    private CameraPreview mPreview;
    public CameraToFile(TextView debugView, TextView infoView, Handler handler, BarcodeFormat format,String truthFilePath) {
        super(debugView, infoView, handler,format,truthFilePath);
    }
    public void toFile(String fileName,CameraPreview mPreview){
        Log.i(TAG,"process camera");
        LinkedBlockingQueue<byte[]> rev = new LinkedBlockingQueue<>();
        Camera.Size previewSize=mPreview.getPreviewSize();
        int frameWidth=previewSize.width;
        int frameHeight=previewSize.height;
        this.mPreview=mPreview;
        mPreview.start(rev);
        streamToFile(rev,frameWidth,frameHeight,fileName);
    }
    public int getImgColorType(){
        return 1;
    }
    public void notFound(int fileByteNum){
        if(fileByteNum==-1){
            Log.d(TAG,"camera focusing");
            mPreview.focus();
        }
    }
    public void crcCheckFailed(){
        Log.d(TAG, "camera focusing");
        mPreview.focus();
    }
    public void beforeDataDecoded(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                mPreview.stop();
            }
        });
        Log.d(TAG,"stopped camera preview");
    }
}
