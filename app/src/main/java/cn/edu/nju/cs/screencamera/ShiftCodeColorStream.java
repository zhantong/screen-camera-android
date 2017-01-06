package cn.edu.nju.cs.screencamera;

import java.util.Map;

/**
 * Created by zhantong on 2016/12/17.
 */

public class ShiftCodeColorStream extends ShiftCodeStream {
    public ShiftCodeColorStream(Map<DecodeHintType, ?> hints) {
        super(hints);
    }
    protected MediateBarcode getMediateBarcode(RawImage frame) throws NotFoundException {
        return new MediateBarcode(frame,new ShiftCodeColorConfig(),rectangle,RawImage.CHANNLE_U);
    }
    protected ShiftCode getShiftCode(MediateBarcode mediateBarcode){
        return new ShiftCodeColor(mediateBarcode,hints);
    }
}
