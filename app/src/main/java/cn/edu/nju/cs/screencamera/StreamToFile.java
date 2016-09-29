package cn.edu.nju.cs.screencamera;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import net.fec.openrq.parameters.FECParameters;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhantong on 16/2/29.
 */
public class StreamToFile extends MediaToFile implements ProcessFrame.FrameCallback{
    private static final String TAG = "StreamToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log
    private static final long QUEUE_WAIT_SECONDS=4;
    private static BarcodeFormat barcodeFormat;
    private Handler processHandler;
    public StreamToFile(Handler handler,BarcodeFormat format,String truthFilePath) {
        super(handler);
        barcodeFormat=format;
        ProcessFrame processFrame=new ProcessFrame("process");
        processFrame.start();
        processHandler=new Handler(processFrame.getLooper(), processFrame);
        processHandler.sendMessage(processHandler.obtainMessage(ProcessFrame.WHAT_BARCODE_FORMAT,format));
        if(!truthFilePath.equals("")) {
            processHandler.sendMessage(processHandler.obtainMessage(ProcessFrame.WHAT_TRUTH_FILE_PATH,truthFilePath));
        }
        processFrame.setCallback(this);
    }
    public int getImgColorType(){
        return -1;
    }
    public void notFound(int fileByteNum){}
    public void crcCheckFailed(){}
    public void beforeDataDecoded(){}
    protected void streamToFile(LinkedBlockingQueue<byte[]> imgs,int frameWidth,int frameHeight,String fileName) {
        processHandler.sendMessage(processHandler.obtainMessage(ProcessFrame.WHAT_FILE_NAME,fileName));
        final int NUMBER_OF_SOURCE_BLOCKS=1;
        int fileByteNum=-1;
        int frameCount = -1;
        int lastSuccessIndex = 0;
        int frameAmount = 0;
        byte[] img = {};
        Matrix matrix;
        int[] borders=null;
        int imgColorType=getImgColorType();
        while (true) {
            frameCount++;
            updateInfo("正在识别...");
            try {
                if(VERBOSE){Log.d(TAG,"is queue empty:"+imgs.isEmpty());}
                img = imgs.poll(QUEUE_WAIT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e.getMessage());
            }
            if(img==null){
                updateInfo("识别失败！");
                break;
            }
            updateDebug(lastSuccessIndex, frameAmount, frameCount);
            Log.i(TAG,"processing frame: "+frameCount);
            try {
                matrix=MatrixFactory.createMatrix(barcodeFormat,img,imgColorType, frameWidth, frameHeight,borders);
                matrix.perspectiveTransform();
            } catch (NotFoundException e) {
                Log.d(TAG, e.getMessage());
                notFound(fileByteNum);
                borders=null;
                continue;
            }
            if(fileByteNum==-1){
                try {
                    fileByteNum = getFileByteNum(matrix);
                }catch (CRCCheckException e){
                    Log.d(TAG, "head CRC check failed");
                    crcCheckFailed();
                    continue;
                }
                if(fileByteNum==0){
                    Log.d(TAG,"wrong file byte number");
                    fileByteNum=-1;
                    continue;
                }
                Log.i(TAG,"file is "+fileByteNum+" bytes");
                int length=matrix.realContentByteLength();
                FECParameters parameters = FECParameters.newParameters(fileByteNum, length, NUMBER_OF_SOURCE_BLOCKS);
                Log.i(TAG,"FEC parameters: "+parameters.toString());
                processHandler.sendMessage(processHandler.obtainMessage(ProcessFrame.WHAT_FEC_PARAMETERS,parameters));
            }
            matrix.sampleContent();
            RawContent rawContent=matrix.getRaw();
            rawContent.frameIndex=frameCount;
            processHandler.sendMessage(processHandler.obtainMessage(ProcessFrame.WHAT_RAW_CONTENT,rawContent));
            borders=smallBorder(matrix.borders);
        }
    }
    @Override
    public void onLastPacket() {
        beforeDataDecoded();
    }
}