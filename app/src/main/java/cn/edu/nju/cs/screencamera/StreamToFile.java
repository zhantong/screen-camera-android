package cn.edu.nju.cs.screencamera;

import android.os.Handler;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.fec.openrq.parameters.FECParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import cn.edu.nju.cs.screencamera.Logback.CustomMarker;

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

        Logger LOG= LoggerFactory.getLogger(MainActivity.class);

        matrix=MatrixFactory.createMatrix(barcodeFormat);
        LOG.info(CustomMarker.barcodeMeta,matrix.getMetaInString());

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
                updateInfo("识别成功！");
                break;
            }
            updateDebug(lastSuccessIndex, frameAmount, frameCount);
            Log.i(TAG,"processing frame: "+frameCount);
            try {
                matrix=MatrixFactory.createMatrix(barcodeFormat,img,imgColorType, frameWidth, frameHeight,borders);
                matrix.perspectiveTransform();
                matrix.frameIndex=frameCount;
            } catch (NotFoundException e) {
                Log.d(TAG, e.getMessage());
                notFound(fileByteNum);
                borders=null;
                continue;
            }

            matrix.sampleContent();
            LOG.info(CustomMarker.raw,matrix.getSampleDataInString());

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
                ObjectMapper mapper=new ObjectMapper();
                JsonNode root=mapper.createObjectNode();
                ((ObjectNode)root).set("dataLength", IntNode.valueOf(parameters.dataLengthAsInt()));
                ((ObjectNode)root).set("numberOfSourceBlocks", IntNode.valueOf(parameters.numberOfSourceBlocks()));
                ((ObjectNode)root).set("symbolSize", IntNode.valueOf(parameters.symbolSize()));
                ((ObjectNode)root).set("totalSymbols", IntNode.valueOf(parameters.totalSymbols()));
                LOG.info(CustomMarker.raptorQMeta,root.toString());
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