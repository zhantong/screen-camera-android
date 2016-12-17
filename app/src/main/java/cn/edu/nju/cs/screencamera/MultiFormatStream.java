package cn.edu.nju.cs.screencamera;

import java.util.EnumMap;
import java.util.Map;

/**
 * Created by zhantong on 2016/12/17.
 */

public class MultiFormatStream {
    public static void decode(BarcodeFormat barcodeFormat,String videoFilePath,String ImageFilePath,CameraPreview cameraPreview){
        StreamDecode streamDecode;

        Map<DecodeHintType,Object> hints=new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.RS_ERROR_CORRECTION_SIZE,12);
        hints.put(DecodeHintType.RS_ERROR_CORRECTION_LEVEL,0.1);
        hints.put(DecodeHintType.RAPTORQ_NUMBER_OF_SOURCE_BLOCKS,1);

        switch (barcodeFormat){
            case SHIFTCODE:
                streamDecode=new ShiftCodeStream(hints);
                break;
            case SHIFTCODECOLOR:
                streamDecode=new ShiftCodeColorStream(hints);
                break;
            /*
            case SHIFTCODEML:
                streamDecode=new ShiftCodeMLStream(hints);
                break;
            */
            default:
                throw new IllegalArgumentException();
        }
        if(videoFilePath!=null){
            streamDecode.setVideo(videoFilePath);
        }else if(ImageFilePath!=null){
            streamDecode.setImage(ImageFilePath);
        }else {
            streamDecode.setCamera(cameraPreview);
        }
        streamDecode.start();
    }
}
