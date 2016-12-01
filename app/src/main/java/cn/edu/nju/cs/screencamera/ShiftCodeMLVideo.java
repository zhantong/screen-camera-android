package cn.edu.nju.cs.screencamera;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by zhantong on 2016/11/28.
 */

public class ShiftCodeMLVideo extends ShiftCodeMLStream implements ShiftCodeMLStream.Callback {
    private VideoToFrames videoToFrames;
    public ShiftCodeMLVideo(String videoFilePath,Map<DecodeHintType, ?> hints) {
        super(hints);
        setCallback(this);
        LinkedBlockingQueue<RawImage> frameQueue = new LinkedBlockingQueue<>(10);
        videoToFrames = new VideoToFrames();
        videoToFrames.setEnqueue(frameQueue);
        try {
            videoToFrames.decode(videoFilePath);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        try {
            stream(frameQueue);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBarcodeNotFound() {

    }

    @Override
    public void onCRCCheckFailed() {

    }

    @Override
    public void onBeforeDataDecoded() {
        videoToFrames.stopDecode();
    }
}
