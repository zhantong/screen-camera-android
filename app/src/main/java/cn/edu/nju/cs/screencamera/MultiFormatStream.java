package cn.edu.nju.cs.screencamera;


import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;

import java.io.IOException;

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

    public static StreamDecode getInstance(BarcodeFormat barcodeFormat, String inputFilePath, String outputFilePath) {
        ContentInfoUtil util = new ContentInfoUtil();
        ContentInfo info = null;
        try {
            info = util.findMatch(inputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (info == null) {
            return null;
        }
        StreamDecode streamDecode = new StreamDecode();
        StreamDecode.CallBack callBack;
        String mimeType = info.getMimeType();
        System.out.println(mimeType);
        if (mimeType.startsWith("video")) {
            streamDecode.setVideo(inputFilePath);
            callBack = MultiFormatStream.getCallBack(barcodeFormat);
        } else if (mimeType.startsWith("image")) {
            streamDecode.setImage(inputFilePath);
            callBack = MultiFormatStream.getCallBack(barcodeFormat);
        } else if (mimeType.startsWith("application/json")) {
            streamDecode.setJsonFile(inputFilePath);
            callBack = MultiFormatStream.getCallBack(barcodeFormat, true);
        } else {
            throw new IllegalArgumentException();
        }
        streamDecode.setCallBack(callBack);
        if (outputFilePath != null) {
            streamDecode.setOutputFilePath(outputFilePath);
        }
        return streamDecode;
    }

    public static StreamDecode getInstance(BarcodeFormat barcodeFormat, CameraPreview cameraPreview, String outputFilePath) {
        StreamDecode streamDecode = new StreamDecode();
        StreamDecode.CallBack callBack = MultiFormatStream.getCallBack(barcodeFormat);
        streamDecode.setCallBack(callBack);
        streamDecode.setCamera(cameraPreview);
        streamDecode.setOutputFilePath(outputFilePath);
        return streamDecode;
    }
}
