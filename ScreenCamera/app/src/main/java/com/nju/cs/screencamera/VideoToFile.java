package com.nju.cs.screencamera;

import android.graphics.Path;
import android.media.MediaExtractor;
import android.media.MediaFormat;
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
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 识别视频中的二维码,处理后写入文件
 */
public class VideoToFile extends MediaToFile {
    private static final String TAG = "VideoToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log

    /**
     * 构造函数,获取必须的参数
     *
     * @param debugView 实例
     * @param infoView  实例
     * @param handler   实例
     */
    public VideoToFile(TextView debugView, TextView infoView, Handler handler) {
        super(debugView, infoView, handler);
    }

    /**
     * 对视频解码的帧队列处理
     * 所有帧都识别成功后写入到文件
     *
     * @param imgs 帧队列
     * @param file 需要写入的文件
     */
    public void videoToFile(String videoFilePath, LinkedBlockingQueue<byte[]> imgs, File file) {

        ArrayDataDecoder dataDecoder=null;
        int fileByteNum=-1;


        File inputFile = new File(videoFilePath);
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(inputFile.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        int trackIndex = selectTrack(extractor);
        extractor.selectTrack(trackIndex);
        MediaFormat format = extractor.getTrackFormat(trackIndex);
        int imgWidth = format.getInteger(MediaFormat.KEY_WIDTH);
        int imgHeight = format.getInteger(MediaFormat.KEY_HEIGHT);

        int count = 0;
        int lastSuccessIndex = 0;
        int frameAmount = 0;
        byte[] img = {};
        RGBMatrix rgbMatrix;
        int index = 0;
        while (true) {
            count++;
            updateInfo("正在识别...");
            try {
                img = imgs.take();
            } catch (InterruptedException e) {
                Log.d(TAG, e.getMessage());
            }
            updateDebug(index, lastSuccessIndex, frameAmount, count);
            try {
                rgbMatrix = new RGBMatrix(img, imgWidth, imgHeight);
                rgbMatrix.perspectiveTransform(0, 0, barCodeWidth, 0, barCodeWidth, barCodeWidth, 0, barCodeWidth);
                //getFileByteNum(rgbMatrix);
            } catch (NotFoundException e) {
                Log.d(TAG, e.getMessage());
                continue;
            }
            System.out.println("current:"+count);
            for(int i=0;i<2;i++) {
                rgbMatrix.reverse=!rgbMatrix.reverse;
                if(fileByteNum==-1){
                    try {
                        fileByteNum = getFileByteNum(rgbMatrix);
                        if(fileByteNum==0){
                            fileByteNum=-1;
                            continue;
                        }
                        int length=contentLength*contentLength/8-ecByteNum-8;
                        FECParameters parameters = FECParameters.newParameters(fileByteNum, length, fileByteNum/(length*10)+1);
                        System.out.println(parameters.toString());
                        dataDecoder = OpenRQ.newDecoder(parameters, 0);
                    }catch (CRCCheckException e){
                        System.out.println("CRC check failed");
                    }
                }
                byte[] current;
                try {
                    current = getContent(rgbMatrix);
                }catch (ReedSolomonException e){
                    System.out.println("error correction failed");
                    continue;
                }
                try {
                    EncodingPacket encodingPacket = dataDecoder.parsePacket(current, true).value();
                    System.out.println("source block number:"+encodingPacket.sourceBlockNumber()+"\tencoding symbol ID:"+encodingPacket.encodingSymbolID()+"\t"+encodingPacket.symbolType());
                    dataDecoder.sourceBlock(encodingPacket.sourceBlockNumber()).putEncodingPacket(encodingPacket);
                }catch (Exception e){
                    e.printStackTrace();
                    continue;
                }
            }
            if(fileByteNum!=-1) {
                for (SourceBlockDecoder sourceBlockDecoder : dataDecoder.sourceBlockIterable()) {
                    System.out.println("source block number:" + sourceBlockDecoder.sourceBlockNumber() + "\tstate:" + sourceBlockDecoder.latestState());
                }
                System.out.println("is decoded:" + dataDecoder.isDataDecoded());
                if(dataDecoder.isDataDecoded()){
                    break;
                }
            }
            rgbMatrix = null;
        }

        byte[] out=dataDecoder.dataArray();

        OutputStream os;
        try {
            os = new FileOutputStream(file);
            os.write(out);
            os.close();
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
        String sha1=FileVerification.fileToSHA1(file.getAbsolutePath());
        System.out.println(sha1);

        /*
        updateInfo("识别完成!正在写入文件");
        Log.d("videoToFile", "total length:" + buffer.size());
        bufferToFile(buffer, file);
        updateInfo("写入文件成功!");
        */
    }
    private int selectTrack(MediaExtractor extractor) {
        // Select the first video track we find, ignore the rest.
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                if (VERBOSE) {
                    Log.d(TAG, "Extractor selected track " + i + " (" + mime + "): " + format);
                }
                return i;
            }
        }
        return -1;
    }
}
