package cn.edu.nju.cs.screencamera;

import java.util.Map;

/**
 * Created by zhantong on 2017/6/4.
 */

public class RDCodeMLStream extends ColorCodeMLStream{

    public RDCodeMLStream(Map<DecodeHintType, ?> hints) {
        super(hints);
    }
    BlackWhiteCodeML getBarcodeInstance(MediateBarcode mediateBarcode,Map<DecodeHintType,?> hints){
        return new RDCodeML(mediateBarcode,hints);
    }
    BarcodeConfig getBarcodeConfigInstance(){
        return new RDCodeMLConfig();
    }
}
