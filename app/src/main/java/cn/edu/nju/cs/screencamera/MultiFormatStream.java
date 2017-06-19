package cn.edu.nju.cs.screencamera;

import java.util.EnumMap;
import java.util.Map;

/**
 * Created by zhantong on 2016/12/17.
 */

public class MultiFormatStream {
    public static StreamDecode getStreamDecode(BarcodeFormat barcodeFormat){
        return getStreamDecode(barcodeFormat,false);
    }
    public static StreamDecode getStreamDecode(BarcodeFormat barcodeFormat,boolean isFile){
        StreamDecode streamDecode;

        Map<DecodeHintType,Object> hints=new EnumMap<>(DecodeHintType.class);
        //hints.put(DecodeHintType.RS_ERROR_CORRECTION_SIZE,12);
        hints.put(DecodeHintType.RS_ERROR_CORRECTION_SIZE,8);
        hints.put(DecodeHintType.RS_ERROR_CORRECTION_LEVEL,0.1);
        hints.put(DecodeHintType.RAPTORQ_NUMBER_OF_SOURCE_BLOCKS,1);
        hints.put(DecodeHintType.NUMBER_OF_RANDOM_BARCODES,100);
        if(isFile){
            switch (barcodeFormat){
                case BLACK_WHITE_CODE_ML:
                    streamDecode=new BlackWhiteCodeMLFile(hints);
                    break;
                case COLOR_CODE_ML:
                    streamDecode=new ColorCodeMLFile(hints);
                    break;
                case RD_CODE_ML:
                    streamDecode=new RDCodeMLFile(hints);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }else {
            switch (barcodeFormat) {
                case SHIFT_CODE:
                    streamDecode = new ShiftCodeStream(hints);
                    break;
                case SHIFT_CODE_COLOR:
                    streamDecode = new ShiftCodeColorStream(hints);
                    break;

                case SHIFT_CODE_ML:
                    streamDecode = new ShiftCodeMLStream(hints);
                    break;
                case SHIFT_CODE_COLOR_ML:
                    streamDecode = new ShiftCodeColorMLStream(hints);
                    break;
                case BLACK_WHITE_CODE_ML:
                    streamDecode = new BlackWhiteCodeMLStream(hints);
                    break;
                case COLOR_CODE_ML:
                    streamDecode = new ColorCodeMLStream(hints);
                    break;
                case BLACK_WHITE_CODE_WITH_BAR:
                    streamDecode = new BlackWhiteCodeWithBarStream(hints);
                    break;
                case RD_CODE_ML:
                    streamDecode = new RDCodeMLStream(hints);
                    break;
                case BLACK_WHITE_CODE:
                    streamDecode = new BlackWhiteCodeStream(hints);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }
        return streamDecode;
    }
}
