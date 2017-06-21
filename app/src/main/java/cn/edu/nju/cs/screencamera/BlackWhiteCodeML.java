package cn.edu.nju.cs.screencamera;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * Created by zhantong on 2017/5/11.
 */

public class BlackWhiteCodeML extends BlackWhiteCodeWithBar{
    public static final String KEY_NUMBER_RANDOM_BARCODES="NUMBER_RANDOM_BARCODES";
    private boolean isRandom=false;
    public BlackWhiteCodeML(MediateBarcode mediateBarcode) {
        super(mediateBarcode);
        if(mediateBarcode.rawImage==null){
            return;
        }
        processBorderDown();
    }
    static List<int[]> randomBarcodeValue(BarcodeConfig config,int numRandomBarcode){
        int bitsPerUnit=config.mainBlock.get(District.MAIN).getBitsPerUnit();
        int bitSetLength=config.mainWidth*config.mainHeight*bitsPerUnit;
        List<BitSet> randomBitSetList=Utils.randomBitSetList(bitSetLength,numRandomBarcode,0);
        List<int[]> randomIntArrayList=new ArrayList<>(randomBitSetList.size());
        for(BitSet randomBitSet:randomBitSetList){
            int[] randomIntArray=new int[bitSetLength/bitsPerUnit];
            for(int i=0,intArrayPos=0;i<bitSetLength;i+=bitsPerUnit,intArrayPos++){
                randomIntArray[intArrayPos]=Utils.bitsToInt(randomBitSet,bitsPerUnit,i);
            }
            randomIntArrayList.add(randomIntArray);
        }
        return randomIntArrayList;
    }
    private void processBorderDown(){
        processBorderDown(RawImage.CHANNLE_Y);
    }
    private void processBorderDown(int channel){
        int[] content=mediateBarcode.getContent(mediateBarcode.districts.get(Districts.BORDER).get(District.DOWN),channel);
        if(content[2]>binaryThreshold){
            isRandom=true;
        }
    }
    public boolean getIsRandom(){
        return isRandom;
    }
    public int getTransmitFileLengthInBytes(int channel) throws CRCCheckException{
        if(isRandom) {
            int[] content=mediateBarcode.getContent(mediateBarcode.districts.get(Districts.BORDER).get(District.UP),channel);
            BitSet data=new BitSet();
            for(int i=0;i<content.length;i++){
                if(content[i]>binaryThreshold){
                    data.set(i);
                }
            }
            int transmitFileLengthInBytes = Utils.bitsToInt(Utils.reverse(data, 32), 32, 0);
            return Utils.grayCodeToInt(transmitFileLengthInBytes);
        }
        return super.getTransmitFileLengthInBytes(channel);
    }

    @Override
    JsonObject toJson() {
        JsonObject root= super.toJson();
        root.addProperty("isRandom",isRandom);
        return root;
    }
}
