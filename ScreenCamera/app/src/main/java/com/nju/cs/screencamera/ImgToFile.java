package com.nju.cs.screencamera;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
 * Created by zhantong on 15/11/15.
 */
public class ImgToFile extends FileToImg{
    private static final String TAG = "ImgToFile";
    private static final boolean VERBOSE = false;
    private TextView debugView;
    private TextView infoView;
    private Handler handler;
    private CameraPreview mPreview;
    private final int imgWidth;
    private final int imgHeight;
    int barCodeWidth =(frameBlackLength+frameVaryLength)*2+contentLength;
    public ImgToFile(TextView debugView,TextView infoView,Handler handler,int imgWidth,int imgHeight,CameraPreview mPreview){
        this.debugView=debugView;
        this.infoView=infoView;
        this.handler=handler;
        this.mPreview=mPreview;
        this.imgWidth=imgWidth;
        this.imgHeight=imgHeight;
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
    public void cameraToFile(LinkedBlockingQueue<byte[]> imgs, File file){
        int count=0;
        int lastSuccessIndex=0;
        int frameAmount=0;
        List<byte[]> buffer=new LinkedList<>();
        byte[] img={};
        Matrix matrix;
        byte[] stream;
        int index=0;
        while (true){
            count++;
            updateInfo("正在识别...");
            try {
                img = imgs.take();
            }catch (InterruptedException e){
                Log.d(TAG, e.getMessage());
            }
            updateDebug(index, lastSuccessIndex, frameAmount, count);
            try {
                matrix =imgToMatrix(img);
            }catch (NotFoundException e){
                if(lastSuccessIndex==0) {
                    mPreview.focus();
                }
                Log.d(TAG, e.getMessage());
                continue;
            }catch (CRCCheckException e){
                Log.d(TAG, "CRC check failed");
                continue;
            }
            index=matrix.frameIndex;
            Log.i("frame "+index+"/" + count, "processing...");
            if(lastSuccessIndex==index){
                Log.i("frame "+index+"/" + count, "same frame index!");
                continue;
            }
            else if(index-lastSuccessIndex!=1){
                Log.i("frame " + index + "/" + count, "bad frame index!");
                continue;
            }
            try {
                stream = imgToArray(matrix);
            }catch (ReedSolomonException e){
                Log.d(TAG, e.getMessage());
                continue;
            }
            buffer.add(stream);
            lastSuccessIndex = index;
            Log.i("frame " + index + "/" + count, "done!");
            updateDebug(index, lastSuccessIndex, frameAmount, count);
            if(lastSuccessIndex==frameAmount){
                mPreview.stop();
                break;
            }
            if(frameAmount==0){
                try {
                    frameAmount = getFrameAmount(matrix);
                }catch (CRCCheckException e){
                    Log.d(TAG, "CRC check failed");
                    continue;
                }
            }
            matrix =null;
        }
        updateInfo("识别完成!正在写入文件");
        Log.d("cameraToFile", "total length:" + buffer.size());
        bufferToFile(buffer, file);
        updateInfo("写入文件成功!");
    }
    public void videoToFile(LinkedBlockingQueue<byte[]> imgs,File file){
        int count=0;
        int lastSuccessIndex=0;
        int frameAmount=0;
        List<byte[]> buffer=new LinkedList<>();
        byte[] img={};
        RGBMatrix rgbMatrix;
        byte[] stream;
        int index=0;
        while (true){
            count++;
            updateInfo("正在识别...");
            try {
                img = imgs.take();
            }catch (InterruptedException e){
                Log.d(TAG, e.getMessage());
            }
            updateDebug(index, lastSuccessIndex, frameAmount, count);
            try {
                rgbMatrix =new RGBMatrix(img,imgWidth,imgHeight);
                rgbMatrix.perspectiveTransform(0, 0, barCodeWidth, 0, barCodeWidth, barCodeWidth, 0, barCodeWidth);
                rgbMatrix.frameIndex = getIndex(rgbMatrix);
            }catch (NotFoundException e){
                Log.d(TAG, e.getMessage());
                continue;
            }catch (CRCCheckException e){
                Log.d(TAG, "CRC check failed");
                continue;
            }
            index=rgbMatrix.frameIndex;
            Log.i("frame "+index+"/" + count, "processing...");
            if(lastSuccessIndex==index){
                Log.i("frame "+index+"/" + count, "same frame index!");
                continue;
            }
            else if(index-lastSuccessIndex!=1){
                Log.i("frame " + index + "/" + count, "bad frame index!");
                continue;
            }
            try {
                stream = imgToArray(rgbMatrix);
            }catch (ReedSolomonException e){
                Log.d(TAG, e.getMessage());
                continue;
            }
            buffer.add(stream);
            lastSuccessIndex = index;
            Log.i("frame " + index + "/" + count, "done!");
            updateDebug(index, lastSuccessIndex, frameAmount, count);
            if(lastSuccessIndex==frameAmount){
                break;
            }
            if(frameAmount==0){
                try {
                    frameAmount = getFrameAmount(rgbMatrix);
                }catch (CRCCheckException e){
                    Log.d(TAG, "CRC check failed");
                    continue;
                }
            }
            rgbMatrix =null;
        }
        updateInfo("识别完成!正在写入文件");
        Log.d("videoToFile", "total length:" + buffer.size());
        bufferToFile(buffer, file);
        updateInfo("写入文件成功!");
    }
    public void singleImg(String filePath){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap= BitmapFactory.decodeFile(filePath,options);
        ByteBuffer byteBuffer=ByteBuffer.allocateDirect(bitmap.getWidth()*bitmap.getHeight()*4);
        bitmap.copyPixelsToBuffer(byteBuffer);
        bitmap.recycle();
        updateInfo("正在识别...");
        RGBMatrix rgbMatrix;
        try {
            rgbMatrix = new RGBMatrix(byteBuffer.array(), bitmap.getWidth(), bitmap.getHeight());
            rgbMatrix.perspectiveTransform(0, 0, barCodeWidth, 0, barCodeWidth, barCodeWidth, 0, barCodeWidth);
            rgbMatrix.frameIndex = getIndex(rgbMatrix);
        }catch (NotFoundException e){
            Log.d(TAG, e.getMessage());
            return;
        }catch (CRCCheckException e){
            Log.d(TAG, "CRC check failed");
            return;
        }
        Log.d(TAG,"frame index:"+rgbMatrix.frameIndex);
        int frameAmount;
        try {
            frameAmount = getFrameAmount(rgbMatrix);
        }catch (CRCCheckException e){
            Log.d(TAG, "CRC check failed");
            return;
        }
        Log.d(TAG,"frame amount:"+frameAmount);
        byte[] stream;
        try {
            stream = imgToArray(rgbMatrix);
        }catch (ReedSolomonException e){
            Log.d(TAG, e.getMessage());
            return;
        }
        Log.i(TAG,"done!");
        updateInfo("识别完成!");
    }
    public Matrix imgToMatrix(byte[] img) throws NotFoundException,CRCCheckException{
        Matrix matrix =new Matrix(img,imgWidth,imgHeight);
        matrix.perspectiveTransform(0, 0, barCodeWidth, 0, barCodeWidth, barCodeWidth, 0, barCodeWidth);
        matrix.frameIndex = getIndex(matrix);
        return matrix;
    }
    public int getIndex(Matrix matrix) throws CRCCheckException{
        String row= matrix.sampleRow(barCodeWidth, barCodeWidth, frameBlackLength);
        if(VERBOSE){Log.d(TAG,"index row:"+row);}
        int index=Integer.parseInt(row.substring(frameBlackLength, frameBlackLength + 16), 2);
        int crc=Integer.parseInt(row.substring(frameBlackLength + 16, frameBlackLength + 24), 2);
        int truth=CRC8.calcCrc8(index);
        if(VERBOSE){Log.d(TAG,"CRC check: index:"+index+" CRC:"+crc+" truth:"+truth);}
        if(crc!=truth){
            throw  CRCCheckException.getNotFoundInstance();
        }
        return index;
    }
    public int getFrameAmount(Matrix matrix) throws CRCCheckException{
        String row= matrix.sampleRow(barCodeWidth, barCodeWidth, frameBlackLength);
        int frameAmount=Integer.parseInt(row.substring(frameBlackLength+24, frameBlackLength + 40),2);
        int crc=Integer.parseInt(row.substring(frameBlackLength + 40, frameBlackLength + 48), 2);
        if(crc!=CRC8.calcCrc8(frameAmount)){
            throw  CRCCheckException.getNotFoundInstance();
        }
        return frameAmount;
    }
    public byte[] imgToArray(Matrix matrix) throws ReedSolomonException{
        BinaryMatrix binaryMatrix= matrix.sampleGrid(barCodeWidth, barCodeWidth);
        return binaryMatrixToArray(binaryMatrix);
    }
    public byte[] binaryMatrixToArray(BinaryMatrix binaryMatrix) throws ReedSolomonException{
        int startOffset=frameBlackLength+frameVaryLength;
        int stopOffset=startOffset+contentLength;
        int contentByteNum=contentLength*contentLength/8;
        int realByteNum=contentByteNum-ecByteNum;
        int[] result=new int[contentByteNum];
        binaryMatrix.toArray(startOffset, startOffset, stopOffset, stopOffset, result);
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
