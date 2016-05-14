package cn.edu.nju.cs.screencamera;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by zhantong on 16/5/12.
 */
public class New {
    private static final String TAG="VideoToFrames";
    private static final boolean VERBOSE=true;
    private static final long DEFAULT_TIMEOUT_US = 10000;
    private LinkedBlockingQueue<byte[]> mQueue;

    private boolean saveFrames=false;
    private String OUTPUT_DIR;


    public New(LinkedBlockingQueue<byte[]> queue){
        mQueue=queue;
    }
    public void setSaveFrames(String dir) throws IOException{
        File theDir=new File(dir);
        if(!theDir.exists()){
            theDir.mkdirs();
        } else if(!theDir.isDirectory()){
            throw new IOException("Not a directory");
        }
        OUTPUT_DIR=theDir.getAbsolutePath()+"/";
        saveFrames=true;
    }
    public void videoDecode(String videoFilePath) throws IOException{
        MediaExtractor extractor=null;
        MediaCodec decoder=null;
        try {
            File videoFile = new File(videoFilePath);
            extractor = new MediaExtractor();
            extractor.setDataSource(videoFile.toString());
            int trackIndex = selectTrack(extractor);
            if (trackIndex < 0) {
                throw new RuntimeException("No video track found in " + videoFilePath);
            }
            extractor.selectTrack(trackIndex);
            MediaFormat mediaFormat = extractor.getTrackFormat(trackIndex);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            decoder = MediaCodec.createDecoderByType(mime);
            if(isFlexibleFormatSupported(decoder.getCodecInfo().getCapabilitiesForType(mime))){
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
                Log.i(TAG,"set decode color format to COLOR_FormatYUV420Flexible");
            }else{
                Log.i(TAG,"unable to set decode color format to COLOR_FormatYUV420Flexible, codec not supported");
            }
            decodeFramesToImage(decoder,extractor,mediaFormat);
            decoder.stop();
        }finally {
            if (decoder != null) {
                decoder.stop();
                decoder.release();
                decoder = null;
            }
            if (extractor != null) {
                extractor.release();
                extractor = null;
            }
        }
    }
    private boolean isFlexibleFormatSupported(MediaCodecInfo.CodecCapabilities caps){
        for(int c:caps.colorFormats){
            if(c== MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible){
                return true;
            }
        }
        return false;
    }
    private void decodeFramesToImage(MediaCodec decoder,MediaExtractor extractor,MediaFormat mediaFormat){
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS=false;
        boolean sawOutputEOS=false;
        decoder.configure(mediaFormat,null,null,0);
        decoder.start();
        int outputFrameCount = 0;
        while (!sawOutputEOS){
            if(!sawInputEOS) {
                int inputBufferId = decoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
                if (inputBufferId >= 0) {
                    ByteBuffer inputBuffer = decoder.getInputBuffer(inputBufferId);
                    int sampleSize = extractor.readSampleData(inputBuffer, 0);
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inputBufferId, 0, 0, 0l, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        sawInputEOS = true;
                    } else {
                        long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTimeUs, 0);
                        extractor.advance();
                    }
                }
            }
            int outputBufferId=decoder.dequeueOutputBuffer(info,DEFAULT_TIMEOUT_US);
            if(outputBufferId>=0){
                if((info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0){
                    sawOutputEOS=true;
                }
                boolean doRender = (info.size != 0);
                if(doRender){
                    outputFrameCount++;
                    Image image=decoder.getOutputImage(outputBufferId);
                    System.out.println(image.getFormat());
                    /*
                    ByteBuffer buffer=image.getPlanes()[0].getBuffer();
                    byte[] arr=new byte[buffer.remaining()];
                    buffer.get(arr);
                    if(mQueue!=null){
                        try {
                            mQueue.put(arr);
                        }catch (InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                    */
                    if(saveFrames){
                        String fileName=OUTPUT_DIR+String.format("frame-%05d.yuv", outputFrameCount);
                        dumpFile(fileName,getDataFromImage(image));
                    }
                    image.close();
                    decoder.releaseOutputBuffer(outputBufferId, true);
                }
            }
        }

    }
    private static int selectTrack(MediaExtractor extractor) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                if (VERBOSE) {
                    Log.d(TAG, "Extractor selected track " + i + " (" + mime + "): " + format);
                }
                return i;
            }
        }

        return -1;
    }
    private static boolean isImageFormatSupported(Image image) {
        int format = image.getFormat();
        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                return true;
        }
        return false;
    }
    /*
    private static byte[] getDataFromImage(Image image) {
        if(!isImageFormatSupported(image)){
            throw new RuntimeException("can't convert Image to byte array, format "+image.getFormat());
        }
        int width= image.getWidth();
        int height=image.getHeight();
        byte[] raw=new byte[0];
        int offset=0;
        for(int pos=0;pos<2;pos++){
            ByteBuffer buffer=image.getPlanes()[pos].getBuffer();
            byte[] arr=new byte[buffer.remaining()];
            buffer.get(arr);
            for(int i=0;i<20;i++){
                System.out.print(arr[i]);
            }
            System.out.println();
            System.out.println("array length:"+arr.length);
            byte[] newRaw=new byte[raw.length+arr.length];
            System.arraycopy(raw,0,newRaw,0,raw.length);
            System.arraycopy(arr,0,newRaw,raw.length,arr.length);
            raw=newRaw;
        }
        return raw;
    }
    */

    private static byte[] getDataFromImage(Image image) {
        if(!isImageFormatSupported(image)){
            throw new RuntimeException("can't convert Image to byte array, format "+image.getFormat());
        }
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        int rowStride, pixelStride;
        byte[] data = null;
        // Read image data
        Image.Plane[] planes = image.getPlanes();
        // Check image validity
        ByteBuffer buffer = null;
        int offset = 0;
        data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        if(VERBOSE) Log.v(TAG, "get data from " + planes.length + " planes");
        for (int i = 0; i < planes.length; i++) {
            int shift = (i == 0) ? 0 : 1;
            buffer = planes[i].getBuffer();
            System.out.println("position:"+buffer.position()+"\tlimit:"+buffer.limit()+"\tcapacity:"+buffer.capacity());
            rowStride = planes[i].getRowStride();
            pixelStride = planes[i].getPixelStride();
            if (VERBOSE) {
                Log.v(TAG, "pixelStride " + pixelStride);
                Log.v(TAG, "rowStride " + rowStride);
                Log.v(TAG, "width " + width);
                Log.v(TAG, "height " + height);
            }
            // For multi-planar yuv images, assuming yuv420 with 2x2 chroma subsampling.
            int w = crop.width() >> shift;
            int h = crop.height() >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int bytesPerPixel = ImageFormat.getBitsPerPixel(format) / 8;
                int length;
                if (pixelStride == bytesPerPixel) {
                    // Special case: optimized read of the entire row
                    length = w * bytesPerPixel;
                    buffer.get(data, offset, length);
                    offset += length;
                } else {
                    // Generic case: should work for any pixelStride but slower.
                    // Use intermediate buffer to avoid read byte-by-byte from
                    // DirectByteBuffer, which is very bad for performance
                    length = (w - 1) * pixelStride + bytesPerPixel;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[offset++] = rowData[col * pixelStride];
                    }
                }
                // Advance buffer the remainder of the row stride
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
            if (VERBOSE) Log.v(TAG, "Finished reading data from plane " + i);
        }
        return data;
    }

    private static void dumpFile(String fileName, byte[] data) {
        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(fileName);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to create output file " + fileName, ioe);
        }
        try {
            outStream.write(data);
            outStream.close();
        } catch (IOException ioe) {
            throw new RuntimeException("failed writing data to file " + fileName, ioe);
        }
    }
}
