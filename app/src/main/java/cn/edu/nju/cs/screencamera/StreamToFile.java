package cn.edu.nju.cs.screencamera;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

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
        Log.i(TAG,"process video file");
        int[] widthAndHeight=frameWidthAndHeight(videoFilePath);
        int frameWidth=widthAndHeight[0];
        int frameHeight=widthAndHeight[1];
        streamToFile(imgs, frameWidth, frameHeight, fileName, null);
    }
    public void toFile(LinkedBlockingQueue<byte[]> imgs, String fileName, int frameWidth, int frameHeight, CameraPreview mPreview) {
        Log.i(TAG,"process camera");
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
        int[] border=null;
        int index = 0;
        while (true) {
            count++;
            updateInfo("正在识别...");
            try {
                if(VERBOSE){Log.d(TAG,"is queue empty:"+imgs.isEmpty());}
                img = imgs.take();
            } catch (InterruptedException e) {
                Log.d(TAG, e.getMessage());
            }
            updateDebug(index, lastSuccessIndex, frameAmount, count);
            try {
                if(mPreview!=null){
                    matrix = new Matrix(img, frameWidth, frameHeight,border);
                }
                else {
                    matrix = new RGBMatrix(img, frameWidth, frameHeight,border);
                }
                matrix.perspectiveTransform(0, 0, barCodeWidth, 0, barCodeWidth, barCodeHeight, 0, barCodeHeight);
            } catch (NotFoundException e) {
                Log.d(TAG, e.getMessage());
                if(fileByteNum==-1&&mPreview!=null){
                    Log.d(TAG,"camera focusing");
                    mPreview.focus();
                }
                border=null;
                continue;
            }
            Log.i(TAG, "current frame:" + count);
            for(int i=1;i<3;i++) {
                if(i==2){
                    if(!matrix.isMixed){
                        break;
                    }
                    matrix.reverse=true;
                }
                Log.i(TAG,"try "+i+" :");
                if(fileByteNum==-1){
                    try {
                        fileByteNum = getFileByteNum(matrix);
                    }catch (CRCCheckException e){
                        Log.d(TAG, "head CRC check failed");
                        Log.d(TAG,"camera focusing");
                        mPreview.focus();
                        continue;
                    }
                    if(fileByteNum==0){
                        Log.d(TAG,"wrong file byte number");
                        fileByteNum=-1;
                        continue;
                    }
                    Log.i(TAG,"file is "+fileByteNum+" bytes");
                    int length=contentLength*contentLength/8-ecNum*ecLength/8-8;
                    FECParameters parameters = FECParameters.newParameters(fileByteNum, length, 1);
                    Log.d(TAG, "RaptorQ parameters:" + parameters.toString());
                    dataDecoder = OpenRQ.newDecoder(parameters, 0);
                }
                byte[] current;
                try {
                    current = getContent(matrix);
                }catch (ReedSolomonException e){
                    Log.d(TAG, "content error correction failed");
                    continue;
                }
                EncodingPacket encodingPacket = dataDecoder.parsePacket(current, true).value();
                Log.i(TAG, "got 1 source block: source block number:" + encodingPacket.sourceBlockNumber() + "\tencoding symbol ID:" + encodingPacket.encodingSymbolID() + "\t" + encodingPacket.symbolType());
                dataDecoder.sourceBlock(encodingPacket.sourceBlockNumber()).putEncodingPacket(encodingPacket);
            }
            if(fileByteNum!=-1) {
                //checkSourceBlockStatus(dataDecoder);
                Log.d(TAG, "is file decoded: " + dataDecoder.isDataDecoded());
                if(dataDecoder.isDataDecoded()){
                    if(mPreview!=null) {
                        mPreview.stop();
                        Log.d(TAG,"stopped camera preview");
                    }
                    break;
                }
            }
            border=smallBorder(matrix.border);
            if(VERBOSE){Log.d(TAG,"reduced borders (to 4/5): left:"+border[0]+"\tup:"+border[1]+"\tright:"+border[2]+"\tdown:"+border[3]);}
            matrix = null;
        }
        updateInfo("识别完成！正在写入文件...");
        byte[] out=dataDecoder.dataArray();
        String sha1=FileVerification.bytesToSHA1(out);
        Log.d(TAG, "file SHA-1 verification: " + sha1);
        bytesToFile(out,fileName);
    }
    private void checkSourceBlockStatus(ArrayDataDecoder dataDecoder){
        Log.i(TAG,"check source block status:");
        for (SourceBlockDecoder sourceBlockDecoder : dataDecoder.sourceBlockIterable()) {
            Log.i(TAG, "source block number:" + sourceBlockDecoder.sourceBlockNumber() + "\tstate:" + sourceBlockDecoder.latestState());
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
