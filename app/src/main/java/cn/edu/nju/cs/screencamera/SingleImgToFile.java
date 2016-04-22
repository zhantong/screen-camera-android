package cn.edu.nju.cs.screencamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.decoder.SourceBlockDecoder;
import net.fec.openrq.parameters.FECParameters;

import java.nio.ByteBuffer;

/**
 * 识别图片中的二维码,测试用
 */
public class SingleImgToFile extends MediaToFile {
    private static final String TAG = "SingleImgToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log

    /**
     * 构造函数,获取必须的参数
     *
     * @param debugView 实例
     * @param infoView  实例
     * @param handler   实例
     */
    public SingleImgToFile(TextView debugView, TextView infoView, Handler handler) {
        super(debugView, infoView, handler);
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
        Matrix matrix=null;
        ArrayDataDecoder dataDecoder=null;
        int fileByteNum=-1;
        int[] border=null;
        try {
            if(barcodeType==0){
                matrix=new MatrixNormal(byteBuffer.array(),0, bitmap.getWidth(), bitmap.getHeight(),border);
            }
            else if(barcodeType==1){
                matrix=new MatrixZoom(byteBuffer.array(),0, bitmap.getWidth(), bitmap.getHeight(),border);
            }
            else{
                return;
            }
            matrix.perspectiveTransform(0, 0, barCodeWidth, 0, barCodeWidth, barCodeHeight, 0, barCodeHeight);
        } catch (NotFoundException e) {
            Log.d(TAG, e.getMessage());
            return;
        }
        for(int i=0;i<2;i++) {
            matrix.reverse=!matrix.reverse;
            if(fileByteNum==-1){
                try {
                    fileByteNum = getFileByteNum(matrix);
                }catch (CRCCheckException e){
                    Log.d(TAG,"head CRC check failed");
                    continue;
                }
                if(fileByteNum==0){
                    fileByteNum=-1;
                    continue;
                }
                int length=bitsPerBlock*contentLength*contentLength/8-ecNum*ecLength/8-8;
                FECParameters parameters = FECParameters.newParameters(fileByteNum, length, 1);
                Log.d(TAG, "RaptorQ parameters:" + parameters.toString());
                dataDecoder = OpenRQ.newDecoder(parameters, 0);
            }
            byte[] current;
            try {
                current = getContent(matrix);
            }catch (ReedSolomonException e){
                Log.d(TAG, "content error correction failed");
                continue;
            }
            EncodingPacket encodingPacket = dataDecoder.parsePacket(current, true).value();
            Log.i(TAG, "got 1 source block: source block number:" + encodingPacket.sourceBlockNumber() + "\tencoding symbol ID:" + encodingPacket.encodingSymbolID() + "\t" + encodingPacket.symbolType());
            dataDecoder.sourceBlock(encodingPacket.sourceBlockNumber()).putEncodingPacket(encodingPacket);
        }
        if(fileByteNum!=-1) {
            checkSourceBlockStatus(dataDecoder);
            Log.d(TAG, "is file decoded: " + dataDecoder.isDataDecoded());
        }
        matrix = null;
    }
    private void checkSourceBlockStatus(ArrayDataDecoder dataDecoder){
        Log.i(TAG,"check source block status:");
        for (SourceBlockDecoder sourceBlockDecoder : dataDecoder.sourceBlockIterable()) {
            Log.i(TAG,"source block number:" + sourceBlockDecoder.sourceBlockNumber() + "\tstate:" + sourceBlockDecoder.latestState());
        }
    }
}
