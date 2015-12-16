package com.nju.cs.screencamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.nio.ByteBuffer;

/**
 * 识别图片中的二维码,测试用
 */
public class SingleImgToFile extends MediaToFile {
    private static final String TAG = "SingleImgToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log

    /**
     * 构造函数,获取必须的参数
     *
     * @param debugView 实例
     * @param infoView  实例
     * @param handler   实例
     */
    public SingleImgToFile(TextView debugView, TextView infoView, Handler handler) {
        super(debugView, infoView, handler);
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
        RGBMatrix rgbMatrix=null;
        byte[] stream=null;
        try {
            rgbMatrix = new RGBMatrix(byteBuffer.array(), bitmap.getWidth(), bitmap.getHeight());
            rgbMatrix.perspectiveTransform(0, 0, barCodeWidth, 0, barCodeWidth, barCodeWidth, 0, barCodeWidth);
            //rgbMatrix.frameIndex = getIndex(rgbMatrix);
        } catch (NotFoundException e) {
            Log.d(TAG, e.getMessage());
        }
        /*
        catch (CRCCheckException e) {
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
        */

        try {
            stream=imgToArray(rgbMatrix);
            //stream = imgToIntArray(rgbMatrix);
        } catch (ReedSolomonException e) {
            Log.d(TAG, e.getMessage());
        }
        Log.i(TAG, "done!");
        updateInfo("识别完成!");
        System.out.println(stream);
    }
    public void driver(){
        String filePath="/storage/emulated/0/test/frame-19.png";
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bitmap.getWidth() * bitmap.getHeight() * 4);
        bitmap.copyPixelsToBuffer(byteBuffer);
        bitmap.recycle();
        updateInfo("正在识别...");
        RGBMatrix rgbMatrix=null;
        int startOffset = frameBlackLength + frameVaryLength;
        int stopOffset = startOffset + contentLength;
        int contentByteNum = contentLength * contentLength / 8;
        int[] stream=null;
        try {
            rgbMatrix = new RGBMatrix(byteBuffer.array(), bitmap.getWidth(), bitmap.getHeight());
            rgbMatrix.perspectiveTransform(0, 0, barCodeWidth, 0, barCodeWidth, barCodeWidth, 0, barCodeWidth);
            //rgbMatrix.frameIndex = getIndex(rgbMatrix);
        } catch (NotFoundException e) {
            Log.d(TAG, e.getMessage());
        }
        try {
            //imgToArray(rgbMatrix);
            BinaryMatrix binaryMatrix = rgbMatrix.sampleGrid(barCodeWidth, barCodeWidth);

            int realByteNum = contentByteNum - ecByteNum;
            int[] result = new int[contentByteNum];
            binaryMatrix.toArray(startOffset, startOffset, stopOffset, stopOffset, result);
            ReedSolomonDecoder decoder = new ReedSolomonDecoder(GenericGF.QR_CODE_FIELD_256);
            try {
                decoder.decode(result, ecByteNum);
            } catch (Exception e) {
                throw new ReedSolomonException("error correcting failed");
            }
            stream = result;
        } catch (ReedSolomonException e) {
            Log.d(TAG, e.getMessage());
        }
        Log.i(TAG, "done!");
        updateInfo("识别完成!");
        StringBuffer stringBuffer=new StringBuffer();
        for(int b:stream){
            String s=Integer.toBinaryString(b);
            int temp=Integer.parseInt(s);
            stringBuffer.append(String.format("%1$08d",temp));
        }

        filePath="/storage/emulated/0/test/frame-21.png";
        bitmap = BitmapFactory.decodeFile(filePath, options);
        byteBuffer = ByteBuffer.allocateDirect(bitmap.getWidth() * bitmap.getHeight() * 4);
        bitmap.copyPixelsToBuffer(byteBuffer);
        bitmap.recycle();
        try {
            rgbMatrix = new RGBMatrix(byteBuffer.array(), bitmap.getWidth(), bitmap.getHeight());
            rgbMatrix.perspectiveTransform(0, 0, barCodeWidth, 0, barCodeWidth, barCodeWidth, 0, barCodeWidth);
            //rgbMatrix.frameIndex = getIndex(rgbMatrix);
        } catch (NotFoundException e) {
            Log.d(TAG, e.getMessage());
        }
        try {
            //imgToArray(rgbMatrix);
            BinaryMatrix binaryMatrix = rgbMatrix.sampleGrid(barCodeWidth, barCodeWidth);

            int realByteNum = contentByteNum - ecByteNum;
            int[] result = new int[contentByteNum];
            binaryMatrix.toArray(startOffset, startOffset, stopOffset, stopOffset, result);
            ReedSolomonDecoder decoder = new ReedSolomonDecoder(GenericGF.QR_CODE_FIELD_256);
            try {
                decoder.decode(result, ecByteNum);
            } catch (Exception e) {
                throw new ReedSolomonException("error correcting failed");
            }
            stream = result;
        } catch (ReedSolomonException e) {
            Log.d(TAG, e.getMessage());
        }
        Log.i(TAG, "done!");
        updateInfo("识别完成!");
        StringBuffer stringBuffer2=new StringBuffer();
        for(int b:stream){
            String s=Integer.toBinaryString(b);
            int temp=Integer.parseInt(s);
            stringBuffer2.append(String.format("%1$08d",temp));
        }


        filePath="/storage/emulated/0/test/frame-20.png";
        bitmap = BitmapFactory.decodeFile(filePath, options);
        byteBuffer = ByteBuffer.allocateDirect(bitmap.getWidth() * bitmap.getHeight() * 4);
        bitmap.copyPixelsToBuffer(byteBuffer);
        bitmap.recycle();
        try {
            rgbMatrix = new RGBMatrix(byteBuffer.array(), bitmap.getWidth(), bitmap.getHeight());
        }catch (Exception e){
            e.printStackTrace();
        }
        rgbMatrix.perspectiveTransform(0, 0, barCodeWidth, 0, barCodeWidth, barCodeWidth, 0, barCodeWidth);
        BinaryMatrix binaryMatrix = rgbMatrix.sampleGrid(barCodeWidth, barCodeWidth);
        int[] data=binaryMatrix.pixels;
        System.out.println(stringBuffer.length()+" "+data.length);
        int index = 0;
        int countWrong=0;
        for (int j = startOffset; j < stopOffset; j++) {
            int jValue=j*binaryMatrix.width();
            for (int i = startOffset; i < stopOffset; i++) {
                int c=Integer.parseInt(stringBuffer2.charAt(index)+"");
                if(data[jValue+i]==-1){
                    int t=Integer.parseInt(stringBuffer.charAt(index)+"");
                    if(t==0){
                        data[jValue+i]=1;
                    }else if(t==1){
                        data[jValue+i]=0;
                    }else{
                        System.out.println("WRONG");
                    }
                    System.out.println(data[jValue+i]+" "+c);
                }
                if(data[jValue+i]!=c){
                    countWrong++;
                }
                //System.out.println(data[jValue+i]+" "+c);
                index++;
            }
        }
        System.out.println("countWrong:"+countWrong);
        binaryMatrix.pixels=data;
        binaryMatrix.print();
        int[] result = new int[contentByteNum];
        binaryMatrix.toArray(startOffset, startOffset, stopOffset, stopOffset, result);
        ReedSolomonDecoder decoder = new ReedSolomonDecoder(GenericGF.QR_CODE_FIELD_256);
        try {
            decoder.decode(result, ecByteNum);
        } catch (ReedSolomonException e) {
            System.out.println("error correcting failed");
        }
    }
    public int[] imgToIntArray(Matrix matrix) throws ReedSolomonException {
        BinaryMatrix binaryMatrix = matrix.sampleGrid(barCodeWidth, barCodeWidth);
        binaryMatrix.print();
        return binaryMatrix.pixels;
    }
}
