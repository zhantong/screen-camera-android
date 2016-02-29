package com.nju.cs.screencamera;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.decoder.SourceBlockDecoder;
import net.fec.openrq.parameters.FECParameters;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by zhantong on 16/2/29.
 */
public class StreamToFile extends MediaToFile {
    private static final String TAG = "StreamToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log
    public StreamToFile(TextView debugView, TextView infoView, Handler handler) {
        super(debugView, infoView, handler);
    }
    public void toFile(LinkedBlockingQueue<byte[]> imgs, String fileName,String videoFilePath){
        int[] widthAndHeight=frameWidthAndHeight(videoFilePath);
        int frameWidth=widthAndHeight[0];
        int frameHeight=widthAndHeight[1];
        streamToFile(imgs, frameWidth, frameHeight, fileName, null);
    }
    public void toFile(LinkedBlockingQueue<byte[]> imgs, String fileName, int frameWidth, int frameHeight, CameraPreview mPreview) {
        streamToFile(imgs, frameWidth, frameHeight, fileName, mPreview);
    }
    private void streamToFile(LinkedBlockingQueue<byte[]> imgs,int frameWidth,int frameHeight,String fileName,CameraPreview mPreview) {
        ArrayDataDecoder dataDecoder=null;
        int fileByteNum=-1;
        int count = 0;
        int lastSuccessIndex = 0;
        int frameAmount = 0;
        byte[] img = {};
        Matrix matrix;
        int index = 0;
        while (true) {
            count++;
            updateInfo("正在识别...");
            try {
                //System.out.println("queue empty:"+imgs.isEmpty());
                img = imgs.take();
            } catch (InterruptedException e) {
                Log.d(TAG, e.getMessage());
            }
            updateDebug(index, lastSuccessIndex, frameAmount, count);
            try {
                if(mPreview!=null){
                    matrix = new Matrix(img, frameWidth, frameHeight);
                }
                else {
                    matrix = new RGBMatrix(img, frameWidth, frameHeight);
                }
                matrix.perspectiveTransform(0, 0, barCodeWidth, 0, barCodeWidth, barCodeHeight, 0, barCodeHeight);
            } catch (NotFoundException e) {
                Log.d(TAG, e.getMessage());
                if(fileByteNum==-1&&mPreview!=null){
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
                    if(mPreview!=null) {
                        mPreview.stop();
                    }
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
    private int[] frameWidthAndHeight(String videoFilePath){
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
        return new int[] {imgWidth,imgHeight};
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
