package com.nju.cs.screencamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.decoder.SourceBlockDecoder;
import net.fec.openrq.parameters.FECParameters;

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
        ArrayDataDecoder dataDecoder=null;
        int fileByteNum=-1;

        try {
            rgbMatrix = new RGBMatrix(byteBuffer.array(), bitmap.getWidth(), bitmap.getHeight());
            rgbMatrix.perspectiveTransform(0, 0, barCodeWidth, 0, barCodeWidth, barCodeHeight, 0, barCodeHeight);
        } catch (NotFoundException e) {
            Log.d(TAG, e.getMessage());
            return;
        }
        for(int i=0;i<2;i++) {
            rgbMatrix.reverse=!rgbMatrix.reverse;
            if(fileByteNum==-1){
                try {
                    fileByteNum = getFileByteNum(rgbMatrix);
                }catch (CRCCheckException e){
                    System.out.println("CRC check failed");
                    continue;
                }
                if(fileByteNum==0){
                    fileByteNum=-1;
                    continue;
                }
                int length=contentLength*contentLength/8-ecNum*ecLength/8-8;
                FECParameters parameters = FECParameters.newParameters(fileByteNum, length, 1);
                System.out.println(parameters.toString());
                dataDecoder = OpenRQ.newDecoder(parameters, 0);
            }
            byte[] current;
            try {
                current = getContent(rgbMatrix);
            }catch (ReedSolomonException e){
                System.out.println("error correction failed");
                continue;
            }
            EncodingPacket encodingPacket = dataDecoder.parsePacket(current, true).value();
            System.out.println("source block number:"+encodingPacket.sourceBlockNumber()+"\tencoding symbol ID:"+encodingPacket.encodingSymbolID()+"\t"+encodingPacket.symbolType());
            dataDecoder.sourceBlock(encodingPacket.sourceBlockNumber()).putEncodingPacket(encodingPacket);
        }
        if(fileByteNum!=-1) {
            checkSourceBlockStatus(dataDecoder);
            System.out.println("is decoded:" + dataDecoder.isDataDecoded());
        }
        rgbMatrix = null;
    }
    private void checkSourceBlockStatus(ArrayDataDecoder dataDecoder){
        for (SourceBlockDecoder sourceBlockDecoder : dataDecoder.sourceBlockIterable()) {
            System.out.println("source block number:" + sourceBlockDecoder.sourceBlockNumber() + "\tstate:" + sourceBlockDecoder.latestState());
        }
    }
}
