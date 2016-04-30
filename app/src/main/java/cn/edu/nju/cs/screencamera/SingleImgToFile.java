package cn.edu.nju.cs.screencamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;
import android.widget.Spinner;
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
    private static BarcodeFormat barcodeFormat;

    /**
     * 构造函数,获取必须的参数
     *
     * @param debugView 实例
     * @param infoView  实例
     * @param handler   实例
     */
    public SingleImgToFile(TextView debugView, TextView infoView, Handler handler,BarcodeFormat format,String truthFilePath) {
        super(debugView, infoView, handler);
        barcodeFormat=format;
        if(!truthFilePath.equals("")) {
            setDebug(MatrixFactory.createMatrix(format), truthFilePath);
        }
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
        long startTime;
        long endTime;
        startTime= System.currentTimeMillis();
        try {
            matrix=MatrixFactory.createMatrix(barcodeFormat,byteBuffer.array(),0, bitmap.getWidth(), bitmap.getHeight(),border);
            matrix.perspectiveTransform();
        } catch (NotFoundException e) {
            Log.d(TAG, e.getMessage());
        }
        for(int i=1;i<3;i++) {
            if(i==2){
                if(!matrix.isMixed){
                    break;
                }
                matrix.reverse=true;
            }
            Log.i(TAG,"try "+i+" :");
            if(fileByteNum==-1){
                try {
                    fileByteNum = getFileByteNum(matrix);
                }catch (CRCCheckException e){
                    Log.d(TAG, "head CRC check failed");
                    continue;
                }
                if(fileByteNum==0){
                    Log.d(TAG,"wrong file byte number");
                    fileByteNum=-1;
                    continue;
                }
                Log.i(TAG,"file is "+fileByteNum+" bytes");
                int length=matrix.realContentByteLength();
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
        matrix = null;
        endTime= System.currentTimeMillis();
        Log.d(TAG,"process frame takes "+(endTime-startTime)+"ms");
    }
}
