package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 2017/5/15.
 */

public class ColorCodeMLConfig extends BlackWhiteCodeMLConfig {
    public ColorCodeMLConfig(){
        borderLength = new DistrictConfig<>(1);
        paddingLength = new DistrictConfig<>(2,0,2,0);
        metaLength=new DistrictConfig<>(0);

        mainWidth = 40;
        mainHeight = 40;

        blockLengthInPixel = 20;

        borderBlock = new DistrictConfig<Block>(new BlackWhiteBlock());
        mainBlock = new DistrictConfig<Block>(new ColorBlock(2));
    }
}
