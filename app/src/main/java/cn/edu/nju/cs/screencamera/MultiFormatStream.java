package cn.edu.nju.cs.screencamera;


import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;

import java.io.IOException;

/**
 * Created by zhantong on 2016/12/17.
 */

public class MultiFormatStream {
    public static StreamDecode getStreamDecode(BarcodeFormat barcodeFormat) {
        return getStreamDecode(barcodeFormat, false);
    }

    public static StreamDecode getStreamDecode(BarcodeFormat barcodeFormat, boolean isFile) {
        StreamDecode streamDecode;
        if (isFile) {
            switch (barcodeFormat) {
                case BLACK_WHITE_CODE_ML:
                    streamDecode = new BlackWhiteCodeMLFile();
                    break;
                case COLOR_CODE_ML:
                    streamDecode = new ColorCodeMLFile();
                    break;
                case RD_CODE_ML:
                    streamDecode = new RDCodeMLFile();
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        } else {
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

    public static StreamDecode getInstance(BarcodeConfig barcodeConfig, String inputFilePath) {
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
        BarcodeFormat barcodeFormat = barcodeConfig.barcodeFormat;
        StreamDecode streamDecode;
        String mimeType = info.getMimeType();
        if (mimeType.startsWith("video")) {
            streamDecode = getStreamDecode(barcodeFormat);
            streamDecode.setVideo(inputFilePath);
        } else if (mimeType.startsWith("image")) {
            streamDecode = getStreamDecode(barcodeFormat);
            streamDecode.setImage(inputFilePath);
        } else if (mimeType.startsWith("application/json")) {
            streamDecode = getStreamDecode(barcodeFormat, true);
            streamDecode.setJsonFile(inputFilePath);
        } else {
            throw new IllegalArgumentException();
        }
        streamDecode.setBarcodeConfig(barcodeConfig);
        return streamDecode;
    }

    public static StreamDecode getInstance(BarcodeConfig barcodeConfig, CameraPreview cameraPreview) {
        StreamDecode streamDecode = getStreamDecode(barcodeConfig.barcodeFormat);
        streamDecode.setCamera(cameraPreview);
        streamDecode.setBarcodeConfig(barcodeConfig);
        return streamDecode;
    }
}
