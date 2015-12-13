package com.nju.cs.screencamera;

import android.hardware.Camera;
import android.media.MediaCodec;
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
public class CameraToFile extends MediaToFile{
    private static final String TAG = "ImgToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log
    private CameraPreview mPreview;//相机
    /**
     * 构造函数,获取必须的参数
     *
     * @param debugView 实例
     * @param infoView  实例
     * @param handler   实例
     * @param imgWidth  图像宽度
     * @param imgHeight 图像高度
     * @param mPreview  实例
     */
    public CameraToFile(TextView debugView, TextView infoView, Handler handler, int imgWidth, int imgHeight, CameraPreview mPreview){
        super(debugView, infoView, handler,imgWidth,imgHeight);
        this.mPreview = mPreview;
    }
    /**
     * 从队列中取出预览帧进行处理,根据处理情况控制相机
     * 所有帧都识别成功后写入到文件
     *
     * @param imgs 帧队列
     * @param file 需要写入的文件
     */
    public void cameraToFile(LinkedBlockingQueue<byte[]> imgs, File file) {
        int count = 0;
        int lastSuccessIndex = 0;
        int frameAmount = 0;
        List<byte[]> buffer = new LinkedList<>();
        byte[] img = {};
        Matrix matrix;
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
                matrix = imgToMatrix(img);
            } catch (NotFoundException e) {
                if (lastSuccessIndex == 0) {
                    mPreview.focus();
                }
                Log.d(TAG, e.getMessage());
                continue;
            } catch (CRCCheckException e) {
                Log.d(TAG, "CRC check failed");
                continue;
            }
            index = matrix.frameIndex;
            Log.i("frame " + index + "/" + count, "processing...");
            if (lastSuccessIndex == index) {
                Log.i("frame " + index + "/" + count, "same frame index!");
                continue;
            } else if (index - lastSuccessIndex != 1) {
                Log.i("frame " + index + "/" + count, "bad frame index!");
                continue;
            }
            try {
                stream = imgToArray(matrix);
            } catch (ReedSolomonException e) {
                Log.d(TAG, e.getMessage());
                continue;
            }
            buffer.add(stream);
            lastSuccessIndex = index;
            Log.i("frame " + index + "/" + count, "done!");
            updateDebug(index, lastSuccessIndex, frameAmount, count);
            if (lastSuccessIndex == frameAmount) {
                mPreview.stop();
                break;
            }
            if (frameAmount == 0) {
                try {
                    frameAmount = getFrameAmount(matrix);
                } catch (CRCCheckException e) {
                    Log.d(TAG, "CRC check failed");
                    continue;
                }
            }
            matrix = null;
        }
        updateInfo("识别完成!正在写入文件");
        Log.d("cameraToFile", "total length:" + buffer.size());
        bufferToFile(buffer, file);
        updateInfo("写入文件成功!");
    }

    /**
     * 帧转换为Matrix,Matrix实例化时进行一些图像操作
     *
     * @param img 帧
     * @return 保存有此帧信息的Matrix
     * @throws NotFoundException 对图像处理时,不能找到二维码则抛出未找到异常
     * @throws CRCCheckException 解析帧编号时,如果CRC校验失败,则抛出异常
     */
    public Matrix imgToMatrix(byte[] img) throws NotFoundException, CRCCheckException {
        Matrix matrix = new Matrix(img, imgWidth, imgHeight);
        matrix.perspectiveTransform(0, 0, barCodeWidth, 0, barCodeWidth, barCodeWidth, 0, barCodeWidth);
        matrix.frameIndex = getIndex(matrix);
        return matrix;
    }
}
