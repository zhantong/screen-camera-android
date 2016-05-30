package cn.edu.nju.cs.screencamera;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.SymbolType;
import net.fec.openrq.decoder.SourceBlockDecoder;
import net.fec.openrq.parameters.FECParameters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import cn.edu.nju.cs.screencamera.ReedSolomon.GenericGF;
import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonDecoder;
import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

/**
 * Created by zhantong on 16/5/29.
 */
public class ProcessFrame extends HandlerThread implements Handler.Callback {
    private static final String TAG="ProcessFrame";
    List<RawContent> list;
    Matrix matrix;
    ArrayDataDecoder dataDecoder;
    SourceBlockDecoder sourceBlock;
    ReedSolomonDecoder decoder = new ReedSolomonDecoder(GenericGF.AZTEC_DATA_10);
    String fileName;
    FrameCallback mFrameCallback;
    public ProcessFrame(String name){
        super(name);
        list=new ArrayList<>();
    }



    public interface FrameCallback{
        void onLastPacket();
    }
    public void setCallback(FrameCallback callback){
        mFrameCallback=callback;
    }
    public void put(RawContent content){
        byte[] raw;
        EncodingPacket encodingPacket;
        boolean reverse=false;
        for(int i=1;i<3;i++){
            if(i==2){
                if(content.isMixed){
                    reverse=true;
                }else{
                    break;
                }
            }
            try {
                raw = getContent(content.getRawContent(reverse));
                encodingPacket = dataDecoder.parsePacket(raw, true).value();
                Log.i(TAG, "got 1 encoding packet: encoding symbol ID:" + encodingPacket.encodingSymbolID() + "\t" + encodingPacket.symbolType());
                if(isLastEncodingPacket(sourceBlock,encodingPacket)){
                    if(mFrameCallback!=null) {
                        mFrameCallback.onLastPacket();
                    }
                }
                dataDecoder.sourceBlock(encodingPacket.sourceBlockNumber()).putEncodingPacket(encodingPacket);
                if(dataDecoder.isDataDecoded()){
                    writeRaptorQDataFile(dataDecoder,fileName);
                    break;
                }
            }catch (ReedSolomonException e){
                Log.d(TAG,"RS decode failed");
            }
        }
    }

    public int[] getRawContent(BitSet content){
        int[] con=new int[matrix.bitsPerBlock*matrix.contentLength*matrix.contentLength/matrix.ecLength];
        for(int i=0;i<con.length*matrix.ecLength;i++){
            if(content.get(i)){
                con[i/matrix.ecLength]|=1<<(i%matrix.ecLength);
            }
        }
        return con;
    }
    public byte[] getContent(BitSet content) throws ReedSolomonException {
        int[] rawContent=getRawContent(content);
        int[] decodedContent=decode(rawContent,matrix.ecNum);
        int realByteNum=matrix.RSContentByteLength();
        byte[] res=new byte[realByteNum];
        for(int i=0;i<res.length*8;i++){
            if((decodedContent[i/matrix.ecLength]&(1<<(i%matrix.ecLength)))>0){
                res[i/8]|=1<<(i%8);
            }
        }
        return res;
    }
    public int[] decode(int[] raw,int ecNum) throws ReedSolomonException {
        decoder.decode(raw, ecNum);
        return raw;
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
    public boolean bytesToFile(byte[] bytes,String fileName){
        if(fileName.isEmpty()){
            Log.i(TAG, "file name is empty");
            return false;
        }
        File file = new File(Environment.getExternalStorageDirectory() + "/Download/" + fileName);
        OutputStream os;
        try {
            os = new FileOutputStream(file);
            os.write(bytes);
            os.close();
        } catch (FileNotFoundException e) {
            Log.i(TAG, "file path error, cannot create file:" + e.toString());
            return false;
        }catch (IOException e){
            Log.i(TAG, "IOException:" + e.toString());
            return false;
        }
        Log.i(TAG,"file created successfully: "+file.getAbsolutePath());
        return true;
    }

@Override
public boolean handleMessage(Message msg) {
    switch (msg.what) {
        case 1:
            BarcodeFormat format = (BarcodeFormat) msg.obj;
            matrix = MatrixFactory.createMatrix(format);
            break;
        case 2:
            FECParameters parameters = (FECParameters) msg.obj;
            dataDecoder = OpenRQ.newDecoder(parameters, 0);
            sourceBlock = dataDecoder.sourceBlock(dataDecoder.numberOfSourceBlocks() - 1);
            break;
        case 3:
            RawContent content = (RawContent) msg.obj;
            put(content);
            break;
        case 4:
            fileName = (String) msg.obj;
            break;
    }
    return true;
}
}
