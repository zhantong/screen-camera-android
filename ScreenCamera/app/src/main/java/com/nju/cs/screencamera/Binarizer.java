package com.nju.cs.screencamera;

import android.graphics.Bitmap;

/*
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
*/

/**
 * Created by zhantong on 15/11/15.
 */
public class Binarizer {
    public static int threshold(Bitmap img) throws NotFoundException{
        int height=img.getHeight();
        int width=img.getWidth();
        int[] buckets=new int[256];
        for(int y=1;y<5;y++){
            int row=height*y/5;
            int right=(width*4)/5;
            for(int column=width/5;column<right;column++){
                //int argb=img.getRGB(column,row);
                int argb=img.getPixel(column, row);
                int r=(argb>>16)&0xFF;
                int g=(argb>>8)&0xFF;
                int b=(argb)&0xFF;
                int gray=((b*29+g*150+r*77+128)>>8);
                buckets[gray]++;
            }
        }
        int numBuckets=buckets.length;
        int firstPeak=0;
        int firstPeakSize=0;
        for(int x=0;x<numBuckets;x++){
            if(buckets[x]>firstPeakSize){
                firstPeak=x;
                firstPeakSize=buckets[x];
            }
        }
        int secondPeak=0;
        int secondPeakScore=0;
        for(int x=0;x<numBuckets;x++){
            int distanceToFirstPeak=x-firstPeak;
            int score=buckets[x]*distanceToFirstPeak*distanceToFirstPeak;
            if(score>secondPeakScore){
                secondPeak=x;
                secondPeakScore=score;
            }
        }
        if(firstPeak>secondPeak){
            int temp=firstPeak;
            firstPeak=secondPeak;
            secondPeak=temp;
        }
        if(secondPeak-firstPeak<=numBuckets/16){
            throw NotFoundException.getNotFoundInstance();
        }
        int bestValley=0;
        int bestValleyScore=-1;
        for(int x=firstPeak+1;x<secondPeak;x++){
            int fromSecond=secondPeak-x;
            int score=(x-firstPeak)*fromSecond*fromSecond*(firstPeakSize-buckets[x]);
            //int score=fromSecond*fromSecond*(firstPeakSize-buckets[x]);
            if(score>bestValleyScore){
                bestValley=x;
                bestValleyScore=score;
            }
        }
        return bestValley;
    }
    public static BiMatrix convertAndGetThreshold(Bitmap img) throws NotFoundException{
        int threshold=threshold(img);
        int height=img.getHeight();
        int width=img.getWidth();

        int[] argbs=new int[width*height];
        img.getPixels(argbs,0,width,0,0,width,height);
        BiMatrix biMatrix=new BiMatrix(argbs,width,height);
        biMatrix.setThreshold(threshold);
        return biMatrix;
    }
}
