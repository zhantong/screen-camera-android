package cn.edu.nju.cs.screencamera;


/**
 * Created by zhantong on 2017/3/20.
 */

public class ShiftCodeColorMLStream extends ShiftCodeMLStream {
    ShiftCodeML getBarcodeInstance(MediateBarcode mediateBarcode) {
        return new ShiftCodeColorML(mediateBarcode);
    }

    BarcodeConfig getBarcodeConfigInstance() {
        return new ShiftCodeColorMLConfig();
    }

    void sampleContent(BlackWhiteCodeML blackWhiteCodeML) {
        blackWhiteCodeML.mediateBarcode.getContent(blackWhiteCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN), RawImage.CHANNLE_U);
        blackWhiteCodeML.mediateBarcode.getContent(blackWhiteCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN), RawImage.CHANNLE_V);
    }
}
