package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 2016/12/2.
 */

public class ShiftCodeColorConfig extends BarcodeConfig {
    public ShiftCodeColorConfig() {
        borderLength = new DistrictConfig<>(1);
        paddingLength = new DistrictConfig<>(1);

        mainWidth = 40;
        mainHeight = 40;

        blockLengthInPixel = 10;

        borderBlock = new DistrictConfig<Block>(new BlackWhiteBlock());
        mainBlock = new DistrictConfig<Block>(new ColorShiftBlock(new int[]{RawImage.CHANNLE_U, RawImage.CHANNLE_V}));

        hints.put(ShiftCodeColor.KEY_SIZE_RS_ERROR_CORRECTION, 12);
        hints.put(ShiftCodeColor.KEY_LEVEL_RS_ERROR_CORRECTION, 0.1);
        hints.put(ShiftCodeColor.KEY_NUMBER_RAPTORQ_SOURCE_BLOCKS, 1);
    }
}
