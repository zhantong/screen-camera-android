package cn.edu.nju.cs.screencamera;

import android.util.SparseIntArray;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * Created by zhantong on 2017/6/4.
 */

public class RDCodeML {
    public static final int OVERLAP_CLEAR_WHITE=0;
    public static final int OVERLAP_CLEAR_BLACK=1;
    public static final int OVERLAP_BLACK_TO_WHITE=2;
    public static final int OVERLAP_WHITE_TO_BLACK=3;

    public static int numRandomBarcode=100;
    private boolean isRandom=false;
    MediateBarcode mediateBarcode;
    int refWhite;
    int refBlack;
    int threshold;
    int binaryThreshold;
    int overlapSituation;
    Map<DecodeHintType,?> hints;
    public RDCodeML(MediateBarcode mediateBarcode, Map<DecodeHintType, ?> hints) {
        this.mediateBarcode=mediateBarcode;
        this.hints=hints;
        if(mediateBarcode.rawImage==null){
            return;
        }
        processBorderRight();
        processBorderLeft();
        System.out.println("refWhite: "+refWhite+" refBlack: "+refBlack+" threshold: "+threshold);
        System.out.println("overlap: "+overlapSituation);
        processBorderDown();
    }
    public static List<int[]> randomBarcodeValue(RDCodeMLConfig config){
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
    public void processBorderRight(){
        processBorderRight(RawImage.CHANNLE_Y);
    }
    public void processBorderRight(int channel){
        int[] content=mediateBarcode.getContent(mediateBarcode.districts.get(Districts.BORDER).get(District.RIGHT),channel);
        int sumWhite=0,sumBlack=0;
        int maxWhite=0,minWhite=255;
        int maxBlack=0,minBlack=255;
        for(int i=0;i<content.length;i+=2){
            int currentWhite=content[i];
            int currentBlack=content[i+1];
            sumWhite+=currentWhite;
            sumBlack+=currentBlack;
            if(maxWhite<currentWhite){
                maxWhite=currentWhite;
            }
            if(minWhite>currentWhite){
                minWhite=currentWhite;
            }
            if(maxBlack<currentBlack){
                maxBlack=currentBlack;
            }
            if(minBlack>currentBlack){
                minBlack=currentBlack;
            }
        }
        int numHalfPoints=content.length/2;
        refWhite=sumWhite/numHalfPoints;
        refBlack=sumBlack/numHalfPoints;
        binaryThreshold=(refWhite+refBlack)/2;
        threshold=(maxWhite+maxBlack-minWhite-minBlack)/2;
    }
    public void processBorderLeft(){
        processBorderLeft(RawImage.CHANNLE_Y);
    }
    public void processBorderLeft(int channel){
        int[] content=mediateBarcode.getContent(mediateBarcode.districts.get(Districts.BORDER).get(District.LEFT),channel);
        int mixIndicatorUp=content[0];
        int mixIndicatorDown=content[content.length-1];
        int refBlackExpand=refBlack+threshold;
        int refWhiteExpand=refWhite-threshold;
        if((mixIndicatorUp<=refBlackExpand)&&(mixIndicatorDown<=refBlackExpand)){
            overlapSituation=OVERLAP_CLEAR_BLACK;
        }else if((mixIndicatorUp>=refWhiteExpand)&&(mixIndicatorDown>=refWhiteExpand)){
            overlapSituation=OVERLAP_CLEAR_WHITE;
        }else if(mixIndicatorUp>mixIndicatorDown){
            overlapSituation=OVERLAP_WHITE_TO_BLACK;
        }else{
            overlapSituation=OVERLAP_BLACK_TO_WHITE;
        }
        System.out.println("mix indicator up: "+mixIndicatorUp+" mix indicator down: "+mixIndicatorDown+" refWhiteEx: "+refWhiteExpand+" refBlackEx: "+refBlackExpand);
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
    public SparseIntArray[] getVaryBar(){
        return getVaryBar(RawImage.CHANNLE_Y);
    }
    public SparseIntArray[] getVaryBar(int channel){
        Zone leftPadding=mediateBarcode.districts.get(Districts.PADDING).get(District.LEFT);
        Zone rightPadding=mediateBarcode.districts.get(Districts.PADDING).get(District.RIGHT);
        SparseIntArray varyOne=new SparseIntArray();
        leftPadding.scanColumn(0,varyOne,mediateBarcode.transform,mediateBarcode.rawImage,channel);
        rightPadding.scanColumn(0,varyOne,mediateBarcode.transform,mediateBarcode.rawImage,channel);

        SparseIntArray varyTwo=new SparseIntArray();
        leftPadding.scanColumn(1,varyTwo,mediateBarcode.transform,mediateBarcode.rawImage,channel);
        rightPadding.scanColumn(1,varyTwo,mediateBarcode.transform,mediateBarcode.rawImage,channel);
        return new SparseIntArray[]{varyOne,varyTwo};
    }
    public JsonObject getVaryBarToJson(){
        Gson gson=new Gson();
        JsonObject root=new JsonObject();
        SparseIntArray[] varyBars=getVaryBar();
        root.add("vary bar",gson.toJsonTree(varyBars));
        return root;
    }
    public int getOverlapSituation(){
        return overlapSituation;
    }
    public boolean getIsRandom(){
        return isRandom;
    }
    public int getTransmitFileLengthInBytes() throws CRCCheckException{
        return getTransmitFileLengthInBytes(RawImage.CHANNLE_Y);
    }
    public int getTransmitFileLengthInBytes(int channel) throws CRCCheckException{
        int[] content=mediateBarcode.getContent(mediateBarcode.districts.get(Districts.BORDER).get(District.UP),channel);
        System.out.println(mediateBarcode.districts.get(Districts.BORDER).get(District.UP).toJson());
        System.out.println(Arrays.toString(content));
        BitSet data=new BitSet();
        for(int i=0;i<content.length;i++){
            if(content[i]>binaryThreshold){
                data.set(i);
            }
        }
        if(isRandom) {
            int transmitFileLengthInBytes = Utils.bitsToInt(Utils.reverse(data, 32), 32, 0);
            return Utils.grayCodeToInt(transmitFileLengthInBytes);
        }
        int transmitFileLengthInBytes=Utils.bitsToInt(data,32,0);
        int crc=Utils.bitsToInt(data,8,32);
        Utils.crc8Check(transmitFileLengthInBytes,crc);
        return transmitFileLengthInBytes;
    }
}
