package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 2016/11/24.
 */

public class ShiftCodeConfig extends BarcodeConfig {
    public ShiftCodeConfig() {
        borderLength = new DistrictConfig<>(1);
        paddingLength = new DistrictConfig<>(0);

        mainWidth = 40;
        mainHeight = 40;

        blockLengthInPixel = 10;

        borderBlock = new DistrictConfig<Block>(new BlackWhiteBlock());
        mainBlock = new DistrictConfig<Block>(new ShiftBlock());

        hints.put(BlackWhiteCode.KEY_SIZE_RS_ERROR_CORRECTION, 12);
        hints.put(BlackWhiteCode.KEY_LEVEL_RS_ERROR_CORRECTION, 0.1);
        hints.put(BlackWhiteCode.KEY_NUMBER_RAPTORQ_SOURCE_BLOCKS, 1);
    }
}
