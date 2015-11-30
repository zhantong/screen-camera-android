package com.nju.cs.screencamera;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhantong on 15/11/15.
 */
public class ImgToFile extends FileToImg{
    public void imgsToFile(BlockingDeque<Bitmap> bitmaps,File file){
        long TIMEOUT=3000L;
        int count=0;
        int lastSuccessIndex=0;
        int index=0;
        List<byte[]> buf=new ArrayList<>();
        while (true){
            count++;
            Bitmap img;
            try {
                img = bitmaps.poll(TIMEOUT, TimeUnit.MILLISECONDS);
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
                stream = imgToBinaryStream(img);
                if(index-lastSuccessIndex!=1){
                    Log.e("frame "+index+"/" + count, "error lost frame!");
                    break;
                }
                lastSuccessIndex=index;
                buf.add(stream);
                Log.i("frame "+index+"/" + count, "done!");
                img.recycle();
                img = null;
            }
            catch (Exception e){
                Log.i("frame "+index+"/" + count, "code image not found!");
            }
        }
        Log.d("imgsToFile", "total length:" + buf.size());
        binaryStreamToFile(buf, file);
    }

    public int getIndex(Bitmap img) throws NotFoundException{
        BiMatrix biMatrix=Binarizer.convertAndGetThreshold(img);
        int[] border=FindBoarder.findBoarder(biMatrix);
        int imgWidth=(frameBlackLength+frameVaryLength)*2+contentLength;
        GridSampler gs=new GridSampler();
        String row=gs.sampleRow(biMatrix, imgWidth, imgWidth, 0, 0, imgWidth, 0, imgWidth, imgWidth, 0, imgWidth, border[0], border[1], border[2], border[3], border[4], border[5], border[6], border[7], frameBlackLength);
        return GrayCode.toInt(row.substring(frameBlackLength, frameBlackLength + grayCodeLength));
    }
    public byte[] imgToBinaryStream(Bitmap img) throws NotFoundException{
        BiMatrix biMatrix=Binarizer.convertAndGetThreshold(img);
        int[] border=FindBoarder.findBoarder(biMatrix);
        int imgWidth=(frameBlackLength+frameVaryLength)*2+contentLength;
        GridSampler gs=new GridSampler();
        Matrix matrixStream=gs.sampleGrid(biMatrix,imgWidth,imgWidth,0,0,imgWidth,0,imgWidth,imgWidth,0,imgWidth,border[0],border[1],border[2],border[3],border[4],border[5],border[6],border[7]);
        return matrixToBinaryStream(matrixStream);
    }
    public byte[] matrixToBinaryStream(Matrix biMatrix) throws NotFoundException{
        int startOffset=frameBlackLength+frameVaryLength;
        int stopOffset=startOffset+contentLength;
        int contentByteNum=contentLength*contentLength/8;
        int realByteNum=contentByteNum-ecByteNum;
        int[] result=new int[contentByteNum];
        biMatrix.toArray(startOffset,startOffset,stopOffset,stopOffset,result);
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
    public void binaryStreamToFile(List<byte[]> binaryStream,File file){
        int stopIndex=0;
        byte[] oldLast=binaryStream.get(binaryStream.size()-1);
        binaryStream.remove(binaryStream.size()-1);
        for(int i=oldLast.length-1;i>0;i--){
            if(oldLast[i]==-128){
                stopIndex=i;
                break;
            }
        }
        byte[] newLast=new byte[stopIndex];
        System.arraycopy(oldLast,0,newLast,0,stopIndex);
        binaryStream.add(newLast);
        OutputStream os;
        try {
            os = new FileOutputStream(file);
            for(byte[] b:binaryStream){
                os.write(b);
            }
            os.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
