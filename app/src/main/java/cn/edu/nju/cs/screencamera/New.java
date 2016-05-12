package cn.edu.nju.cs.screencamera;

import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
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


    public New(LinkedBlockingQueue<byte[]> queue){
        mQueue=queue;
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
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            decoder = MediaCodec.createDecoderByType(mime);
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
    private void decodeFramesToImage(MediaCodec decoder,MediaExtractor extractor,MediaFormat mediaFormat){
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS=false;
        boolean sawOutputEOS=false;
        decoder.configure(mediaFormat,null,null,0);
        decoder.start();
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
                if(info.size!=0){
                    Image image=decoder.getOutputImage(outputBufferId);
                    ByteBuffer buffer=image.getPlanes()[0].getBuffer();
                    byte[] arr=new byte[buffer.remaining()];
                    buffer.get(arr);
                    image.close();
                    if(mQueue!=null){
                        try {
                            mQueue.put(arr);
                        }catch (InterruptedException e){
                            e.printStackTrace();
                        }
                    }
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
}
