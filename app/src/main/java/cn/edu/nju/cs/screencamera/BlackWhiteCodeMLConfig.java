package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 2017/5/11.
 */

public class BlackWhiteCodeMLConfig extends BarcodeConfig{
    public BlackWhiteCodeMLConfig(){
        borderLength = new DistrictConfig<>(1);
        paddingLength = new DistrictConfig<>(2,0,2,0);
        metaLength=new DistrictConfig<>(0);

        mainWidth = 100;
        mainHeight = 100;

        blockLengthInPixel = 20;

        borderBlock = new DistrictConfig<Block>(new BlackWhiteBlock());
        mainBlock = new DistrictConfig<Block>(new BlackWhiteBlock());
    }
}
