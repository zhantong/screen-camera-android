package cn.edu.nju.cs.screencamera;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.parameters.FECParameters;

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
    ReedSolomonDecoder decoder = new ReedSolomonDecoder(GenericGF.AZTEC_DATA_10);
    public ProcessFrame(String name){
        super(name);
        list=new ArrayList<>();
    }
    public void put(RawContent content){
        byte[] raw;
        EncodingPacket encodingPacket;
        if(!content.isMixed){
            try {
                raw = getContent(content.getRawContent(false));
                encodingPacket = dataDecoder.parsePacket(raw, true).value();
                Log.i(TAG, "got 1 encoding packet: encoding symbol ID:" + encodingPacket.encodingSymbolID() + "\t" + encodingPacket.symbolType());
                dataDecoder.sourceBlock(encodingPacket.sourceBlockNumber()).putEncodingPacket(encodingPacket);
            }catch (ReedSolomonException e){
                Log.d(TAG,"RS decode failed");
            }
        }else{
            try {
                raw = getContent(content.getRawContent(false));
                encodingPacket = dataDecoder.parsePacket(raw, true).value();
                Log.i(TAG, "got 1 encoding packet: encoding symbol ID:" + encodingPacket.encodingSymbolID() + "\t" + encodingPacket.symbolType());
                dataDecoder.sourceBlock(encodingPacket.sourceBlockNumber()).putEncodingPacket(encodingPacket);
            }catch (ReedSolomonException e){
                Log.d(TAG,"RS decode failed");
            }
            try {
                raw = getContent(content.getRawContent(true));
                encodingPacket = dataDecoder.parsePacket(raw, true).value();
                Log.i(TAG, "got 1 encoding packet: encoding symbol ID:" + encodingPacket.encodingSymbolID() + "\t" + encodingPacket.symbolType());
                dataDecoder.sourceBlock(encodingPacket.sourceBlockNumber()).putEncodingPacket(encodingPacket);
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

    @Override
    public boolean handleMessage(Message msg){
        switch (msg.what){
            case 1:
                BarcodeFormat format=(BarcodeFormat)msg.obj;
                matrix=MatrixFactory.createMatrix(format);
                break;
            case 2:
                FECParameters parameters=(FECParameters)msg.obj;
                dataDecoder = OpenRQ.newDecoder(parameters, 0);
                break;
            case 3:
                RawContent content=(RawContent)msg.obj;
                put(content);
        }
        return true;
    }
}
