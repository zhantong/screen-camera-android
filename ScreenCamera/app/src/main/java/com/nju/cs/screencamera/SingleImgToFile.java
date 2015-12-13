package com.nju.cs.screencamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.nio.ByteBuffer;

/**
 * Created by zhantong on 15/12/13.
 */
public class SingleImgToFile extends MediaToFile {
    private static final String TAG = "ImgToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log
    /**
     * 构造函数,获取必须的参数
     *
     * @param debugView 实例
     * @param infoView  实例
     * @param handler   实例
     * @param imgWidth  图像宽度
     * @param imgHeight 图像高度
     */
    public SingleImgToFile(TextView debugView, TextView infoView, Handler handler, int imgWidth, int imgHeight) {
        super(debugView,infoView,handler,imgWidth,imgHeight);
    }
    /**
     * 对单个图片进行解码识别二维码
     * 注意这个方法只是拿来测试识别算法等
     *
     * @param filePath 图片路径
     */
    public void singleImg(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bitmap.getWidth() * bitmap.getHeight() * 4);
        bitmap.copyPixelsToBuffer(byteBuffer);
        bitmap.recycle();
        updateInfo("正在识别...");
        RGBMatrix rgbMatrix;
        try {
            rgbMatrix = new RGBMatrix(byteBuffer.array(), bitmap.getWidth(), bitmap.getHeight());
            rgbMatrix.perspectiveTransform(0, 0, barCodeWidth, 0, barCodeWidth, barCodeWidth, 0, barCodeWidth);
            rgbMatrix.frameIndex = getIndex(rgbMatrix);
        } catch (NotFoundException e) {
            Log.d(TAG, e.getMessage());
            return;
        } catch (CRCCheckException e) {
            Log.d(TAG, "CRC check failed");
            return;
        }
        Log.d(TAG, "frame index:" + rgbMatrix.frameIndex);
        int frameAmount;
        try {
            frameAmount = getFrameAmount(rgbMatrix);
        } catch (CRCCheckException e) {
            Log.d(TAG, "CRC check failed");
            return;
        }
        Log.d(TAG, "frame amount:" + frameAmount);
        byte[] stream;
        try {
            stream = imgToArray(rgbMatrix);
        } catch (ReedSolomonException e) {
            Log.d(TAG, e.getMessage());
            return;
        }
        Log.i(TAG, "done!");
        updateInfo("识别完成!");
    }
}
