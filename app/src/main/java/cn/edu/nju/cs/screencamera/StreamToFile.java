package cn.edu.nju.cs.screencamera;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

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
    public StreamToFile(TextView debugView, TextView infoView, Handler handler,BarcodeFormat format,String truthFilePath) {
        super(debugView, infoView, handler);
        barcodeFormat=format;
        if(!truthFilePath.equals("")) {
            setDebug(MatrixFactory.createMatrix(format), Environment.getExternalStorageDirectory() + "/truth.txt");
        }
    }
    public int getImgColorType(){
        return -1;
    }
    public void notFound(int fileByteNum){}
    public void crcCheckFailed(){}
    public void beforeDataDecoded(){}
    protected void streamToFile(LinkedBlockingQueue<byte[]> imgs,int frameWidth,int frameHeight,String fileName) {
        ArrayDataDecoder dataDecoder=null;
        SourceBlockDecoder lastSourceBlock=null;
        int fileByteNum=-1;
        int count = -1;
        int lastSuccessIndex = 0;
        int frameAmount = 0;
        byte[] img = {};
        Matrix matrix;
        int[] border=null;
        int index = 0;
        int imgColorType=getImgColorType();
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
            updateDebug(index, lastSuccessIndex, frameAmount, count);
            try {
                matrix=MatrixFactory.createMatrix(barcodeFormat,img,imgColorType, frameWidth, frameHeight,border);
                matrix.perspectiveTransform();
            } catch (NotFoundException e) {
                Log.d(TAG, e.getMessage());
                notFound(fileByteNum);
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
                    FECParameters parameters = FECParameters.newParameters(fileByteNum, length, 1);
                    Log.d(TAG, "RaptorQ parameters:" + parameters.toString());
                    dataDecoder = OpenRQ.newDecoder(parameters, 0);
                    lastSourceBlock=dataDecoder.sourceBlock(dataDecoder.numberOfSourceBlocks()-1);
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
                if((lastSourceBlock.missingSourceSymbols().size()-lastSourceBlock.availableRepairSymbols().size()==1)&&((encodingPacket.symbolType()== SymbolType.SOURCE&&!lastSourceBlock.containsSourceSymbol(encodingPacket.encodingSymbolID()))||(encodingPacket.symbolType()== SymbolType.REPAIR&&!lastSourceBlock.containsRepairSymbol(encodingPacket.encodingSymbolID())))){
                    beforeDataDecoded();
                    imgs.clear();
                }
                Log.d(TAG,"received repair symbols:"+lastSourceBlock.availableRepairSymbols().size()+"\tmissing source symbols:"+lastSourceBlock.missingSourceSymbols().size());
                dataDecoder.sourceBlock(encodingPacket.sourceBlockNumber()).putEncodingPacket(encodingPacket);
            }
            if(fileByteNum!=-1) {
                //checkSourceBlockStatus(dataDecoder);
                Log.d(TAG, "is file decoded: " + dataDecoder.isDataDecoded());
                if(dataDecoder.isDataDecoded()){
                    break;
                }
            }
            border=smallBorder(matrix.border);
            if(VERBOSE){Log.d(TAG, "reduced borders (to 4/5): left:" + border[0] + "\tup:" + border[1] + "\tright:" + border[2] + "\tdown:" + border[3]);}
            matrix = null;
        }
        if(dataDecoder!=null&&dataDecoder.isDataDecoded()) {
            updateInfo("识别完成！正在写入文件...");
            byte[] out = dataDecoder.dataArray();
            String sha1 = FileVerification.bytesToSHA1(out);
            Log.d(TAG, "file SHA-1 verification: " + sha1);
            bytesToFile(out, fileName);
        }

    }
}