package com.nju.cs.screencamera;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 识别视频中的二维码,处理后写入文件
 */
public class VideoToFile extends MediaToFile {
    private static final String TAG = "VideoToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log
    String lastRow=null;
    StringBuffer lastBuffer=null;

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
        int startOffset = frameBlackLength + frameVaryLength;
        int stopOffset = startOffset + contentLength;
        int contentByteNum = contentLength * contentLength / 8;
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
        List<byte[]> buffer = new LinkedList<>();
        byte[] img = {};
        RGBMatrix rgbMatrix;
        byte[] stream;
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
                //test(rgbMatrix);
            } catch (NotFoundException e) {
                Log.d(TAG, e.getMessage());
                continue;
            }
            /*
            try {
                rgbMatrix.reverse=false;
                int unre = getIndex(rgbMatrix);
                rgbMatrix.reverse=true;
                int re=getIndex(rgbMatrix);
                if(unre<re){
                    rgbMatrix.reverse=true;
                }else{
                    rgbMatrix.reverse=false;
                }
                //System.out.println(unre+" "+re);
            }catch (CRCCheckException e){
            }
            */
            for(int i=0;i<2;i++) {
                rgbMatrix.reverse=!rgbMatrix.reverse;
                System.out.println("current:"+count);
                test(rgbMatrix);
                test2(rgbMatrix);
                /*
                try{
                    rgbMatrix.frameIndex = getIndex(rgbMatrix);
                }catch (CRCCheckException e){
                    Log.d(TAG, "failed to get frame index: CRC check failed");
                    continue;
                }
                index = rgbMatrix.frameIndex;
                Log.i("frame " + index + "/" + count, "processing...");

                if (lastSuccessIndex == index) {
                    Log.i("frame " + index + "/" + count, "same frame index!");
                    continue;
                } else if (index - lastSuccessIndex != 1) {
                    Log.i("frame " + index + "/" + count, "bad frame index!");
                    continue;
                }
                */
                //BinaryMatrix binaryMatrix = rgbMatrix.sampleGrid(barCodeWidth, barCodeWidth);
            /*
            int[] data=binaryMatrix.pixels;
            index = 0;
            int c=0;
            if(lastBuffer!=null) {
                for (int j = startOffset; j < stopOffset; j++) {
                    int jValue = j * binaryMatrix.width();
                    for (int i = startOffset; i < stopOffset; i++) {
                        if (data[jValue + i] == -1) {
                            System.out.println(i+" "+j);
                            c++;
                            int t = Integer.parseInt(lastBuffer.charAt(index) + "");
                            if (t == 0) {
                                data[jValue + i] = 1;
                            } else if (t == 1) {
                                data[jValue + i] = 0;
                            } else {
                                System.out.println("WRONG");
                            }
                        }
                        //System.out.println(data[jValue+i]+" "+c);
                        index++;
                    }
                }
            }

            System.out.println("reverse count:"+c);
            binaryMatrix.pixels=data;
            */
            /*
            int[] result = new int[contentByteNum];
            binaryMatrix.toArray(startOffset, startOffset, stopOffset, stopOffset, result);
            ReedSolomonDecoder decoder = new ReedSolomonDecoder(GenericGF.QR_CODE_FIELD_256);
            try {
                decoder.decode(result, ecByteNum);
            } catch (ReedSolomonException e) {
                System.out.println("error correcting failed");
                continue;
            }
            StringBuffer stringBuffer=new StringBuffer();
            for(int b:result){
                String s=Integer.toBinaryString(b);
                int temp=Integer.parseInt(s);
                stringBuffer.append(String.format("%1$08d",temp));
            }
            lastBuffer=stringBuffer;
            */
                /*
                try {
                    stream = imgToArray(rgbMatrix);
                } catch (ReedSolomonException e) {
                    Log.d(TAG, e.getMessage());
                    continue;
                }
                //System.out.println("done "+count);

                buffer.add(stream);

                lastSuccessIndex = index;
                Log.i("frame " + index + "/" + count, "done!");
                updateDebug(index, lastSuccessIndex, frameAmount, count);


                if (lastSuccessIndex == frameAmount) {
                    break;
                }

                if (frameAmount == 0) {
                    try {
                        frameAmount = getFrameAmount(rgbMatrix);
                    } catch (CRCCheckException e) {
                        Log.d(TAG, "failed to get frame amount: CRC check failed");
                        continue;
                    }
                }
                */
            }
            rgbMatrix = null;
        }
        /*
        updateInfo("识别完成!正在写入文件");
        Log.d("videoToFile", "total length:" + buffer.size());
        bufferToFile(buffer, file);
        updateInfo("写入文件成功!");
        */
    }
    /*
    public int getIndex(Matrix matrix) throws CRCCheckException {
        String row = matrix.sampleRow(barCodeWidth, barCodeWidth, frameBlackLength);
        if(lastRow!=null){
            StringBuilder stringBuilder = new StringBuilder();
            for(int i=0;i<row.length();i++){
                if(row.charAt(i)=='2'){
                    if(lastRow.charAt(i)=='0'){
                        stringBuilder.append('1');
                    }else{
                        stringBuilder.append('0');
                    }
                }else{
                    stringBuilder.append(row.charAt(i));
                }
            }
            row=stringBuilder.toString();
        }
        if (VERBOSE) {
            Log.d(TAG, "index row:" + row);
        }
        int index = Integer.parseInt(row.substring(frameBlackLength, frameBlackLength + 16), 2);
        int crc = Integer.parseInt(row.substring(frameBlackLength + 16, frameBlackLength + 24), 2);
        int truth = CRC8.calcCrc8(index);
        if (VERBOSE) {
            Log.d(TAG, "CRC check: index:" + index + " CRC:" + crc + " truth:" + truth);
        }
        if (crc != truth) {
            System.out.println("lastRow:"+lastRow+" row:"+row);
            throw CRCCheckException.getNotFoundInstance();
        }
        lastRow=row;
        return index;
    }
    */
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
