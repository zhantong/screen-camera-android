package cn.edu.nju.cs.screencamera;


/**
 * Created by zhantong on 2016/11/29.
 */

public class ShiftCodeMLFile extends BlackWhiteCodeMLFile {
    BlackWhiteCodeML getBarcodeInstance(MediateBarcode mediateBarcode) {
        return new ShiftCodeML(mediateBarcode);
    }
}
