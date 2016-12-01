package cn.edu.nju.cs.screencamera;

import android.util.SparseIntArray;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * Created by zhantong on 2016/11/28.
 */

public class ShiftCodeML extends ShiftCode{
    private static int numRandomBarcode=40;
    private boolean isRandom=false;
    public ShiftCodeML(MediateBarcode mediateBarcode, Map<DecodeHintType, ?> hints) {
        super(mediateBarcode, hints);
        if(mediateBarcode.rawImage==null){
            return;
        }
        processBorderDown();
    }
    public boolean getIsRandom(){
        return isRandom;
    }
    private void processBorderDown(){
        int[] content=mediateBarcode.getContent(mediateBarcode.districts.get(Districts.BORDER).get(District.DOWN));
        if(content[0]>binaryThreshold){
            isRandom=true;
        }
    }
    public SparseIntArray[] getVaryBar(){
        Zone leftPadding=mediateBarcode.districts.get(Districts.PADDING).get(District.LEFT);
        Zone rightPadding=mediateBarcode.districts.get(Districts.PADDING).get(District.RIGHT);
        SparseIntArray varyOne=new SparseIntArray();
        leftPadding.scanColumn(0,varyOne,mediateBarcode.transform,mediateBarcode.rawImage);
        rightPadding.scanColumn(0,varyOne,mediateBarcode.transform,mediateBarcode.rawImage);

        SparseIntArray varyTwo=new SparseIntArray();
        leftPadding.scanColumn(1,varyTwo,mediateBarcode.transform,mediateBarcode.rawImage);
        rightPadding.scanColumn(1,varyTwo,mediateBarcode.transform,mediateBarcode.rawImage);
        return new SparseIntArray[]{varyOne,varyTwo};
    }
    public JsonObject getVaryBarToJson(){
        Gson gson=new Gson();
        JsonObject root=new JsonObject();
        SparseIntArray[] varyBars=getVaryBar();
        root.add("vary bar",gson.toJsonTree(varyBars));
        return root;
    }
    public static List<int[]> randomBarcodeValue(ShiftCodeMLConfig config){
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
}
