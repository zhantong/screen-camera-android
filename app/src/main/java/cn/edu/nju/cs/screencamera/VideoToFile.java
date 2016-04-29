package cn.edu.nju.cs.screencamera;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by zhantong on 16/4/29.
 */
public class VideoToFile extends StreamToFile {
    private static final String TAG = "VideoToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log
    private VideoToFrames videoToFrames;
    public VideoToFile(TextView debugView, TextView infoView, Handler handler, BarcodeFormat format,String truthFilePath) {
        super(debugView, infoView, handler,format,truthFilePath);
    }
    public int getImgColorType(){
        return 0;
    }
    public void beforeDataDecoded(){
        videoToFrames.stopExtract();
    }
    public void toFile(String fileName,final String videoFilePath){
        Log.i(TAG,"process video file");
        final LinkedBlockingQueue<byte[]> frameQueue = new LinkedBlockingQueue<>();
        int[] widthAndHeight=frameWidthAndHeight(videoFilePath);
        int frameWidth=widthAndHeight[0];
        int frameHeight=widthAndHeight[1];
        videoToFrames = new VideoToFrames();
        try {
            videoToFrames.doExtract(videoFilePath,frameQueue);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        /*
        try {
            videoToFrames.setSaveFrames(Environment.getExternalStorageDirectory() + "/captueFrames");
        }catch (IOException e){
            e.printStackTrace();
        }
        */
        streamToFile(frameQueue, frameWidth, frameHeight, fileName);
    }
    private int[] frameWidthAndHeight(String videoFilePath){
        File inputFile = new File(videoFilePath);
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(inputFile.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        int trackIndex = selectTrack(extractor);
        extractor.selectTrack(trackIndex);
        MediaFormat format = extractor.getTrackFormat(trackIndex);
        int imgWidth = format.getInteger(MediaFormat.KEY_WIDTH);
        int imgHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
        return new int[] {imgWidth,imgHeight};
    }
    private int selectTrack(MediaExtractor extractor) {
        // Select the first video track we find, ignore the rest.
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
