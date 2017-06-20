package cn.edu.nju.cs.screencamera;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhantong on 2016/11/24.
 */

public class BarcodeConfig {
    public DistrictConfig<Integer> borderLength=new DistrictConfig<>(1);
    //public DistrictConfig<Integer> borderLength=new DistrictConfig<>(1,1,1,1);

    public DistrictConfig<Integer> paddingLength=new DistrictConfig<>(1);
    //public DistrictConfig<Integer> paddingLength=new DistrictConfig<>(1,1,1,1);

    public DistrictConfig<Integer> metaLength=new DistrictConfig<>(1);
    //public DistrictConfig<Integer> metaLength=new DistrictConfig<>(1,1,1,1);

    public int mainWidth=8;
    public int mainHeight=8;

    public int blockLengthInPixel=4;

    public DistrictConfig<Block> borderBlock=new DistrictConfig<Block>(new BlackWhiteBlock());
    /*
    public DistrictConfig<Block> borderBlock=new DistrictConfig<>(new BlackWhiteBlock(),
            new BlackWhiteBlock(),
            new BlackWhiteBlock(),
            new BlackWhiteBlock(),
            new BlackWhiteBlock(),
            new BlackWhiteBlock(),
            new BlackWhiteBlock(),
            new BlackWhiteBlock());
    */

    public DistrictConfig<Block> paddingBlock=new DistrictConfig<Block>(new BlackWhiteBlock());
    /*
    public DistrictConfig<Block> paddingBlock=new DistrictConfig<>(new BlackWhiteBlock(),
            new BlackWhiteBlock(),
            new BlackWhiteBlock(),
            new BlackWhiteBlock(),
            new BlackWhiteBlock(),
            new BlackWhiteBlock(),
            new BlackWhiteBlock(),
            new BlackWhiteBlock());
    */

    public DistrictConfig<Block> metaBlock=new DistrictConfig<Block>(new BlackWhiteBlock());
    /*
    public DistrictConfig<Block> metaBlock=new DistrictConfig<>(new BlackWhiteBlock(),
            new BlackWhiteBlock(),
            new BlackWhiteBlock(),
            new BlackWhiteBlock(),
            new BlackWhiteBlock(),
            new BlackWhiteBlock(),
            new BlackWhiteBlock(),
            new BlackWhiteBlock());
    */

    public DistrictConfig<Block> mainBlock=new DistrictConfig<Block>(new BlackWhiteBlock());

    public Map<String,Object> hints=new HashMap<>();
}
