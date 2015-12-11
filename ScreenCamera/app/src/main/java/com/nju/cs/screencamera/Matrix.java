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
        this.borders=findBoarder();
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

    public boolean containsBlack(int start,int end,int fixed,boolean horizontal){
        if(horizontal){
            for(int x=start;x<=end;x++){
                if(pixelEquals(x, fixed, 0)){
                    return true;
                }

            }
        }
        else{
            for(int y=start;y<=end;y++){
                if(pixelEquals(fixed, y, 0)){
                    return true;
                }
            }
        }
        return false;
    }
    public int[] findBoarder() throws NotFoundException{
        int init=20;
        int left=width/2-init;
        int right=width/2+init;
        int up=height/2-init;
        int down=height/2+init;
        int leftOrig=left;
        int rightOrig=right;
        int upOrig=up;
        int downOrig=down;
        if(VERBOSE){Log.d(TAG,"boarder init: up:"+up+"\t"+"right:"+right+"\t"+"down:"+down+"\t"+"left:"+left);}
        if(left<0||right>=width||up<0||down>=height){
            throw new NotFoundException("frame size too small");
        }
        boolean flag;
        while(true){
            flag=false;
            while(containsBlack(up,down,right,false)&&right<width){
                right++;
                flag=true;

            }
            while(containsBlack(left,right,down,true)&&down<height){
                down++;
                flag=true;
            }
            while(containsBlack(up,down,left,false)&&left>0){
                left--;
                flag=true;
            }
            while(containsBlack(left,right,up,true)&&up>0){
                up--;
                flag=true;
            }
            if(!flag){
                break;
            }
        }
        if(VERBOSE){Log.d(TAG,"find boarder: up:"+up+"\t"+"right:"+right+"\t"+"down:"+down+"\t"+"left:"+left);}
        if((left==0||up==0||right==width||down==height)||(left==leftOrig&&right==rightOrig&&up==upOrig&&down==downOrig)){
            throw new NotFoundException("didn't find any possible bar code");
        }
        int[] vertexs=new int[8];
        left=findVertex(up,down,left,leftOrig,vertexs,0,3,false,false);
        if(VERBOSE){Log.d(TAG,"found 1 vertex");}
        up=findVertex(left,right,up,upOrig,vertexs,0,1,true,false);
        if(VERBOSE){Log.d(TAG,"found 2 vertex");}
        right=findVertex(up,down,right,rightOrig,vertexs,1,2,false,true);
        if(VERBOSE){Log.d(TAG,"found 3 vertex");}
        down=findVertex(left,right,down,downOrig,vertexs,3,2,true,true);
        if(VERBOSE){Log.d(TAG,"found 4 vertex");}
        if(VERBOSE){
            Log.d(TAG,"vertexes: ("+vertexs[0]+","+vertexs[1]+")\t("+vertexs[2]+","+vertexs[3]+")\t("+vertexs[4]+","+vertexs[5]+")\t("+vertexs[6]+","+vertexs[7]+")");
        }
        if(vertexs[0]==0||vertexs[2]==0||vertexs[4]==0||vertexs[6]==0){
            throw new NotFoundException("vertexs error");
        }
        return vertexs;
    }
    public int findVertex(int b1,int b2,int fixed,int fixedOrig,int[] vertexs,int p1,int p2,boolean horizontal,boolean sub) throws NotFoundException{
        int mid=(b2-b1)/2;
        if(horizontal){
            while(true){
                for (int i = 1; i <= mid; i++) {
                    if (pixelEquals(b1+i,fixed,0)) {
                        if (!isSinglePoint(b1 + i, fixed)) {
                            vertexs[p1 * 2] = b1 + i;
                            vertexs[p1 * 2 + 1] = fixed;
                            return fixed;
                        }
                    }
                    if (pixelEquals(b2-i,fixed,0)) {
                        if (!isSinglePoint(b2 - i, fixed)) {
                            vertexs[p2 * 2] = b2 - i;
                            vertexs[p2 * 2 + 1] = fixed;
                            return fixed;
                        }
                    }
                }
                if(sub){
                    fixed--;
                    if(fixed<=fixedOrig){
                        throw new NotFoundException("didn't find any possible bar code");
                    }
                }
                else{
                    fixed++;
                    if(fixed>=fixedOrig){
                        throw new NotFoundException("didn't find any possible bar code");
                    }
                }
            }
        }
        else{
            while(true) {
                for (int i = 1; i <= mid; i++) {
                    if (pixelEquals(fixed,b1+i,0)) {
                        if (!isSinglePoint(fixed, b1 + i)) {
                            vertexs[p1 * 2] = fixed;
                            vertexs[p1 * 2 + 1] = b1 + i;
                            return fixed;
                        }
                    }
                    if (pixelEquals(fixed,b2-i,0)) {
                        if (!isSinglePoint(fixed, b2 - i)) {
                            vertexs[p2 * 2] = fixed;
                            vertexs[p2 * 2 + 1] = b2 - i;
                            return fixed;
                        }
                    }
                }
                if(sub){
                    fixed--;
                }
                else{
                    fixed++;
                }
            }
        }
    }
    public boolean isSinglePoint(int x,int y){
        int sum= get(x-1,y-1)+ get(x,y-1)+ get(x+1,y-1)+ get(x-1,y)+ get(x+1,y)+ get(x-1,y+1)+ get(x,y+1)+ get(x+1,y+1);
        //System.out.println("isSinglePoint:"+sum);
        return sum>=6;
    }
}
