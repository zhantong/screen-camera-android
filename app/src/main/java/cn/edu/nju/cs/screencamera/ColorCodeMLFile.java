package cn.edu.nju.cs.screencamera;


/**
 * Created by zhantong on 2017/6/18.
 */

public class ColorCodeMLFile extends BlackWhiteCodeMLFile {

    public ColorCodeMLFile() {
    }

    BlackWhiteCodeML getBarcodeInstance(MediateBarcode mediateBarcode) {
        return new ColorCodeML(mediateBarcode);
    }
}
