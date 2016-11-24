package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 2016/11/24.
 */

public class ShiftCodeConfig extends BarcodeConfig{
    public ShiftCodeConfig() {
        marginLength = new DistrictConfig<>(2);
        borderLength = new DistrictConfig<>(1);
        paddingLength = new DistrictConfig<>(0);

        mainWidth = 40;
        mainHeight = 40;

        blockLengthInPixel = 10;

        marginBlock = new DistrictConfig<Block>(new BlackWhiteBlock());
        borderBlock = new DistrictConfig<Block>(new BlackWhiteBlock());
        mainBlock = new DistrictConfig<Block>(new ShiftBlock());
    }
}
