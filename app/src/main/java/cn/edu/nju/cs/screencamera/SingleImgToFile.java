package cn.edu.nju.cs.screencamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

import net.fec.openrq.parameters.FECParameters;

import java.nio.ByteBuffer;

/**
 * 识别图片中的二维码,测试用
 */
public class SingleImgToFile extends MediaToFile {
    private static final String TAG = "SingleImgToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log
    private static BarcodeFormat barcodeFormat;

    private Handler processHandler;

    /**
     * 构造函数,获取必须的参数
     *
     * @param handler   实例
     */
    public SingleImgToFile(Handler handler,BarcodeFormat format,String truthFilePath) {
        super(handler);
        barcodeFormat=format;
        if(!truthFilePath.equals("")) {
            setDebug(MatrixFactory.createMatrix(format), truthFilePath);
        }

        ProcessFrame processFrame=new ProcessFrame("process");
        processFrame.start();
        processHandler=new Handler(processFrame.getLooper(), processFrame);
        processHandler.sendMessage(processHandler.obtainMessage(ProcessFrame.WHAT_BARCODE_FORMAT,format));
        if(!truthFilePath.equals("")) {
            processHandler.sendMessage(processHandler.obtainMessage(ProcessFrame.WHAT_TRUTH_FILE_PATH,truthFilePath));
        }
    }

    /**
     * 对单个图片进行解码识别二维码
     * 注意这个方法只是拿来测试识别算法等
     *
     * @param filePath 图片路径
     */
    public void singleImg(String filePath) {
        final int NUMBER_OF_SOURCE_BLOCKS=1;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bitmap.getWidth() * bitmap.getHeight() * 4);
        bitmap.copyPixelsToBuffer(byteBuffer);
        bitmap.recycle();
        updateInfo("正在识别...");
        Matrix matrix;
        int fileByteNum;
        try {
            matrix=MatrixFactory.createMatrix(barcodeFormat,byteBuffer.array(),0, bitmap.getWidth(), bitmap.getHeight(),null);
            matrix.perspectiveTransform();
        } catch (NotFoundException e) {
            Log.d(TAG, e.getMessage());
            return;
        }
        try {
            fileByteNum = getFileByteNum(matrix);
        }catch (CRCCheckException e){
            Log.d(TAG, "head CRC check failed");
            return;
        }
        if(fileByteNum==0){
            Log.d(TAG,"wrong file byte number");
            return;
        }
        Log.i(TAG,"file is "+fileByteNum+" bytes");
        int length=matrix.realContentByteLength();
        FECParameters parameters = FECParameters.newParameters(fileByteNum, length, NUMBER_OF_SOURCE_BLOCKS);
        processHandler.sendMessage(processHandler.obtainMessage(ProcessFrame.WHAT_FEC_PARAMETERS,parameters));
        matrix.sampleContent();
        processHandler.sendMessage(processHandler.obtainMessage(ProcessFrame.WHAT_RAW_CONTENT,matrix.getRaw()));
    }
}
