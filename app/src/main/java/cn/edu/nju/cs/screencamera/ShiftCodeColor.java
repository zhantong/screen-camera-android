package cn.edu.nju.cs.screencamera;

import java.util.BitSet;
import java.util.Map;

/**
 * Created by zhantong on 2016/12/2.
 */

public class ShiftCodeColor extends ShiftCode {
    int[] thresholds;
    public ShiftCodeColor(MediateBarcode mediateBarcode, Map<DecodeHintType, ?> hints) {
        super(mediateBarcode, hints);
    }
    public int getTransmitFileLengthInBytes() throws CRCCheckException{
        return getTransmitFileLengthInBytes(RawImage.CHANNLE_U);
    }
    public int getTransmitFileLengthInBytes(int channel) throws CRCCheckException{
        int[] content=mediateBarcode.getContent(mediateBarcode.districts.get(Districts.PADDING).get(District.UP),channel);
        BitSet data=new BitSet();
        for(int i=0;i<content.length;i++){
            if(content[i]>binaryThreshold){
                data.set(i);
            }
        }
        int transmitFileLengthInBytes=Utils.bitsToInt(data,32,0);
        int crc=Utils.bitsToInt(data,8,32);
        Utils.crc8Check(transmitFileLengthInBytes,crc);
        return transmitFileLengthInBytes;
    }
    public void processBorderLeft(){
        processBorderLeft(RawImage.CHANNLE_U);
    }
    public void processBorderLeft(int channel){
        int[] content=mediateBarcode.getContent(mediateBarcode.districts.get(Districts.PADDING).get(District.LEFT),channel);
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
    public void processBorderRight(){
        super.processBorderRight(RawImage.CHANNLE_U);
        int[] channels=mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN).getBlock().getChannels();
        thresholds=new int[Utils.max(channels)+1];
        for(int channel:channels){
            int[] content = mediateBarcode.getContent(mediateBarcode.districts.get(Districts.PADDING).get(District.RIGHT), channel);
            int maxWhite = 0, minWhite = 255;
            int maxBlack = 0, minBlack = 255;
            for (int i = 0; i < content.length; i += 2) {
                int currentWhite = content[i];
                int currentBlack = content[i + 1];
                if (maxWhite < currentWhite) {
                    maxWhite = currentWhite;
                }
                if (minWhite > currentWhite) {
                    minWhite = currentWhite;
                }
                if (maxBlack < currentBlack) {
                    maxBlack = currentBlack;
                }
                if (minBlack > currentBlack) {
                    minBlack = currentBlack;
                }
            }
            thresholds[channel] = (maxWhite + maxBlack - minWhite - minBlack) / 2;
        }
    }
    public int[] getClearRawContent(){
        Zone zone=mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN);
        ColorShiftBlock block=(ColorShiftBlock) zone.getBlock();
        int[][] content=mediateBarcode.getContent(zone,block.getChannels());
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
        Zone zone=mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN);
        ColorShiftBlock block=(ColorShiftBlock) zone.getBlock();
        int[][] content=mediateBarcode.getContent(zone,block.getChannels());
        int[] rawDataPrev=new int[zone.widthInBlock*zone.heightInBlock];
        int[] rawDataNext=new int[zone.widthInBlock*zone.heightInBlock];
        int step=block.getNumSamplePoints();
        int offset=0;
        int rawDataPos=0;
        for(int y=0;y<zone.heightInBlock;y++) {
            for (int x = 0; x < zone.widthInBlock; x++) {
                boolean isFormerWhite=(overlapSituation==OVERLAP_WHITE_TO_BLACK);
                int[] values=block.getMixed(isFormerWhite,thresholds,x,y,content,offset);
                offset+=step;
                rawDataPrev[rawDataPos]=values[0];
                rawDataNext[rawDataPos]=values[1];
                rawDataPos++;
            }
        }
        return new int[][]{rawDataPrev,rawDataNext};
    }
}
