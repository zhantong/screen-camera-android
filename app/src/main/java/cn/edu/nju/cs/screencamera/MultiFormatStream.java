package cn.edu.nju.cs.screencamera;


/**
 * Created by zhantong on 2016/12/17.
 */

public class MultiFormatStream {
    public static StreamDecode.CallBack getCallBack(BarcodeFormat barcodeFormat) {
        return getCallBack(barcodeFormat, false);
    }

    public static StreamDecode.CallBack getCallBack(BarcodeFormat barcodeFormat, boolean isFile) {
        StreamDecode.CallBack callBack;
        if (isFile) {
            switch (barcodeFormat) {
                case BLACK_WHITE_CODE_ML:
                    callBack = new BlackWhiteCodeMLFile();
                    break;
                case COLOR_CODE_ML:
                    callBack = new ColorCodeMLFile();
                    break;
                case RD_CODE_ML:
                    callBack = new RDCodeMLFile();
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        } else {
            switch (barcodeFormat) {
                case SHIFT_CODE:
                    callBack = new ShiftCodeStream();
                    break;
                case SHIFT_CODE_COLOR:
                    callBack = new ShiftCodeColorStream();
                    break;

                case SHIFT_CODE_ML:
                    callBack = new ShiftCodeMLStream();
                    break;
                case SHIFT_CODE_COLOR_ML:
                    callBack = new ShiftCodeColorMLStream();
                    break;
                case BLACK_WHITE_CODE_ML:
                    callBack = new BlackWhiteCodeMLStream();
                    break;
                case COLOR_CODE_ML:
                    callBack = new ColorCodeMLStream();
                    break;
                case BLACK_WHITE_CODE_WITH_BAR:
                    callBack = new BlackWhiteCodeWithBarStream();
                    break;
                case RD_CODE_ML:
                    callBack = new RDCodeMLStream();
                    break;
                case BLACK_WHITE_CODE:
                    callBack = new BlackWhiteCodeStream();
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }
        return callBack;
    }
}
