package cn.edu.nju.cs.screencamera;

import java.util.BitSet;

/**
 * Created by zhantong on 16/4/27.
 */
public class RawContent {
    public int length;
    public BitSet clear;
    public BitSet wTob;
    public BitSet bTow;
    public BitSet clearTag;
    boolean isMixed=false;
    int esi1;
    int esi2;
    boolean isEsi1Done=false;
    boolean isEsi2Done=false;
    boolean alreayPut=false;
    public RawContent(int length){
        this.length=length;
        clear=new BitSet();
        wTob=new BitSet();
        bTow=new BitSet();
        clearTag=new BitSet();
    }
    public BitSet getRawContent(boolean reversed){
        BitSet res=(BitSet)clear.clone();
        if(reversed){
            res.or(bTow);
        }
        else{
            res.or(wTob);
        }
        return res;
    }
    public BitSet getOverlapSituation(){
        BitSet res=(BitSet)wTob.clone();
        res.or(bTow);
        return res;
    }
    public void OK(){
        esi1=extractEncodingSymbolID(getFecPayloadID(getRawContent(false)));
        esi2=extractEncodingSymbolID(getFecPayloadID(getRawContent(true)));
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
