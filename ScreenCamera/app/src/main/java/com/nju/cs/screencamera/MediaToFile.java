package com.nju.cs.screencamera;

import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 从相机,视频,图片识别二维码的基础类
 * 包含通用的UI通信显示方法,和图像处理方法等
 */
public class MediaToFile extends FileToImg {
    private static final String TAG = "MediaToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log
    private TextView debugView;//输出处理信息的TextView
    private TextView infoView;//输出全局信息的TextView
    private Handler handler;//与UI进程通信
    int barCodeWidth = (frameBlackLength + frameVaryLength) * 2 + contentLength;//二维码边长
    /**
     * 构造函数,获取必须的参数
     *
     * @param debugView 实例
     * @param infoView  实例
     * @param handler   实例
     */
    public MediaToFile(TextView debugView, TextView infoView, Handler handler) {
        this.debugView = debugView;
        this.infoView = infoView;
        this.handler = handler;
    }
    /**
     * 更新处理信息,即将此线程的信息输出到UI
     *
     * @param index            帧编号
     * @param lastSuccessIndex 最后识别成功的帧编号
     * @param frameAmount      二维码帧总数
     * @param count            已处理的帧个数
     */
    protected void updateDebug(int index, int lastSuccessIndex, int frameAmount, int count) {
        final String text = "当前:" + index + "已识别:" + lastSuccessIndex + "帧总数:" + frameAmount + "已处理:" + count;
        handler.post(new Runnable() {
            @Override
            public void run() {
                debugView.setText(text);
            }
        });
    }
    /**
     * 更新全局信息,即将此线程的信息输出到UI
     *
     * @param msg 信息
     */
    protected void updateInfo(String msg) {
        final String text = msg;
        handler.post(new Runnable() {
            @Override
            public void run() {
                infoView.setText(text);
            }
        });
    }
    public int getIndex(Matrix matrix) throws CRCCheckException {
        String row = matrix.sampleRow(barCodeWidth, barCodeWidth, frameBlackLength);
        if (VERBOSE) {
            Log.d(TAG, "index row:" + row);
        }
        int index = Integer.parseInt(row.substring(frameBlackLength+frameVaryLength+1, frameBlackLength+frameVaryLength+1 + 16), 2);
        int crc = Integer.parseInt(row.substring(frameBlackLength+frameVaryLength+1 + 16, frameBlackLength+frameVaryLength+1 + 24), 2);
        int truth = CRC8.calcCrc8(index);
        if (VERBOSE) {
            Log.d(TAG, "CRC check: index:" + index + " CRC:" + crc + " truth:" + truth);
        }
        if (crc != truth) {
            throw CRCCheckException.getNotFoundInstance();
        }
        return index;
    }
    /**
     * 获取此帧中二维码记录的帧总数
     *
     * @param matrix 包含有像素等信息的Matrix
     * @return 此帧中二维码记录的帧总数
     * @throws CRCCheckException 解析帧总数时,如果CRC校验失败,则抛出异常
     */
    public int getFrameAmount(Matrix matrix) throws CRCCheckException {
        String row = matrix.sampleRow(barCodeWidth, barCodeWidth, frameBlackLength);
        int frameAmount = Integer.parseInt(row.substring(frameBlackLength+frameVaryLength+1 + 24, frameBlackLength+frameVaryLength+1 + 40), 2);
        int crc = Integer.parseInt(row.substring(frameBlackLength+frameVaryLength+1 + 40, frameBlackLength+frameVaryLength+1 + 48), 2);
        if (crc != CRC8.calcCrc8(frameAmount)) {
            System.out.println(row);
            System.out.println(frameAmount);
            System.out.println(crc+" "+CRC8.calcCrc8(frameAmount));
            throw CRCCheckException.getNotFoundInstance();
        }
        return frameAmount;
    }
    /**
     * 将Matrix内的二维码信息转换为byte数组
     *
     * @param matrix 包含有像素等信息的Matrix
     * @return byte数组, 此二维码包含的信息
     * @throws ReedSolomonException 纠错失败则抛出异常
     */
    public byte[] imgToArray(Matrix matrix) throws ReedSolomonException {
        BinaryMatrix binaryMatrix = matrix.sampleGrid(barCodeWidth, barCodeWidth);
        //binaryMatrix.print();
        return binaryMatrixToArray(binaryMatrix);
    }
    /**
     * 将BinaryMatrix内的二维码信息转换为byte数组
     *
     * @param binaryMatrix 包含有像素等信息的BinaryMatrix
     * @return byte数组, 此二维码包含的信息
     * @throws ReedSolomonException 纠错失败则抛出异常
     */
    public byte[] binaryMatrixToArray(BinaryMatrix binaryMatrix) throws ReedSolomonException {
        int startOffset = frameBlackLength + frameVaryLength;
        int stopOffset = startOffset + contentLength;
        int contentByteNum = contentLength * contentLength / 8;
        int realByteNum = contentByteNum - ecByteNum;
        int[] result = new int[contentByteNum];
        binaryMatrix.toArray(startOffset, startOffset, stopOffset, stopOffset, result);
        ReedSolomonDecoder decoder = new ReedSolomonDecoder(GenericGF.QR_CODE_FIELD_256);
        try {
            decoder.decode(result, ecByteNum);
        } catch (Exception e) {
            throw new ReedSolomonException("error correcting failed");
        }
        byte[] res = new byte[realByteNum];
        for (int i = 0; i < realByteNum; i++) {
            res[i] = (byte) result[i];
        }
        return res;
    }
    /**
     * 将每个二维码帧的byte数组构成的队列写入到文件
     *
     * @param buffer 包含二维码byte数组类型信息的队列
     * @param file   需要写入的文件
     */
    public void bufferToFile(List<byte[]> buffer, File file) {
        byte[] oldLast = buffer.get(buffer.size() - 1);
        buffer.remove(buffer.size() - 1);
        buffer.add(cutArrayBack(oldLast, -128));
        OutputStream os;
        try {
            os = new FileOutputStream(file);
            for (byte[] b : buffer) {
                os.write(b);
            }
            os.close();
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
    }

    /**
     * 对最后一帧,消除无用填充信息
     * 最后一帧的数据不会填满整个信息空间,此时通过预先设定的patten,将附加无用信息删掉
     *
     * @param old    最后一帧byte数组
     * @param intCut 附加信息开始处特征值
     * @return 删除附加无用信息的byte数组
     */
    protected byte[] cutArrayBack(byte[] old, int intCut) {
        byte byteCut = (byte) intCut;
        int stopIndex = 0;
        for (int i = old.length - 1; i > 0; i--) {
            if (old[i] == byteCut) {
                stopIndex = i;
                break;
            }
        }
        byte[] array = new byte[stopIndex];
        System.arraycopy(old, 0, array, 0, stopIndex);
        return array;
    }
}
