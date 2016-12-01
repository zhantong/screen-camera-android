package cn.edu.nju.cs.screencamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by zhantong on 2016/11/23.
 */

public class ParseImage {
    private static final String TAG="ParseImage";
    public ParseImage(String filePath){
        Map<DecodeHintType,Object> hints=new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.RS_ERROR_CORRECTION_SIZE,12);
        hints.put(DecodeHintType.RS_ERROR_CORRECTION_LEVEL,0.1);
        hints.put(DecodeHintType.RAPTORQ_NUMBER_OF_SOURCE_BLOCKS,1);

        Log.i(TAG, "decoding video");
        ShiftCodeMLVideo shiftCodeMLVideo=new ShiftCodeMLVideo(filePath,hints);
    }
    public ParseImage(CameraPreview cameraPreview){
        Map<DecodeHintType,Object> hints=new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.RS_ERROR_CORRECTION_SIZE,12);
        hints.put(DecodeHintType.RS_ERROR_CORRECTION_LEVEL,0.1);
        hints.put(DecodeHintType.RAPTORQ_NUMBER_OF_SOURCE_BLOCKS,1);

        Log.i(TAG,"decoding camera");
        ShiftCodeCamera shiftCodeCamera=new ShiftCodeCamera(cameraPreview,hints);
    }
    public ParseImage(String filePath,boolean isDataFile){
        Map<DecodeHintType,Object> hints=new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.RS_ERROR_CORRECTION_SIZE,12);
        hints.put(DecodeHintType.RS_ERROR_CORRECTION_LEVEL,0.1);
        hints.put(DecodeHintType.RAPTORQ_NUMBER_OF_SOURCE_BLOCKS,1);

        ShiftCodeMLFile shiftCodeMLFile=new ShiftCodeMLFile(filePath,hints);
    }
    private static RawImage getRawImage(String imageFilePath) throws NotFoundException{
        if(imageFilePath.endsWith(".yuv")){
            return getRawImageYuv(imageFilePath);
        }
        return getRawImageNormal(imageFilePath);
    }
    private static RawImage getRawImageNormal(String imageFilePath) throws NotFoundException{
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(imageFilePath, options);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bitmap.getWidth() * bitmap.getHeight() * 4);
        bitmap.copyPixelsToBuffer(byteBuffer);
        bitmap.recycle();
        RawImage rawImage=new RawImage(byteBuffer.array(),bitmap.getWidth(),bitmap.getHeight(),RawImage.COLOR_TYPE_RGB);
        return rawImage;
    }
    private static RawImage getRawImageYuv(String imageFilePath) throws  NotFoundException{
        String fileName=Files.getNameWithoutExtension(imageFilePath);
        int[] widthAndHeight=Utils.extractResolution(fileName);
        if(widthAndHeight==null){
            throw new IllegalArgumentException("cannot infer resolution from file name "+fileName);
        }
        byte[] data=null;
        try {
            data=Files.toByteArray(new File(imageFilePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        RawImage rawImage=new RawImage(data,widthAndHeight[0],widthAndHeight[1],RawImage.COLOR_TYPE_YUV);
        return rawImage;
    }
}
