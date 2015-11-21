package com.nju.cs.screencamera;

import android.graphics.Bitmap;
import android.util.Log;

/*
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
*/
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhantong on 15/11/15.
 */
public class ImgToFile extends FileToImg{
    /*
    public static void main(String[] args){
        File outFile=new File("/Users/zhantong/Desktop/t.txt");
        ImgToFile imgToFile=new ImgToFile();
        imgToFile.imgsToFile("/Users/zhantong/Desktop/test1",outFile);
    }
    */
    /*
    public void imgsToFile(String imgsPath,File file){
        File root=new File(imgsPath);
        File[] imgs=root.listFiles();
        int[] buffer={};
        int[] last={};
        for(File img:imgs){
            int[] t;
            try {
                t = imgToBinaryStream(img);
            }catch (NotFoundException e){
                System.out.println("Code image not found!");
                continue;
            }
            if(t==last){
                continue;
            }
            last=t;
            int[] temp=new int[buffer.length+t.length];
            System.arraycopy(buffer,0,temp,0,buffer.length);
            System.arraycopy(t,0,temp,buffer.length,t.length);
            buffer=temp;
            System.out.println("DONE!");
        }
        binaryStreamToFile(buffer,file);
    }
    */
    public void imgsToFile(BlockingDeque<Bitmap> bitmaps,File file){
        long TIMEOUT=3000L;
        int[] buffer={};
        int[] last={};
        int[] xx={};
        //int[] last=new int[2500];
        int count=0;
        TEST:
        while (true){
            int c=0;
            count++;
            Bitmap img;
            try {
                img = bitmaps.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                if (img == null) {
                    break TEST;
                }
                int[] t;
                t = imgToBinaryStream(img);

                if (Arrays.equals(t, last)) {
                    Log.i("Img " + Integer.toString(count), "Same image!");
                    //System.out.println("Same image!");
                    continue TEST;
                }
                c = 0;
                if (!Arrays.equals(last, xx)) {
                    for (int i = 0; i < t.length; i++) {
                        if (t[i] != last[i]) {
                            c++;
                        }
                    }
                    Log.i("Img " + Integer.toString(count), "The difference is "+c);
                    if (c < 40) {
                        continue TEST;
                    }
                }
                last = t;
                int[] temp = new int[buffer.length + t.length];
                System.arraycopy(buffer, 0, temp, 0, buffer.length);
                System.arraycopy(t, 0, temp, buffer.length, t.length);
                buffer = temp;
                Log.i("Img " + Integer.toString(count), "DONE!");
                //System.out.println("DONE!");

                img.recycle();
                img = null;
            }
            catch (Exception e){
                Log.i("Img "+Integer.toString(count),"Code image not found!");
                //System.out.println("Code image not found!");
                continue TEST;
            }
        }
        binaryStreamToFile(buffer, file);
    }
    /*
    public int[] imgToBinaryStream(File file) throws NotFoundException{
        BufferedImage img=null;
        try {
            img = ImageIO.read(file);
        }catch (Exception e){
            e.printStackTrace();
        }
        int[][] biMatrix=Binarizer.binarizer(img);
        int[] border=FindBoarder.findBoarder(biMatrix);
        int imgWidth=(frameBlackLength+frameVaryLength)*2+contentLength;
        GridSampler gs=new GridSampler();
        int[][] matrixStream=gs.sampleGrid(biMatrix,imgWidth,imgWidth,0,0,imgWidth,0,imgWidth,imgWidth,0,imgWidth,border[0],border[1],border[2],border[3],border[4],border[5],border[6],border[7]);
        return matrixToBinaryStream(matrixStream);
    }
    */
    public int[] imgToBinaryStream(Bitmap img) throws NotFoundException{
        BiMatrix biMatrix=Binarizer.binarizer(img);
        int[] border=FindBoarder.findBoarder(biMatrix);
        int imgWidth=(frameBlackLength+frameVaryLength)*2+contentLength;
        GridSampler gs=new GridSampler();
        BiMatrix matrixStream=gs.sampleGrid(biMatrix,imgWidth,imgWidth,0,0,imgWidth,0,imgWidth,imgWidth,0,imgWidth,border[0],border[1],border[2],border[3],border[4],border[5],border[6],border[7]);
        return matrixToBinaryStream(matrixStream);
    }
    public int[] matrixToBinaryStream(BiMatrix biMatrix) throws NotFoundException{
        int startOffset=frameBlackLength+frameVaryLength;
        int stopOffset=startOffset+contentLength;
        System.out.println(startOffset+" "+stopOffset);
        int[] result=new int[contentLength*contentLength];
        int[] verify={0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1};
        int index=0;
        for(int j=frameBlackLength;j<frameBlackLength+contentLength;j++){
            if(!biMatrix.pixelEquals(frameBlackLength+frameVaryLength+contentLength,j,verify[index++])){
                throw NotFoundException.getNotFoundInstance();
            }
            //System.out.print(Integer.toString(biMatrix[frameBlackLength+frameVaryLength+contentLength][j]));
        }
        index=0;
        System.out.println();
        for(int j=startOffset;j<stopOffset;j++){
            for(int i=startOffset;i<stopOffset;i++){
                result[index++]=biMatrix.get(i,j);
            }
        }
        return result;
    }
    public void binaryStreamToFile(int[] binaryStream,File file){
        int stopIndex=0;
        for(int i=binaryStream.length-1;i>0;i--){
            if(binaryStream[i]==0){
                stopIndex=i;
                break;
            }
        }
        byte[] target=new byte[stopIndex+1/8];
        System.out.println(Integer.toString(stopIndex));
        for(int i=0;i<stopIndex;i++){
            if(binaryStream[i]==1) {
                switch (i%8){
                    case 0:
                        target[i/8]= (byte) ((int)target[i/8] | 0x80);
                        break;
                    case 1:
                        target[i/8]= (byte) ((int)target[i/8] | 0x40);
                        break;
                    case 2:
                        target[i/8]= (byte) ((int)target[i/8] | 0x20);
                        break;
                    case 3:
                        target[i/8]= (byte) ((int)target[i/8] | 0x10);
                        break;
                    case 4:
                        target[i/8]= (byte) ((int)target[i/8] | 0x8);
                        break;
                    case 5:
                        target[i/8]= (byte) ((int)target[i/8] | 0x4);
                        break;
                    case 6:
                        target[i/8]= (byte) ((int)target[i/8] | 0x2);
                        break;
                    case 7:
                        target[i/8]= (byte) ((int)target[i/8] | 0x1);
                }
            }
        }
        OutputStream os;
        try{
            os=new FileOutputStream(file);
            os.write(target);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
