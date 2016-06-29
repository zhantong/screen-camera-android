package cn.edu.nju.cs.screencamera;

import android.util.Log;

import net.fec.openrq.parameters.FECParameters;

import java.util.BitSet;
import java.util.List;

/**
 * Created by zhantong on 16/6/26.
 */
public class Statistics {
    private static final String TAG="Statistics";
    List<RawContent> rawContentList;
    FileToBitSet truthBitSet;
    FECParameters parameters;
    BarcodeFormat barcodeFormat;
    Matrix matrix;

    private static final int FRAME_RATE=30;

    int firstFrameIndex;
    int lastFrameIndex;
    int lastEsi;
    public void loadRawContentList(List<RawContent> rawContentList){
        this.rawContentList=rawContentList;
    }
    public void loadTruthFile(String truthFilePath, BarcodeFormat barcodeFormat){
        truthBitSet=new FileToBitSet(barcodeFormat,truthFilePath);
    }
    public void loadFECParameters(FECParameters parameters){
        this.parameters=parameters;
    }
    public void setFirstFrameIndex(int frameIndex){
        firstFrameIndex=frameIndex;
    }
    public void setLastFrameIndex(int frameIndex){
        lastFrameIndex=frameIndex;
    }
    public void setLastEsi(int esi){
        lastEsi=esi;
    }
    public void setBarcodeFormat(BarcodeFormat barcodeFormat){
        this.barcodeFormat=barcodeFormat;
        matrix=MatrixFactory.createMatrix(barcodeFormat);
    }
    public void doStat(){
        int countSuccDecode=0;
        int countBitError=0;
        recoverEsi(rawContentList);
        setFirstFrameIndex(locateFirstFrameIndex(rawContentList));
        for(RawContent rawContent:rawContentList){
            Log.d(TAG,"frame "+rawContent.frameIndex+": "+(rawContent.isMixed?"mixed":"clear")+" status: "+rawContent.isEsi1Done+" "+rawContent.isEsi2Done+" esi1: "+rawContent.esi1+" esi2: "+rawContent.esi2);
            boolean mixed=rawContent.isMixed;

            int esi1BitError=10000;
            int esi2BitError=10000;
            esi1BitError=getBitError(rawContent.getRawContent(false),rawContent.esi1);
            Log.d(TAG,"esi1: "+rawContent.esi1+" bit error: "+esi1BitError);
            if(mixed){
                esi2BitError=getBitError(rawContent.getRawContent(true),rawContent.esi2);
                Log.d(TAG,"esi2: "+rawContent.esi2+" bit error: "+esi2BitError);
            }

            if(rawContent.frameIndex>=firstFrameIndex&&rawContent.frameIndex<=lastFrameIndex) {
                if (rawContent.isEsi1Done) {
                    countSuccDecode++;
                }
                if (rawContent.isEsi2Done) {
                    countSuccDecode++;
                }
                if(!mixed){
                    countBitError+=esi1BitError;
                }else{
                    if(esi1BitError==-1||esi2BitError==-1){
                        countBitError+=(esi1BitError==-1)?esi2BitError:esi1BitError;
                    }else{
                        countBitError+=(esi1BitError<esi2BitError)?esi1BitError:esi2BitError;
                    }
                }
            }
        }
        Log.d(TAG,"first frame: "+firstFrameIndex);
        Log.d(TAG,"last frame: "+lastFrameIndex);
        Log.d(TAG,"successfully decoded count: "+countSuccDecode);
        Log.d(TAG,"total bit error: "+countBitError);

        int symbolBitsPerFrame=parameters.symbolSize()*8;
        int bitsPerFrame=matrix.contentLength*matrix.contentLength*matrix.bitsPerBlock;
        int bitsData=parameters.dataLengthAsInt()*8;
        int numFrames=lastFrameIndex-firstFrameIndex+1;
        double timeInSecond=numFrames/(double)FRAME_RATE;
        double throughPut=(symbolBitsPerFrame*countSuccDecode/timeInSecond)/1024;
        double bitErrorRate=countBitError/((double)bitsPerFrame*countSuccDecode);
        double goodPut=(bitsData/timeInSecond)/1024;
        int numSourceSymbols=parameters.totalSymbols();
        int numExtraSymbols=lastEsi-numSourceSymbols;
        double percentExtraSymbols=(double)numExtraSymbols/numSourceSymbols;
        Log.d(TAG,"symbolBitsPerFrame: "+symbolBitsPerFrame);
        Log.d(TAG,"bitsPerFrame: "+bitsPerFrame);
        Log.d(TAG,"bitsData: "+bitsData);
        Log.d(TAG,"numFrames: "+numFrames);
        Log.d(TAG,"timeInSecond: "+timeInSecond);
        Log.d(TAG,"throughPut: "+throughPut);
        Log.d(TAG,"bitErrorRate: "+bitErrorRate);
        Log.d(TAG,"goodPut: "+goodPut);

        Log.d(TAG,"numSourceSymbols: "+numSourceSymbols);
        Log.d(TAG,"lastEsi: "+lastEsi);
        Log.d(TAG,"numExtraSymbols: "+numExtraSymbols);
        Log.d(TAG,"percentExtraSymbols: "+percentExtraSymbols);

        Log.d(TAG,"matrix: contentLength: "+matrix.contentLength+" ecLength: "+matrix.ecLength+" ecNum: "+matrix.ecNum);
    }
    private int locateFirstFrameIndex(List<RawContent> rawContentList){
        int firstFrameIndex=-1;
        boolean flag;
        for(RawContent rawContent:rawContentList) {
            flag=false;
            if ((rawContent.esi1 == 0 && rawContent.isEsi1Done) || (rawContent.esi2 == 0 && rawContent.isEsi2Done)) {
                firstFrameIndex=rawContent.frameIndex;
                flag=true;
            }
            if(firstFrameIndex!=-1&&!flag){
                break;
            }
        }
        return firstFrameIndex;
    }
    private void recoverEsi(List<RawContent> rawContentList){
        RawContent rawContent;
        for(int index=0;index<rawContentList.size();index++){
            rawContent=rawContentList.get(index);
            if(!rawContent.isMixed){
                if(!rawContent.isEsi1Done){
                    rawContent.esi1=extractEncodingSymbolID(getFecPayloadID(rawContent.getRawContent(false)));
                }
            }else{
                if(rawContent.isEsi1Done&&!rawContent.isEsi2Done){
                    rawContent.esi2=rawContent.esi1+1;
                }else if(!rawContent.isEsi1Done&&rawContent.isEsi2Done){
                    rawContent.esi1=rawContent.esi2-1;
                }else if(!rawContent.isEsi1Done&&!rawContent.isEsi2Done){
                    int startEsi;
                    if (index == 0) {
                        startEsi=rawContentList.get(0).esi1;
                    }else {
                        startEsi = rawContentList.get(index - 1).esi1;
                    }
                    rawContent.esi1=findBestEsi(rawContent.getRawContent(false),startEsi);
                    rawContent.esi2=findBestEsi(rawContent.getRawContent(true),startEsi);
                }
            }
        }
    }
    private int findBestEsi(BitSet content,int startEsi){
        int numNext=4;
        int minBitError=Integer.MAX_VALUE;
        int esi=-1;
        for(int i=startEsi;i<startEsi+numNext;i++){
            int bitError=getBitError(content,i);
            if(bitError<minBitError){
                minBitError=bitError;
                esi=i;
            }
        }
        return esi;
    }
    private int getBitError(BitSet raw, int esi){
        BitSet truth=truthBitSet.getPacket(esi);
        if(truth==null){
            Log.d(TAG,"esi "+esi+" don't exist");
            return -1;
        }
        BitSet clone = (BitSet) raw.clone();
        clone.xor(truth);
        int bitError=clone.cardinality();
        return bitError;
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
