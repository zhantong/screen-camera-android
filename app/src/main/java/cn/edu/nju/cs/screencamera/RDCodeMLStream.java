package cn.edu.nju.cs.screencamera;


/**
 * Created by zhantong on 2017/6/4.
 */

public class RDCodeMLStream extends ColorCodeMLStream {

    public RDCodeMLStream() {
    }

    BlackWhiteCodeML getBarcodeInstance(MediateBarcode mediateBarcode) {
        return new RDCodeML(mediateBarcode);
    }

    BarcodeConfig getBarcodeConfigInstance() {
        return new RDCodeMLConfig();
    }
}
