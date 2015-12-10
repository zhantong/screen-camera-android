package com.nju.cs.screencamera;

import android.util.Log;


/**
 * Created by zhantong on 15/11/15.
 */
public class Binarizer {
    private static final boolean VERBOSE = false;
    private static final String TAG = "Binarizer";
    public static int threshold(BiMatrix biMatrix) throws NotFoundException{
        int width=biMatrix.width();
        int height=biMatrix.height();
        int[] buckets=new int[256];

        for(int y=1;y<5;y++){
            int row=height*y/5;
            int right=(width*4)/5;
            for(int column=width/5;column<right;column++){
                int gray=biMatrix.getGray(column,row);
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
        if(VERBOSE){Log.d(TAG, "threshold:"+bestValley);}
        return bestValley;

    }
    /*
    public static BiMatrix convertAndGetThreshold(BiMatrix biMatrix) throws NotFoundException{
        int threshold=threshold(img);
        if(VERBOSE){Log.d(TAG, "threshold:"+threshold);}
        int width=CameraSettings.previewWidth;
        int height=CameraSettings.previeHeight;
        BiMatrix biMatrix=new BiMatrix(img,width,height);
        biMatrix.setThreshold(threshold);
        return biMatrix;
    }
    */
}
