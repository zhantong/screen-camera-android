package com.nju.cs.screencamera;

import android.graphics.Bitmap;
import android.util.Log;

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
    public void imgsToFile(BlockingDeque<byte[]> bitmaps,File file){
        long TIMEOUT=3000L;
        int count=0;
        int lastSuccessIndex=0;
        int index=0;
        List<byte[]> buffer=new LinkedList<>();
        while (true){
            count++;
            Bitmap img;
            try {
                byte[] temp = bitmaps.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                img=YUVtoBitmap.convert(temp);
                if (img == null) {
                    break;
                }
                index=getIndex(img);
                Log.i("frame "+index+"/" + count, "processing...");
                if(lastSuccessIndex==index){
                    Log.i("frame "+index+"/" + count, "same frame index!");
                    continue;
                }
                byte[] stream;
                stream = imgToArray(img);
                if(index-lastSuccessIndex!=1){
                    //Log.e("frame "+index+"/" + count, "error lost frame!");
                    //break;
                    continue;
                }
                lastSuccessIndex=index;
                buffer.add(stream);
                Log.i("frame "+index+"/" + count, "done!");
                img.recycle();
                img = null;
            }
            catch (Exception e){
                Log.i("frame "+index+"/" + count, "code image not found!");
            }
        }
        Log.d("imgsToFile", "total length:" + buffer.size());
        bufferToFile(buffer, file);
    }

    public int getIndex(Bitmap img) throws NotFoundException{
        BiMatrix biMatrix=Binarizer.convertAndGetThreshold(img);
        int[] border=FindBoarder.findBoarder(biMatrix);
        int imgWidth=(frameBlackLength+frameVaryLength)*2+contentLength;
        GridSampler gs=new GridSampler();
        String row=gs.sampleRow(biMatrix, imgWidth, imgWidth, 0, 0, imgWidth, 0, imgWidth, imgWidth, 0, imgWidth, border[0], border[1], border[2], border[3], border[4], border[5], border[6], border[7], frameBlackLength);
        return GrayCode.toInt(row.substring(frameBlackLength, frameBlackLength + grayCodeLength));
    }
    public byte[] imgToArray(Bitmap img) throws NotFoundException{
        BiMatrix biMatrix=Binarizer.convertAndGetThreshold(img);
        int[] border=FindBoarder.findBoarder(biMatrix);
        int imgWidth=(frameBlackLength+frameVaryLength)*2+contentLength;
        GridSampler gs=new GridSampler();
        Matrix matrixStream=gs.sampleGrid(biMatrix, imgWidth, imgWidth, 0, 0, imgWidth, 0, imgWidth, imgWidth, 0, imgWidth, border[0], border[1], border[2], border[3], border[4], border[5], border[6], border[7]);
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
