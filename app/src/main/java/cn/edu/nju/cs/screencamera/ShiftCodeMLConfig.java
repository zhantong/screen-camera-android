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

        borderBlock = new DistrictConfig<Block>(new BlackWhiteBlock());
        mainBlock = new DistrictConfig<Block>(new ShiftBlock());

        hints.put(ShiftCodeML.KEY_SIZE_RS_ERROR_CORRECTION,12);
        hints.put(ShiftCodeML.KEY_LEVEL_RS_ERROR_CORRECTION,0.1);
        hints.put(ShiftCodeML.KEY_NUMBER_RAPTORQ_SOURCE_BLOCKS,1);
        hints.put(ShiftCodeML.KEY_NUMBER_RANDOM_BARCODES,100);
    }
}
