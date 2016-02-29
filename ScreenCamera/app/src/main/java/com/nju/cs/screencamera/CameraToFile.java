package com.nju.cs.screencamera;

import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.decoder.SourceBlockDecoder;
import net.fec.openrq.parameters.FECParameters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 识别相机中的二维码,处理后写入文件
 */
public class CameraToFile extends MediaToFile {
    private static final String TAG = "CameraToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log
    private int imgWidth;//相机分辨率宽度
    private int imgHeight;//相机分辨率高度
    private CameraPreview mPreview;//相机

    /**
     * 构造函数,获取必须的参数
     *
     * @param debugView 实例
     * @param infoView  实例
     * @param handler   实例
     * @param imgWidth  相机分辨率宽度
     * @param imgHeight 相机分辨率高度
     * @param mPreview  实例
     */
    public CameraToFile(TextView debugView, TextView infoView, Handler handler, int imgWidth, int imgHeight, CameraPreview mPreview) {
        super(debugView, infoView, handler);
        this.imgWidth = imgWidth;
        this.imgHeight = imgHeight;
        this.mPreview = mPreview;
    }

    /**
     * 从队列中取出预览帧进行处理,根据处理情况控制相机
     * 所有帧都识别成功后写入到文件
     *
     * @param imgs 帧队列
     * @param fileName 需要写入的文件
     */
    public void cameraToFile(LinkedBlockingQueue<byte[]> imgs, String fileName) {
        int count = 0;
        int lastSuccessIndex = 0;
        int frameAmount = 0;
        byte[] img = {};
        int index = 0;
        Matrix matrix;
        ArrayDataDecoder dataDecoder=null;
        int fileByteNum=-1;
        while (true) {
            count++;
            updateInfo("正在识别...");
            try {
                System.out.println("queue empty:"+imgs.isEmpty());
                img = imgs.take();
            } catch (InterruptedException e) {
                Log.d(TAG, e.getMessage());
            }
            updateDebug(index, lastSuccessIndex, frameAmount, count);
            try {
                matrix = new Matrix(img, imgWidth, imgHeight);
                matrix.perspectiveTransform(0, 0, barCodeWidth, 0, barCodeWidth, barCodeHeight, 0, barCodeHeight);
            } catch (NotFoundException e) {
                Log.d(TAG, e.getMessage());
                if(fileByteNum==-1){
                    mPreview.focus();
                }
                continue;
            }
            System.out.println("current:"+count);
            for(int i=0;i<2;i++) {
                matrix.reverse=!matrix.reverse;
                if(fileByteNum==-1){
                    try {
                        fileByteNum = getFileByteNum(matrix);
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
                    current = getContent(matrix);
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
                if(dataDecoder.isDataDecoded()){
                    mPreview.stop();
                    break;
                }
            }
            matrix = null;
        }
        byte[] out=dataDecoder.dataArray();
        String sha1=FileVerification.bytesToSHA1(out);
        System.out.println("SHA-1 verification:"+sha1);
        bytesToFile(out,fileName);
    }
    private void checkSourceBlockStatus(ArrayDataDecoder dataDecoder){
        for (SourceBlockDecoder sourceBlockDecoder : dataDecoder.sourceBlockIterable()) {
            System.out.println("source block number:" + sourceBlockDecoder.sourceBlockNumber() + "\tstate:" + sourceBlockDecoder.latestState());
        }
    }
}
