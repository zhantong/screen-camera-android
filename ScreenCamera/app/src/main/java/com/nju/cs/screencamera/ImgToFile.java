package com.nju.cs.screencamera;

import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhantong on 15/11/15.
 */
public class ImgToFile extends FileToImg{
    private static final String TAG = "ImgToFile";
    private static final boolean VERBOSE = false;
    private TextView debugView;
    private TextView infoView;
    private Handler handler;
    private CameraPreview mPreview;
    int imgWidth=(frameBlackLength+frameVaryLength)*2+contentLength;
    public ImgToFile(CameraPreview mPreview,TextView debugView,TextView infoView,Handler handler){
        this.debugView=debugView;
        this.infoView=infoView;
        this.handler=handler;
        this.mPreview=mPreview;
    }
    private void updateDebug(int index,int lastSuccessIndex,int frameAmount,int count){
        final String text="当前:"+index+"已识别:"+lastSuccessIndex+"帧总数:"+frameAmount+"已处理:"+count;
        handler.post(new Runnable() {
            @Override
            public void run() {
                debugView.setText(text);
            }
        });
    }
    private void updateInfo(String msg){
        final String text=msg;
        handler.post(new Runnable() {
            @Override
            public void run() {
                infoView.setText(text);
            }
        });
    }
    public void imgsToFile(LinkedBlockingQueue<byte[]> bitmaps,File file){
        long TIMEOUT=3000L;
        int count=0;
        int lastSuccessIndex=0;
        int index=0;
        int frameAmount=0;
        List<byte[]> buffer=new LinkedList<>();
        byte[] img={};
        BiMatrix biMatrix=new BiMatrix(1);
        byte[] stream={};
        /*
        handler.post(new Runnable() {
            @Override
            public void run() {
                mPreview.startPreview();
            }
        });
        */
        while (true){
            count++;

            updateInfo("正在识别...");
            try {
                img = bitmaps.take();
            }catch (InterruptedException e){
                Log.d(TAG, e.getMessage());
            }
            updateDebug(index, lastSuccessIndex, frameAmount, count);
            try {
            biMatrix=new BiMatrix(img,CameraSettings.previewWidth,CameraSettings.previeHeight);
            biMatrix.perspectiveTransform(0, 0, imgWidth, 0, imgWidth, imgWidth, 0, imgWidth);
            //Log.i("get picture", "caught...");
            index = getIndex(biMatrix);
            }catch (NotFoundException e){
                if(lastSuccessIndex==0) {
                    mPreview.focus();
                }
                Log.d(TAG, e.getMessage());
            }catch (CRCCheckException e){
                Log.d(TAG, "CRC check failed");
            }
            Log.i("frame "+index+"/" + count, "processing...");
            if(lastSuccessIndex==index){
                Log.i("frame "+index+"/" + count, "same frame index!");
                continue;
            }


            try {
                stream = imgToArray(biMatrix);
            }catch (ReedSolomonException e){
                Log.d(TAG, e.getMessage());
            }
            if(index-lastSuccessIndex!=1){
                //Log.e("frame "+index+"/" + count, "error lost frame!");
                Log.i("frame " + index + "/" + count, "bad frame index!");
                //break;
                continue;
            }
            buffer.add(stream);
            Log.i("frame " + index + "/" + count, "done!");
            if(frameAmount==0){
                try {
                    frameAmount = getFrameAmount(biMatrix);
                }catch (CRCCheckException e){
                    Log.d(TAG, "CRC check failed");
                }
            }
            lastSuccessIndex = index;
            updateDebug(index, lastSuccessIndex, frameAmount, count);
            if(lastSuccessIndex==frameAmount){
                mPreview.stop();
                break;
            }
            biMatrix=null;

        }
        updateInfo("识别完成!正在写入文件");
        Log.d("imgsToFile", "total length:" + buffer.size());
        bufferToFile(buffer, file);
        updateInfo("写入文件成功!");
    }

    public int getIndex(BiMatrix biMatrix) throws CRCCheckException{

        String row=biMatrix.sampleRow(imgWidth, imgWidth, frameBlackLength);
        int index=Integer.parseInt(row.substring(frameBlackLength, frameBlackLength + 16),2);
        int crc=Integer.parseInt(row.substring(frameBlackLength + 16, frameBlackLength + 24), 2);
        if(crc!=CRC8.calcCrc8(index)){
            throw  CRCCheckException.getNotFoundInstance();
        }
        return index;
    }
    public int getFrameAmount(BiMatrix biMatrix) throws CRCCheckException{
        String row=biMatrix.sampleRow(imgWidth, imgWidth, frameBlackLength);
        int frameAmount=Integer.parseInt(row.substring(frameBlackLength+24, frameBlackLength + 40),2);
        int crc=Integer.parseInt(row.substring(frameBlackLength + 40, frameBlackLength + 48), 2);
        if(crc!=CRC8.calcCrc8(frameAmount)){
            throw  CRCCheckException.getNotFoundInstance();
        }
        return frameAmount;
    }
    public byte[] imgToArray(BiMatrix biMatrix) throws ReedSolomonException{
        Matrix matrixStream=biMatrix.sampleGrid(imgWidth,imgWidth);
        return matrixToArray(matrixStream);
    }
    public byte[] matrixToArray(Matrix biMatrix) throws ReedSolomonException{
        int startOffset=frameBlackLength+frameVaryLength;
        int stopOffset=startOffset+contentLength;
        int contentByteNum=contentLength*contentLength/8;
        int realByteNum=contentByteNum-ecByteNum;
        int[] result=new int[contentByteNum];
        biMatrix.toArray(startOffset, startOffset, stopOffset, stopOffset, result);
        ReedSolomonDecoder decoder=new ReedSolomonDecoder(GenericGF.QR_CODE_FIELD_256);
        try{
            decoder.decode(result,ecByteNum);
        }catch (Exception e){
            throw new ReedSolomonException("error correcting failed");
        }
        byte[] res=new byte[realByteNum];
        for(int i=0;i<realByteNum;i++){
            res[i]=(byte)result[i];
        }
        return res;
    }
    public void bufferToFile(List<byte[]> buffer,File file){
        byte[] oldLast=buffer.get(buffer.size()-1);
        buffer.remove(buffer.size() - 1);
        buffer.add(cutArrayBack(oldLast,-128));
        OutputStream os;
        try {
            os = new FileOutputStream(file);
            for(byte[] b:buffer){
                os.write(b);
            }
            os.close();
        }catch (Exception e){
            Log.d(TAG, e.getMessage());
        }
    }
    private byte[] cutArrayBack(byte[] old,int intCut){
        byte byteCut=(byte)intCut;
        int stopIndex=0;
        for(int i=old.length-1;i>0;i--){
            if(old[i]==byteCut){
                stopIndex=i;
                break;
            }
        }
        byte[] array=new byte[stopIndex];
        System.arraycopy(old,0,array,0,stopIndex);
        return array;
    }
}
