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
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhantong on 15/11/15.
 */
public class ImgToFile extends FileToImg{
    private TextView textView;
    private Handler handler;
    private CameraPreview mPreview;
    int imgWidth=(frameBlackLength+frameVaryLength)*2+contentLength;
    public ImgToFile(CameraPreview mPreview,TextView textView,Handler handler){
        this.textView=textView;
        this.handler=handler;
        this.mPreview=mPreview;
    }
    private void update(String info){
        final String text=info;
        handler.post(new Runnable() {
            @Override
            public void run() {
                textView.setText(text);
            }
        });
    }
    public void imgsToFile(BlockingDeque<byte[]> bitmaps,File file){
        long TIMEOUT=3000L;
        int count=0;
        int lastSuccessIndex=0;
        int index=0;
        int frameAmount=0;
        List<byte[]> buffer=new LinkedList<>();
        byte[] img={};
        while (true){
            count++;

            try {
                try {
                    img = bitmaps.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                }
                catch (InterruptedException e){
                    e.printStackTrace();
                }
                if (img == null) {
                    break;
                }
                BiMatrix biMatrix=new BiMatrix(img,CameraSettings.previewWidth,CameraSettings.previeHeight);
                biMatrix.perspectiveTransform(0, 0, imgWidth, 0, imgWidth, imgWidth, 0, imgWidth);
                //Log.i("get picture", "caught...");
                update("frame "+index+"/" + count+"same frame index!");
                index=getIndex(biMatrix);

                Log.i("frame "+index+"/" + count, "processing...");
                if(lastSuccessIndex==index){
                    Log.i("frame "+index+"/" + count, "same frame index!");
                    continue;
                }

                byte[] stream;
                stream = imgToArray(biMatrix);
                if(index-lastSuccessIndex!=1){
                    //Log.e("frame "+index+"/" + count, "error lost frame!");
                    Log.i("frame "+index+"/" + count, "bad frame index!");
                    //break;
                    continue;
                }
                buffer.add(stream);
                Log.i("frame "+index+"/" + count, "done!");
                if(frameAmount==0){
                    frameAmount=getFrameAmount(biMatrix);
                    System.out.println("frameAmount:"+frameAmount);
                }
                lastSuccessIndex=index;
                if(lastSuccessIndex==frameAmount){
                    mPreview.stop();
                    break;
                }
                biMatrix=null;
            }
            catch (NotFoundException e){
                Log.i("frame "+index+"/" + count, "code image not found!");
            }
        }
        Log.d("imgsToFile", "total length:" + buffer.size());
        bufferToFile(buffer, file);
    }

    public int getIndex(BiMatrix biMatrix) throws NotFoundException{

        String row=biMatrix.sampleRow(imgWidth, imgWidth, frameBlackLength);
        System.out.println(row);
        int index=Integer.parseInt(row.substring(frameBlackLength, frameBlackLength + 16),2);
        System.out.println("index:" + index);
        int crc=Integer.parseInt(row.substring(frameBlackLength + 16, frameBlackLength + 24), 2);
        System.out.println(index+" "+crc+" "+CRC8.calcCrc8(index));
        if(crc!=CRC8.calcCrc8(index)){
            throw  NotFoundException.getNotFoundInstance();
        }
        return index;
    }
    public int getFrameAmount(BiMatrix biMatrix) throws NotFoundException{
        String row=biMatrix.sampleRow(imgWidth, imgWidth, frameBlackLength);
        int frameAmount=Integer.parseInt(row.substring(frameBlackLength+24, frameBlackLength + 40),2);
        int crc=Integer.parseInt(row.substring(frameBlackLength + 40, frameBlackLength + 48), 2);
        if(crc!=CRC8.calcCrc8(frameAmount)){
            throw  NotFoundException.getNotFoundInstance();
        }
        return frameAmount;
    }
    public byte[] imgToArray(BiMatrix biMatrix) throws NotFoundException{
        Matrix matrixStream=biMatrix.sampleGrid(imgWidth,imgWidth);
        return matrixToArray(matrixStream);
    }
    public byte[] matrixToArray(Matrix biMatrix) throws NotFoundException{
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
            throw  NotFoundException.getNotFoundInstance();
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
            e.printStackTrace();
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
