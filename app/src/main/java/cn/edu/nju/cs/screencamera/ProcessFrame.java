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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.edu.nju.cs.screencamera.ReedSolomon.GenericGF;
import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonDecoder;
import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

/**
 * Created by zhantong on 16/5/29.
 */
public class ProcessFrame extends HandlerThread implements Handler.Callback {
    private static final String TAG="ProcessFrame";
    private static final boolean ENABLE_INTER_FRAME_ERROR_CORRECTION=false;
    List<RawContent> list;
    Matrix matrix;
    ArrayDataDecoder dataDecoder;
    SourceBlockDecoder sourceBlock;
    ReedSolomonDecoder decoder = new ReedSolomonDecoder(GenericGF.AZTEC_DATA_10);
    String fileName;
    FrameCallback mFrameCallback;
    Map<Integer,BitSet> map;
    Map<Integer,BitSet> temp;
    int numSymbols;
    FileToBitSet truthBitSet;

    public ProcessFrame(String name){
        super(name);
        list=new ArrayList<>();
        map=new HashMap<>();
        temp=new HashMap<>();
    }



    public interface FrameCallback{
        void onLastPacket();
    }
    public void setCallback(FrameCallback callback){
        mFrameCallback=callback;
    }
    public void put(RawContent content,boolean doRepair){
        EncodingPacket encodingPacket;
        boolean reverse=false;
        for(int j=1;j<3;j++){
            if(j==2){
                if(content.isMixed){
                    reverse=true;
                    if(!content.alreayPut) {
                        content.alreayPut=true;
                        list.add(content);
                        Log.d(TAG,"put into list:"+content.esi1 + " " + content.isEsi1Done + "\t" + content.esi2 + " " + content.isEsi2Done);
                    }
                }else{
                    break;
                }
            }
            if(!reverse){
                if(content.isEsi1Done){
                    continue;
                }
            }else{
                if(content.isEsi2Done){
                    continue;
                }
            }
            try {
                BitSet bitSetContent=content.getRawContent(reverse);
                if(!reverse){
                    if(content.esi1==-1) {
                        content.esi1 = extractEncodingSymbolID(getFecPayloadID(bitSetContent));
                    }
                }else{
                    if(content.esi2==-1) {
                        content.esi2 = extractEncodingSymbolID(getFecPayloadID(bitSetContent));
                        if (content.esi1 > numSymbols && content.esi2 < numSymbols) {
                            content.esi1 = content.esi2 - 1;
                        } else if (content.esi2 > numSymbols && content.esi1 < numSymbols) {
                            content.esi2 = content.esi1 + 1;
                        }
                    }
                }
                if(content.isMixed){
                    Log.d(TAG,"current mixed frame contains: esi1:"+content.esi1+" esi2:"+content.esi2);
                }else{
                    Log.d(TAG,"current clear frame contains: esi:"+content.esi1);
                }

                int[] conIn10=getRawContent(bitSetContent);
                if(truthBitSet!=null){
                    if(!reverse){
                        checkBitSet(bitSetContent,content.esi1);
                    } else{
                        checkBitSet(bitSetContent,content.esi2);
                    }
                }
                decoder.decode(conIn10,matrix.ecNum);
                int realByteNum=matrix.RSContentByteLength();
                byte[] raw=new byte[realByteNum];
                for(int i=0;i<raw.length*8;i++){
                    if((conIn10[i/matrix.ecLength]&(1<<(i%matrix.ecLength)))>0){
                        raw[i/8]|=1<<(i%8);
                    }
                }
                encodingPacket = dataDecoder.parsePacket(raw, true).value();
                Log.i(TAG, "got 1 encoding packet: encoding symbol ID:" + encodingPacket.encodingSymbolID() + "\t" + encodingPacket.symbolType());
                if(isLastEncodingPacket(sourceBlock,encodingPacket)){
                    Log.d(TAG,"the last esi is "+encodingPacket.encodingSymbolID());
                    if(mFrameCallback!=null) {
                        mFrameCallback.onLastPacket();
                    }
                }
                dataDecoder.sourceBlock(encodingPacket.sourceBlockNumber()).putEncodingPacket(encodingPacket);
                if(dataDecoder.isDataDecoded()){
                    writeRaptorQDataFile(dataDecoder,fileName);
                    break;
                }
                if(!doRepair) {
                    if (!reverse) {
                        content.isEsi1Done = true;
                    } else {
                        content.isEsi2Done = true;
                    }
                }
                BitSet bitSet=toBitSet(conIn10,matrix.ecLength);
                int esi=extractEncodingSymbolID(getFecPayloadID(bitSet));
                if(!reverse){
                    content.esi1=esi;
                    content.esi2=esi+1;
                }else{
                    content.esi1=esi-1;
                    content.esi2=esi;
                }
                if(!map.containsKey(esi)){
                    if(!doRepair) {
                        temp.put(esi, bitSet);
                    }else{
                        map.put(esi, bitSet);
                    }
                }
                if(doRepair) {
                    test();
                }
                /*
                BitSet clone = (BitSet) bitSet.clone();
                clone.xor(bitSetContent);
                int bitError=clone.cardinality();
                System.out.println("bit error:"+bitError);
                */

            }catch (ReedSolomonException e){
                Log.d(TAG,"RS decode failed");
            }

        }
    }
    private void checkBitSet(BitSet raw,int esi){
        BitSet truth=truthBitSet.getPacket(esi);
        if(truth==null){
            Log.d(TAG,"esi "+esi+" don't exist");
            return;
        }
        BitSet clone = (BitSet) raw.clone();
        clone.xor(truth);
        int bitError=clone.cardinality();
        Log.d(TAG, "esi " + esi + " has " + bitError + " bit errors");
    }
    private void test(){
        Log.d(TAG,"start repair");
        Log.d(TAG,"the list:");
        for(RawContent con:list){
            Log.d(TAG,con.esi1 + " " + con.isEsi1Done + "\t" + con.esi2 + " " + con.isEsi2Done);
        }
        for(Map.Entry<Integer, BitSet> entry : map.entrySet()){
            int esi=entry.getKey();
            BitSet bitSet=entry.getValue();
            for (Iterator<RawContent> it=list.iterator();it.hasNext();) {
                RawContent content=it.next();
                if(map.containsKey(content.esi1)&&map.containsKey(content.esi2)){
                    it.remove();
                    continue;
                }
                if ((esi==content.esi1||esi==content.esi2)&&!content.isEsi1Done&&!content.isEsi2Done) {
                    Log.d(TAG,"esi "+esi+" processing "+content.esi1 + " " + content.isEsi1Done + "\t" + content.esi2 + " " + content.isEsi2Done);
                    BitSet clearTag = content.clearTag;
                    for (int i = clearTag.nextSetBit(0); i >= 0; i = clearTag.nextSetBit(i + 1)) {
                        content.clear.set(i, bitSet.get(i));
                    }
                    put(content,false);
                }
            }
        }
        map.putAll(temp);
        Log.d(TAG,"stop repair");
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
    private static BitSet toBitSet(int data[],int bitNum){
        int index=0;
        BitSet bitSet=new BitSet();
        for(int current:data){
            for(int i=0;i<bitNum;i++){
                if((current&(1<<i))>0){
                    bitSet.set(index);
                }
                index++;
            }
        }
        return bitSet;
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
            numSymbols=(int)(sourceBlock.numberOfSourceSymbols()*1.5);
            break;
        case 3:
            RawContent content = (RawContent) msg.obj;
            put(content,ENABLE_INTER_FRAME_ERROR_CORRECTION);
            break;
        case 4:
            fileName = (String) msg.obj;
            break;
        case 5:
            String truthFilePath=(String)msg.obj;
            truthBitSet=new FileToBitSet(matrix,truthFilePath);
            break;
    }
    return true;
}

    public int getFecPayloadID(BitSet bitSet){
        int value=0;
        for (int i = bitSet.nextSetBit(0); i <32; i = bitSet.nextSetBit(i + 1)) {
            value|=(1<<(i%8))<<(3-i/8)*8;
        }
        return value;
    }
    public int extractSourceBlockNumber(int fecPayloadID){
        return fecPayloadID>>24;
    }
    public int extractEncodingSymbolID(int fecPayloadID){
        return fecPayloadID&0x0FFF;
    }
}
