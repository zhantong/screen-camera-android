package cn.edu.nju.cs.screencamera;


/**
 * Created by zhantong on 2016/12/17.
 */

public class ShiftCodeColorStream extends ShiftCodeStream {
    protected ShiftCode getShiftCode(MediateBarcode mediateBarcode) {
        return new ShiftCodeColor(mediateBarcode);
    }
}
