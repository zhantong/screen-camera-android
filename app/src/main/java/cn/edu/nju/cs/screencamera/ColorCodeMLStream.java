package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 2017/5/15.
 */

public class ColorCodeMLStream extends BlackWhiteCodeMLStream {

    public ColorCodeMLStream() {
    }

    BlackWhiteCodeML getBarcodeInstance(MediateBarcode mediateBarcode) {
        return new ColorCodeML(mediateBarcode);
    }

    void sampleContent(BlackWhiteCodeML blackWhiteCodeML) {
        blackWhiteCodeML.mediateBarcode.getContent(blackWhiteCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN), RawImage.CHANNLE_U);
        blackWhiteCodeML.mediateBarcode.getContent(blackWhiteCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN), RawImage.CHANNLE_V);
    }
}
