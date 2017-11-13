package cn.edu.nju.cs.screencamera;


/**
 * Created by zhantong on 2016/11/25.
 */

public class ShiftCode extends BlackWhiteCode {

    public ShiftCode(MediateBarcode mediateBarcode) {
        super(mediateBarcode);
    }

    public int[] getClearRawContent() {
        return getClearRawContent(RawImage.CHANNLE_Y);
    }

    public int[] getClearRawContent(int channel) {
        Zone zone = mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN);
        ShiftBlock block = (ShiftBlock) zone.getBlock();
        int[] content = mediateBarcode.getContent(zone, channel);
        int[] rawData = new int[zone.widthInBlock * zone.heightInBlock];
        int step = block.getNumSamplePoints();
        int offset = 0;
        int rawDataPos = 0;
        for (int y = 0; y < zone.heightInBlock; y++) {
            for (int x = 0; x < zone.widthInBlock; x++) {
                boolean isWhite = (overlapSituation == OVERLAP_CLEAR_WHITE);
                int value = block.getClear(isWhite, x, y, content, offset);
                offset += step;
                rawData[rawDataPos] = value;
                rawDataPos++;
            }
        }
        return rawData;
    }

    public int[][] getMixedRawContent() {
        return getMixedRawContent(RawImage.CHANNLE_Y);
    }

    public int[][] getMixedRawContent(int channel) {
        Zone zone = mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN);
        ShiftBlock block = (ShiftBlock) zone.getBlock();
        int[] content = mediateBarcode.getContent(zone, channel);
        int[] rawDataPrev = new int[zone.widthInBlock * zone.heightInBlock];
        int[] rawDataNext = new int[zone.widthInBlock * zone.heightInBlock];
        int step = block.getNumSamplePoints();
        int offset = 0;
        int rawDataPos = 0;
        for (int y = 0; y < zone.heightInBlock; y++) {
            for (int x = 0; x < zone.widthInBlock; x++) {
                boolean isFormerWhite = (overlapSituation == OVERLAP_WHITE_TO_BLACK);
                int[] values = block.getMixed(isFormerWhite, expand, x, y, content, offset);
                offset += step;
                rawDataPrev[rawDataPos] = values[0];
                rawDataNext[rawDataPos] = values[1];
                rawDataPos++;
            }
        }
        return new int[][]{rawDataPrev, rawDataNext};
    }
}
