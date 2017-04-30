package cn.edu.nju.cs.screencamera;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;

/**
 * Created by zhantong on 2017/3/20.
 */

public class ShiftCodeColorML extends ShiftCodeML {
    int[] thresholds;
    public ShiftCodeColorML(MediateBarcode mediateBarcode, Map<DecodeHintType, ?> hints) {
        super(mediateBarcode, hints);
    }
    public void processBorderRight(){
        super.processBorderRight(RawImage.CHANNLE_Y);
        int[] channels=mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN).getBlock().getChannels();
        thresholds=new int[Utils.max(channels)+1];
        for(int channel:channels){
            int[] content = mediateBarcode.getContent(mediateBarcode.districts.get(Districts.META).get(District.RIGHT), channel);
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
