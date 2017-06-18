package cn.edu.nju.cs.screencamera;

import java.util.Map;

/**
 * Created by zhantong on 2017/6/18.
 */

public class ColorCodeMLFile extends BlackWhiteCodeMLFile{

    public ColorCodeMLFile(Map<DecodeHintType, ?> hints) {
        super(hints);
    }

    BlackWhiteCodeML getBarcodeInstance(MediateBarcode mediateBarcode, Map<DecodeHintType,?> hints){
        return new ColorCodeML(mediateBarcode,hints);
    }
    BarcodeConfig getBarcodeConfigInstance(){
        return new ColorCodeMLConfig();
    }
}
