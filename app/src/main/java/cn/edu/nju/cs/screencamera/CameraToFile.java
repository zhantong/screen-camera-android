package cn.edu.nju.cs.screencamera;

import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;


import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by zhantong on 16/4/29.
 */
public class CameraToFile extends StreamToFile {
    private static final String TAG = "StreamToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log
    private static final int COLOR_TYPE=Matrix.COLOR_TYPE_YUV;
    private CameraPreview mPreview;
    public CameraToFile(Handler handler, BarcodeFormat format,String truthFilePath) {
        super(handler,format,truthFilePath);
    }
    public void toFile(String fileName,CameraPreview mPreview){
        if(VERBOSE){Log.i(TAG,"process camera");}
        LinkedBlockingQueue<byte[]> rev = new LinkedBlockingQueue<>();
        Camera.Size previewSize=mPreview.getPreviewSize();
        int frameWidth=previewSize.width;
        int frameHeight=previewSize.height;
        this.mPreview=mPreview;
        mPreview.start(rev);
        streamToFile(rev,frameWidth,frameHeight,fileName);
    }
    public int getImgColorType(){
        return COLOR_TYPE;
    }
    public void notFound(int fileByteNum){
        if(fileByteNum==-1){
            if(VERBOSE){Log.d(TAG,"camera focusing");}
            mPreview.focus();
        }
    }
    public void crcCheckFailed(){
        if(VERBOSE){Log.d(TAG, "camera focusing");}
        mPreview.focus();
    }
    public void beforeDataDecoded(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                mPreview.stop();
            }
        });
        if(VERBOSE){Log.d(TAG,"stopped camera preview");}
    }
}
