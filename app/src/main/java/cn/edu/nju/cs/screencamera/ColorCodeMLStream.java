package cn.edu.nju.cs.screencamera;

import java.util.Map;


/**
 * Created by zhantong on 2017/5/15.
 */

public class ColorCodeMLStream extends BlackWhiteCodeMLStream {

    public ColorCodeMLStream(Map<DecodeHintType,?> hints){
        super(hints);
    }
    BlackWhiteCodeML getBarcodeInstance(MediateBarcode mediateBarcode,Map<DecodeHintType,?> hints){
        return new ColorCodeML(mediateBarcode,hints);
    }
    BarcodeConfig getBarcodeConfigInstance(){
        return new ColorCodeMLConfig();
    }
    void sampleContent(BlackWhiteCodeML blackWhiteCodeML){
        blackWhiteCodeML.mediateBarcode.getContent(blackWhiteCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN),RawImage.CHANNLE_U);
        blackWhiteCodeML.mediateBarcode.getContent(blackWhiteCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN),RawImage.CHANNLE_V);
    }
}
