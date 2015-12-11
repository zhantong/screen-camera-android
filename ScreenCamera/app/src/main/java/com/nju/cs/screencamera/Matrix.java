package com.nju.cs.screencamera;

import android.util.Log;

/**
 * Created by zhantong on 15/11/21.
 */
public final class Matrix {
    private static final boolean VERBOSE = false;
    private static final String TAG = "Matrix";
    private final int width;
    private final int height;
    private final byte[] pixels;
    private int threshold =0;
    private int[] borders;
    private PerspectiveTransform transform;

    public Matrix(int dimension){
        this(dimension,dimension);
    }
    public Matrix(int width, int height){
        this.width=width;
        this.height=height;
        this.pixels=new byte[width*height];
    }
    public Matrix(byte[] pixels, int width, int height) throws NotFoundException{
        this.pixels=pixels;
        this.width=width;
        this.height=height;
        this.threshold=getThreshold();
        this.borders=FindBoarder.findBoarder(this);
    }

    public void perspectiveTransform(float p1ToX, float p1ToY,
                                float p2ToX, float p2ToY,
                                float p3ToX, float p3ToY,
                                float p4ToX, float p4ToY){
        transform=PerspectiveTransform.quadrilateralToQuadrilateral(p1ToX, p1ToY,
                                                                    p2ToX, p2ToY,
                                                                    p3ToX, p3ToY,
                                                                    p4ToX, p4ToY,
                                                            borders[0], borders[1],
                                                            borders[2], borders[3],
                                                            borders[4], borders[5],
                                                            borders[6], borders[7]);
    }

    public int getGray(int x,int y){
        return pixels[y*width+x]&0xff;
    }
    public int get(int x,int y){
        int gray = pixels[y*width+x]&0xff;
        if(gray<= threshold) {
            return 0;
        }
        else{
            return 1;
        }
    }
    public boolean pixelEquals(int x,int y,int pixel){
        return get(x,y)==pixel;
    }
    public int width(){
        return width;
    }
    public int height(){
        return height;
    }
    public String sampleRow(int dimensionX, int dimensionY, int row){
        StringBuilder stringBuilder=new StringBuilder();
        float[] points=new float[2*dimensionX];
        int max=points.length;
        float rowValue=(float)row+0.5f;
        for(int x=0;x<max;x+=2){
            points[x]=(float)(x/2)+0.5f;
            points[x+1]=rowValue;
        }
        transform.transformPoints(points);
        for(int x=0;x<max;x+=2){
            if(pixelEquals((int) points[x], (int) points[x + 1], 1)){
                stringBuilder.append('1');
            }
            else{
                stringBuilder.append('0');
            }
        }
        return stringBuilder.toString();
    }
    public BinaryMatrix sampleGrid(int dimensionX,int dimensionY){
        BinaryMatrix binaryMatrix=new BinaryMatrix(dimensionX,dimensionY);
        //int[][] result=new int[dimensionX][dimensionY];
        float[] points=new float[2*dimensionX];
        int max=points.length;
        for(int y=0;y<dimensionY;y++){
            float iValue=(float)y+0.5f;
            for(int x=0;x<max;x+=2){
                points[x]=(float)(x/2)+0.5f;
                points[x+1]=iValue;
            }
            transform.transformPoints(points);
            for(int x=0;x<max;x+=2){
                if(pixelEquals((int)points[x],(int)points[x+1],1)){
                    binaryMatrix.set(x/2,y,1);
                }
            }
        }
        return binaryMatrix;
    }
    private int getThreshold() throws NotFoundException{
        int[] buckets=new int[256];

        for(int y=1;y<5;y++){
            int row=height*y/5;
            int right=(width*4)/5;
            for(int column=width/5;column<right;column++){
                int gray=getGray(column, row);
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
            throw new NotFoundException("can't get proper binary threshold");
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
        if(VERBOSE){Log.d(TAG, "threshold:" + bestValley);}
        return bestValley;
    }
}
