package com.nju.cs.screencamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 识别相机,视频或图片中的二维码,处理后写入文件
 * 这是此APP的主线
 */
public class ImgToFile extends FileToImg {
    private static final String TAG = "ImgToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log
    private TextView debugView;//输出处理信息的TextView
    private TextView infoView;//输出全局信息的TextView
    private Handler handler;//与UI进程通信
    private CameraPreview mPreview;//相机
    private int imgWidth;//图像宽度
    private int imgHeight;//图像高度
    int barCodeWidth = (frameBlackLength + frameVaryLength) * 2 + contentLength;//二维码边长

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
    public ImgToFile(TextView debugView, TextView infoView, Handler handler, int imgWidth, int imgHeight, CameraPreview mPreview) {
        this.debugView = debugView;
        this.infoView = infoView;
        this.handler = handler;
        this.mPreview = mPreview;
        this.imgWidth = imgWidth;
        this.imgHeight = imgHeight;
    }

    /**
     * 更新处理信息,即将此线程的信息输出到UI
     *
     * @param index            帧编号
     * @param lastSuccessIndex 最后识别成功的帧编号
     * @param frameAmount      二维码帧总数
     * @param count            已处理的帧个数
     */
    private void updateDebug(int index, int lastSuccessIndex, int frameAmount, int count) {
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
    private void updateInfo(String msg) {
        final String text = msg;
        handler.post(new Runnable() {
            @Override
            public void run() {
                infoView.setText(text);
            }
        });
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

    /**
     * 对单个图片进行解码识别二维码
     * 注意这个方法只是拿来测试识别算法等
     *
     * @param filePath 图片路径
     */
    public void singleImg(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bitmap.getWidth() * bitmap.getHeight() * 4);
        bitmap.copyPixelsToBuffer(byteBuffer);
        bitmap.recycle();
        updateInfo("正在识别...");
        RGBMatrix rgbMatrix;
        try {
            rgbMatrix = new RGBMatrix(byteBuffer.array(), bitmap.getWidth(), bitmap.getHeight());
            rgbMatrix.perspectiveTransform(0, 0, barCodeWidth, 0, barCodeWidth, barCodeWidth, 0, barCodeWidth);
            rgbMatrix.frameIndex = getIndex(rgbMatrix);
        } catch (NotFoundException e) {
            Log.d(TAG, e.getMessage());
            return;
        } catch (CRCCheckException e) {
            Log.d(TAG, "CRC check failed");
            return;
        }
        Log.d(TAG, "frame index:" + rgbMatrix.frameIndex);
        int frameAmount;
        try {
            frameAmount = getFrameAmount(rgbMatrix);
        } catch (CRCCheckException e) {
            Log.d(TAG, "CRC check failed");
            return;
        }
        Log.d(TAG, "frame amount:" + frameAmount);
        byte[] stream;
        try {
            stream = imgToArray(rgbMatrix);
        } catch (ReedSolomonException e) {
            Log.d(TAG, e.getMessage());
            return;
        }
        Log.i(TAG, "done!");
        updateInfo("识别完成!");
    }


    /**
     * 获取此帧的二维码帧编号
     *
     * @param matrix 包含有像素等信息的Matrix
     * @return 此帧的二维码帧编号
     * @throws CRCCheckException 解析帧编号时,如果CRC校验失败,则抛出异常
     */
    public int getIndex(Matrix matrix) throws CRCCheckException {
        String row = matrix.sampleRow(barCodeWidth, barCodeWidth, frameBlackLength);
        if (VERBOSE) {
            Log.d(TAG, "index row:" + row);
        }
        int index = Integer.parseInt(row.substring(frameBlackLength, frameBlackLength + 16), 2);
        int crc = Integer.parseInt(row.substring(frameBlackLength + 16, frameBlackLength + 24), 2);
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
        int frameAmount = Integer.parseInt(row.substring(frameBlackLength + 24, frameBlackLength + 40), 2);
        int crc = Integer.parseInt(row.substring(frameBlackLength + 40, frameBlackLength + 48), 2);
        if (crc != CRC8.calcCrc8(frameAmount)) {
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
    private byte[] cutArrayBack(byte[] old, int intCut) {
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
