package com.nju.cs.screencamera;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by zhantong on 15/12/13.
 */
public class VideoToFile extends MediaToFile {
    private static final String TAG = "ImgToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log
    /**
     * 构造函数,获取必须的参数
     *
     * @param debugView 实例
     * @param infoView  实例
     * @param handler   实例
     * @param imgWidth  图像宽度
     * @param imgHeight 图像高度
     */
    public VideoToFile(TextView debugView, TextView infoView, Handler handler, int imgWidth, int imgHeight) {
        super(debugView,infoView,handler,imgWidth,imgHeight);
    }
    /**
     * 对视频解码的帧队列处理
     * 所有帧都识别成功后写入到文件
     *
     * @param imgs 帧队列
     * @param file 需要写入的文件
     */
    public void videoToFile(String videoFilePath, LinkedBlockingQueue<byte[]> imgs, File file) {
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
        imgWidth = format.getInteger(MediaFormat.KEY_WIDTH);
        imgHeight = format.getInteger(MediaFormat.KEY_HEIGHT);

        int count = 0;
        int lastSuccessIndex = 0;
        int frameAmount = 0;
        List<byte[]> buffer = new LinkedList<>();
        byte[] img = {};
        RGBMatrix rgbMatrix;
        byte[] stream;
        int index = 0;
        while (true) {
            count++;
            updateInfo("正在识别...");
            try {
                img = imgs.take();
            } catch (InterruptedException e) {
                Log.d(TAG, e.getMessage());
            }
            updateDebug(index, lastSuccessIndex, frameAmount, count);
            try {
                rgbMatrix = new RGBMatrix(img, imgWidth, imgHeight);
                rgbMatrix.perspectiveTransform(0, 0, barCodeWidth, 0, barCodeWidth, barCodeWidth, 0, barCodeWidth);
                rgbMatrix.frameIndex = getIndex(rgbMatrix);
            } catch (NotFoundException e) {
                Log.d(TAG, e.getMessage());
                continue;
            } catch (CRCCheckException e) {
                Log.d(TAG, "CRC check failed");
                continue;
            }
            index = rgbMatrix.frameIndex;
            Log.i("frame " + index + "/" + count, "processing...");
            if (lastSuccessIndex == index) {
                Log.i("frame " + index + "/" + count, "same frame index!");
                continue;
            } else if (index - lastSuccessIndex != 1) {
                Log.i("frame " + index + "/" + count, "bad frame index!");
                continue;
            }
            try {
                stream = imgToArray(rgbMatrix);
            } catch (ReedSolomonException e) {
                Log.d(TAG, e.getMessage());
                continue;
            }
            buffer.add(stream);
            lastSuccessIndex = index;
            Log.i("frame " + index + "/" + count, "done!");
            updateDebug(index, lastSuccessIndex, frameAmount, count);
            if (lastSuccessIndex == frameAmount) {
                break;
            }
            if (frameAmount == 0) {
                try {
                    frameAmount = getFrameAmount(rgbMatrix);
                } catch (CRCCheckException e) {
                    Log.d(TAG, "CRC check failed");
                    continue;
                }
            }
            rgbMatrix = null;
        }
        updateInfo("识别完成!正在写入文件");
        Log.d("videoToFile", "total length:" + buffer.size());
        bufferToFile(buffer, file);
        updateInfo("写入文件成功!");

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
