package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 2017/5/24.
 */

public class BlackWhiteCodeConfig extends BarcodeConfig{
    public BlackWhiteCodeConfig(){
        borderLength = new DistrictConfig<>(1);
        paddingLength = new DistrictConfig<>(2,0,2,0);
        metaLength=new DistrictConfig<>(0);

        mainWidth = 120;
        mainHeight = 120;

        blockLengthInPixel = 20;

        borderBlock = new DistrictConfig<Block>(new BlackWhiteBlock());
        mainBlock = new DistrictConfig<Block>(new BlackWhiteBlock());
    }
}
