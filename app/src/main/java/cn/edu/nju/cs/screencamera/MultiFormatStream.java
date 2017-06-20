package cn.edu.nju.cs.screencamera;


/**
 * Created by zhantong on 2016/12/17.
 */

public class MultiFormatStream {
    public static StreamDecode getStreamDecode(BarcodeFormat barcodeFormat){
        return getStreamDecode(barcodeFormat,false);
    }
    public static StreamDecode getStreamDecode(BarcodeFormat barcodeFormat,boolean isFile){
        StreamDecode streamDecode;
        if(isFile){
            switch (barcodeFormat){
                case BLACK_WHITE_CODE_ML:
                    streamDecode=new BlackWhiteCodeMLFile();
                    break;
                case COLOR_CODE_ML:
                    streamDecode=new ColorCodeMLFile();
                    break;
                case RD_CODE_ML:
                    streamDecode=new RDCodeMLFile();
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }else {
            switch (barcodeFormat) {
                case SHIFT_CODE:
                    streamDecode = new ShiftCodeStream();
                    break;
                case SHIFT_CODE_COLOR:
                    streamDecode = new ShiftCodeColorStream();
                    break;

                case SHIFT_CODE_ML:
                    streamDecode = new ShiftCodeMLStream();
                    break;
                case SHIFT_CODE_COLOR_ML:
                    streamDecode = new ShiftCodeColorMLStream();
                    break;
                case BLACK_WHITE_CODE_ML:
                    streamDecode = new BlackWhiteCodeMLStream();
                    break;
                case COLOR_CODE_ML:
                    streamDecode = new ColorCodeMLStream();
                    break;
                case BLACK_WHITE_CODE_WITH_BAR:
                    streamDecode = new BlackWhiteCodeWithBarStream();
                    break;
                case RD_CODE_ML:
                    streamDecode = new RDCodeMLStream();
                    break;
                case BLACK_WHITE_CODE:
                    streamDecode = new BlackWhiteCodeStream();
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }
        return streamDecode;
    }
}
