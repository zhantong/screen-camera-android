package cn.edu.nju.cs.screencamera;

import java.util.EnumMap;
import java.util.Map;

/**
 * Created by zhantong on 2016/12/17.
 */

public class MultiFormatStream {
    public static StreamDecode getStreamDecode(BarcodeFormat barcodeFormat){
        StreamDecode streamDecode;

        Map<DecodeHintType,Object> hints=new EnumMap<>(DecodeHintType.class);
        //hints.put(DecodeHintType.RS_ERROR_CORRECTION_SIZE,12);
        hints.put(DecodeHintType.RS_ERROR_CORRECTION_SIZE,8);
        hints.put(DecodeHintType.RS_ERROR_CORRECTION_LEVEL,0.1);
        hints.put(DecodeHintType.RAPTORQ_NUMBER_OF_SOURCE_BLOCKS,1);

        switch (barcodeFormat){
            case SHIFTCODE:
                streamDecode=new ShiftCodeStream(hints);
                break;
            case SHIFTCODECOLOR:
                streamDecode=new ShiftCodeColorStream(hints);
                break;

            case SHIFTCODEML:
                streamDecode=new ShiftCodeMLStream(hints);
                break;
            case SHIFTCODECOLORML:
                streamDecode=new ShiftCodeColorMLStream(hints);
                break;
            case BLACKWHITECODEML:
                streamDecode=new BlackWhiteCodeMLStream(hints);
                break;
            case COLORCODEML:
                streamDecode=new ColorCodeMLStream(hints);
                break;
            case BLACKWHITECODE:
                streamDecode=new BlackWhiteCodeWithBarStream(hints);
                break;
            case RDCODEML:
                streamDecode=new RDCodeMLStream(hints);
                break;
            default:
                throw new IllegalArgumentException();
        }
        return streamDecode;
    }
}
