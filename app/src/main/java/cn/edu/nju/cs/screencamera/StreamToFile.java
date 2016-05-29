package cn.edu.nju.cs.screencamera;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.SymbolType;
import net.fec.openrq.decoder.SourceBlockDecoder;
import net.fec.openrq.parameters.FECParameters;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhantong on 16/2/29.
 */
public class StreamToFile extends MediaToFile {
    private static final String TAG = "StreamToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log
    private static final long queueWaitSeconds=4;
    private static BarcodeFormat barcodeFormat;
    private HandlerThread handlerThread;
    private Handler processHandler;
    public StreamToFile(Handler handler,BarcodeFormat format,String truthFilePath) {
        super(handler);
        barcodeFormat=format;
        if(!truthFilePath.equals("")) {
            setDebug(MatrixFactory.createMatrix(format), truthFilePath);
        }

        handlerThread=new ProcessFrame("process");
        handlerThread.start();
        processHandler=new Handler(handlerThread.getLooper(), (Handler.Callback) handlerThread);
        processHandler.sendMessage(processHandler.obtainMessage(1,format));
    }
    public int getImgColorType(){
        return -1;
    }
    public void notFound(int fileByteNum){}
    public void crcCheckFailed(){}
    public void beforeDataDecoded(){}
    protected void streamToFile(LinkedBlockingQueue<byte[]> imgs,int frameWidth,int frameHeight,String fileName) {
        final int NUMBER_OF_SOURCE_BLOCKS=1;
        long processStartTime=System.currentTimeMillis();
        long processEndTime=0;
        ArrayDataDecoder dataDecoder=null;
        SourceBlockDecoder sourceBlock=null;
        int fileByteNum=-1;
        int count = -1;
        int lastSuccessIndex = 0;
        int frameAmount = 0;
        byte[] img = {};
        Matrix matrix;
        int[] border=null;
        int index = 0;
        int imgColorType=getImgColorType();
        long startTime;
        long endTime;
        boolean frameDecodeSuccess;
        while (true) {
            count++;
            updateInfo("正在识别...");
            try {
                if(VERBOSE){Log.d(TAG,"is queue empty:"+imgs.isEmpty());}
                img = imgs.poll(queueWaitSeconds, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Log.d(TAG, e.getMessage());
            }
            if(img==null){
                updateInfo("识别失败！");
                break;
            }
            frameDecodeSuccess=false;
            updateDebug(index, lastSuccessIndex, frameAmount, count);
            startTime= System.currentTimeMillis();
            Log.i(TAG, "current frame:" + count);
            try {
                matrix=MatrixFactory.createMatrix(barcodeFormat,img,imgColorType, frameWidth, frameHeight,border);
                matrix.perspectiveTransform();
            } catch (NotFoundException e) {
                Log.d(TAG, e.getMessage());
                notFound(fileByteNum);
                border=null;
                continue;
            }
            if(fileByteNum==-1){
                try {
                    fileByteNum = getFileByteNum(matrix);
                }catch (CRCCheckException e){
                    Log.d(TAG, "head CRC check failed");
                    crcCheckFailed();
                    continue;
                }
                if(fileByteNum==0){
                    Log.d(TAG,"wrong file byte number");
                    fileByteNum=-1;
                    continue;
                }
                Log.i(TAG,"file is "+fileByteNum+" bytes");
                int length=matrix.realContentByteLength();
                FECParameters parameters = FECParameters.newParameters(fileByteNum, length, NUMBER_OF_SOURCE_BLOCKS);
                processHandler.sendMessage(processHandler.obtainMessage(2,parameters));
            }
            byte[] current;
            matrix.sampleContent();
            processHandler.sendMessage(processHandler.obtainMessage(3,matrix.getRaw()));
            border=null;
            /*
            for(int i=1;i<3;i++) {
                if(i==2){
                    if(!matrix.isMixed){
                        break;
                    }
                    matrix.reverse=true;
                }
                Log.i(TAG,"try reverse "+matrix.reverse+" :");
                if(fileByteNum==-1){
                    try {
                        fileByteNum = getFileByteNum(matrix);
                    }catch (CRCCheckException e){
                        Log.d(TAG, "head CRC check failed");
                        crcCheckFailed();
                        continue;
                    }
                    if(fileByteNum==0){
                        Log.d(TAG,"wrong file byte number");
                        fileByteNum=-1;
                        continue;
                    }
                    Log.i(TAG,"file is "+fileByteNum+" bytes");
                    int length=matrix.realContentByteLength();
                    FECParameters parameters = FECParameters.newParameters(fileByteNum, length, NUMBER_OF_SOURCE_BLOCKS);
                    processHandler.sendMessage(processHandler.obtainMessage(2,parameters));

                    Log.d(TAG, "RaptorQ parameters:" + parameters.toString());
                    dataDecoder = OpenRQ.newDecoder(parameters, 0);
                    if(sourceBlock==null) {
                        sourceBlock = dataDecoder.sourceBlock(NUMBER_OF_SOURCE_BLOCKS - 1);
                    }

                }
                byte[] current;
                matrix.sampleContent();
                processHandler.sendMessage(processHandler.obtainMessage(3,matrix.getRaw()));

                try {
                    current = getContent(matrix);
                }catch (ReedSolomonException e){
                    Log.d(TAG, "content error correction failed");
                    continue;
                }
                EncodingPacket encodingPacket = dataDecoder.parsePacket(current, true).value();
                Log.i(TAG, "got 1 encoding packet: encoding symbol ID:" + encodingPacket.encodingSymbolID() + "\t" + encodingPacket.symbolType());
                if(isLastEncodingPacket(sourceBlock,encodingPacket)){
                    processEndTime=System.currentTimeMillis();
                    beforeDataDecoded();
                    imgs.clear();
                }
                if(VERBOSE){Log.d(TAG,sourceBlock.toString());}
                dataDecoder.sourceBlock(encodingPacket.sourceBlockNumber()).putEncodingPacket(encodingPacket);
                frameDecodeSuccess=true;

            }
            */
            /*
            if(frameDecodeSuccess){
                if(dataDecoder.isDataDecoded()){
                    break;
                }
                border=smallBorder(matrix.border);
                if(VERBOSE){Log.d(TAG, "reduced borders (to 4/5): left:" + border[0] + "\tup:" + border[1] + "\tright:" + border[2] + "\tdown:" + border[3]);}
            }
            matrix = null;
            endTime= System.currentTimeMillis();
            Log.d(TAG,"process frame "+count+" takes "+(endTime-startTime)+"ms");
            */
        }
        if(dataDecoder!=null&&dataDecoder.isDataDecoded()) {
            updateInfo("识别完成！正在写入文件...");
            writeRaptorQDataFile(dataDecoder,fileName);
        }
        bitErrorCount.logAverageBitError();
        Log.d(TAG,"totally takes "+(processEndTime-processStartTime)+"ms");
    }
    private boolean isLastEncodingPacket(SourceBlockDecoder sourceBlock,EncodingPacket encodingPacket){
        return (sourceBlock.missingSourceSymbols().size()-sourceBlock.availableRepairSymbols().size()==1)
                &&((encodingPacket.symbolType()== SymbolType.SOURCE&&!sourceBlock.containsSourceSymbol(encodingPacket.encodingSymbolID()))
                ||(encodingPacket.symbolType()== SymbolType.REPAIR&&!sourceBlock.containsRepairSymbol(encodingPacket.encodingSymbolID())));
    }
    private void writeRaptorQDataFile(ArrayDataDecoder decoder,String fileName){
        byte[] out = decoder.dataArray();
        String sha1 = FileVerification.bytesToSHA1(out);
        Log.d(TAG, "file SHA-1 verification: " + sha1);
        bytesToFile(out, fileName);
    }
}