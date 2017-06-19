package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 2017/6/4.
 */

public class RDCodeMLConfig extends BarcodeConfig {
    int regionWidth;
    int regionHeight;
    int numRegionHorizon;
    int numRegionVertical;

    public RDCodeMLConfig(){
        borderLength = new DistrictConfig<>(1);
        paddingLength = new DistrictConfig<>(2,0,2,0);
        metaLength=new DistrictConfig<>(0);

        regionWidth=12;
        regionHeight=12;
        numRegionHorizon=3;
        numRegionVertical=3;

        mainWidth = numRegionHorizon*regionWidth;
        mainHeight = numRegionVertical*regionHeight;

        borderBlock = new DistrictConfig<Block>(new BlackWhiteBlock());
        mainBlock = new DistrictConfig<Block>(new ColorBlock(2));
    }
}
