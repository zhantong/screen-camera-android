package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 2016/12/2.
 */

public class ShiftCodeColorConfig extends BarcodeConfig {
    public ShiftCodeColorConfig() {
        borderLength = new DistrictConfig<>(1);
        paddingLength = new DistrictConfig<>(0);

        mainWidth = 40;
        mainHeight = 40;

        blockLengthInPixel = 10;

        borderBlock = new DistrictConfig<Block>(new BlackWhiteBlock());
        mainBlock = new DistrictConfig<Block>(new ColorShiftBlock());
    }
}
