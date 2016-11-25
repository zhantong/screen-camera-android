package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 2016/11/25.
 */

public class ShiftCodeMLConfig extends BarcodeConfig {
    public ShiftCodeMLConfig(){
        borderLength = new DistrictConfig<>(1);
        paddingLength = new DistrictConfig<>(2,0,2,0);

        mainWidth = 40;
        mainHeight = 40;

        blockLengthInPixel = 10;

        borderBlock = new DistrictConfig<Block>(new BlackWhiteBlock());
        mainBlock = new DistrictConfig<Block>(new ShiftBlock());
    }
}
