package cn.edu.nju.cs.screencamera;

import android.util.SparseIntArray;

import java.util.Map;


/**
 * Created by zhantong on 2017/5/24.
 */

public class BlackWhiteCodeWithBar extends BlackWhiteCode{


    public BlackWhiteCodeWithBar(MediateBarcode mediateBarcode, Map<DecodeHintType,?> hints){
        super(mediateBarcode,hints);
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

    public int[][] getRawContents(){
        if(overlapSituation==OVERLAP_CLEAR_WHITE||overlapSituation==OVERLAP_CLEAR_BLACK){
            return new int[][]{getClearRawContent()};
        }else{
            return getMixedRawContent();
        }
    }
    public int[] getClearRawContent(){
        return getClearRawContent(RawImage.CHANNLE_Y);
    }
    public int[] getClearRawContent(int channel){
        Zone zone=mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN);
        ShiftBlock block=(ShiftBlock) zone.getBlock();
        int[] content=mediateBarcode.getContent(zone,channel);
        int[] rawData=new int[zone.widthInBlock*zone.heightInBlock];
        int step=block.getNumSamplePoints();
        int offset=0;
        int rawDataPos=0;
        for(int y=0;y<zone.heightInBlock;y++) {
            for (int x = 0; x < zone.widthInBlock; x++) {
                boolean isWhite=(overlapSituation==OVERLAP_CLEAR_WHITE);
                int value=block.getClear(isWhite,x,y,content,offset);
                offset+=step;
                rawData[rawDataPos]=value;
                rawDataPos++;
            }
        }
        return rawData;
    }
    public int[][] getMixedRawContent(){
        return getMixedRawContent(RawImage.CHANNLE_Y);
    }
    public int[][] getMixedRawContent(int channel){
        Zone zone=mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN);
        BlackWhiteBlock block=(BlackWhiteBlock) zone.getBlock();
        int[] content=mediateBarcode.getContent(zone,channel);
        int[] realSamplePoints=mediateBarcode.getRealSamplePoints(zone);
        SparseIntArray[] varyBars=getVaryBar();
        int[] rawDataPrev=new int[zone.widthInBlock*zone.heightInBlock];
        int[] rawDataNext=new int[zone.widthInBlock*zone.heightInBlock];
        int step=block.getNumSamplePoints();
        int offset=0;
        int offsetCoord=0;
        int rawDataPos=0;
        for(int y=0;y<zone.heightInBlock;y++) {
            for (int x = 0; x < zone.widthInBlock; x++) {
                //boolean isFormerWhite=(overlapSituation==OVERLAP_WHITE_TO_BLACK);
                int[] values=block.getMixed(content[offset],refBlack,refWhite,varyBars[0].get(realSamplePoints[offsetCoord+1]),varyBars[1].get(realSamplePoints[offsetCoord+1]));
                offset+=step;
                offsetCoord+=2;
                rawDataPrev[rawDataPos]=values[0];
                rawDataNext[rawDataPos]=values[1];
                rawDataPos++;
            }
        }
        return new int[][]{rawDataPrev,rawDataNext};
    }
}
