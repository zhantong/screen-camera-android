package com.nju.cs.screencamera;

import android.util.Log;

/**
 * Created by zhantong on 15/11/15.
 */
public class FindBoarder {
    private static final String TAG = "FindBoarder";
    private static final boolean VERBOSE = true;
    public static boolean containsBlack(BiMatrix biMatrix,int a,int b,int fixed,boolean horizontal){
        if(horizontal){
            for(int x=a;x<=b;x++){
                if(biMatrix.pixelEquals(x,fixed,0)){
                    return true;
                }

            }
        }
        else{
            for(int y=a;y<=b;y++){
                if(biMatrix.pixelEquals(fixed,y,0)){
                    return true;
                }
            }
        }
        return false;
    }
    public static int[] findBoarder(BiMatrix biMatrix) throws NotFoundException{
        int width=biMatrix.width();
        int height=biMatrix.height();
        int init=20;
        int left=width/2-init;
        int right=width/2+init;
        int up=height/2-init;
        int down=height/2+init;
        if(VERBOSE){Log.d(TAG,"boarder init: up:"+up+"\t"+"right:"+right+"\t"+"down:"+down+"\t"+"left:"+left);}
        if(left<0||right>=width||up<0||down>=height){
            throw NotFoundException.getNotFoundInstance();
        }
        boolean flag;
        while(true){
            flag=false;
            while(containsBlack(biMatrix,up,down,right,false)&&right<width){
                right++;
                flag=true;

            }
            while(containsBlack(biMatrix,left,right,down,true)&&down<height){
                down++;
                flag=true;
            }
            while(containsBlack(biMatrix,up,down,left,false)&&left>0){
                left--;
                flag=true;
            }
            while(containsBlack(biMatrix,left,right,up,true)&&up>0){
                up--;
                flag=true;
            }
            if(!flag){
                break;
            }
        }
        if(VERBOSE){Log.d(TAG,"find boarder: up:"+up+"\t"+"right:"+right+"\t"+"down:"+down+"\t"+"left:"+left);}
        if(left==0||up==0||right==width||down==height){
            throw NotFoundException.getNotFoundInstance();
        }
        if(left==width/2-init&&right==width/2+init&&up==height/2-init&&down==height/2+init){
            throw NotFoundException.getNotFoundInstance();
        }
        int[] vertexs=new int[8];
        left=findVertex(biMatrix,up,down,left,vertexs,0,3,false,false);
        if(VERBOSE){Log.d(TAG,"found 1 vertex");}
        up=findVertex(biMatrix,left,right,up,vertexs,0,1,true,false);
        if(VERBOSE){Log.d(TAG,"found 2 vertex");}
        right=findVertex(biMatrix,up,down,right,vertexs,1,2,false,true);
        if(VERBOSE){Log.d(TAG,"found 3 vertex");}
        down=findVertex(biMatrix,left,right,down,vertexs,3,2,true,true);
        if(VERBOSE){Log.d(TAG,"found 4 vertex");}
        if(VERBOSE){
            Log.d(TAG,"vertexes: ("+vertexs[0]+","+vertexs[1]+")\t("+vertexs[2]+","+vertexs[3]+")\t("+vertexs[4]+","+vertexs[5]+")\t("+vertexs[6]+","+vertexs[7]+")");
        }
        if(vertexs[0]==0||vertexs[2]==0||vertexs[4]==0||vertexs[6]==0){
            throw NotFoundException.getNotFoundInstance();
        }
        return vertexs;
    }
    public static int findVertex(BiMatrix biMatrix,int b1,int b2,int fixed,int[] vertexs,int p1,int p2,boolean horizontal,boolean sub){
        int mid=(b2-b1)/2;
        if(horizontal){
            while(true){
                for (int i = 1; i <= mid; i++) {
                    if (biMatrix.pixelEquals(b1+i,fixed,0)) {
                        if (!isSinglePoint(biMatrix, b1 + i, fixed)) {
                            vertexs[p1 * 2] = b1 + i;
                            vertexs[p1 * 2 + 1] = fixed;
                            return fixed;
                        }
                    }
                    if (biMatrix.pixelEquals(b2-i,fixed,0)) {
                        if (!isSinglePoint(biMatrix, b2 - i, fixed)) {
                            vertexs[p2 * 2] = b2 - i;
                            vertexs[p2 * 2 + 1] = fixed;
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
        else{
            while(true) {
                for (int i = 1; i <= mid; i++) {
                    if (biMatrix.pixelEquals(fixed,b1+i,0)) {
                        if (!isSinglePoint(biMatrix, fixed, b1 + i)) {
                            vertexs[p1 * 2] = fixed;
                            vertexs[p1 * 2 + 1] = b1 + i;
                            return fixed;
                        }
                    }
                    if (biMatrix.pixelEquals(fixed,b2-i,0)) {
                        if (!isSinglePoint(biMatrix, fixed, b2 - i)) {
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
    public static boolean isSinglePoint(BiMatrix biMatrix,int x,int y){
        int sum=biMatrix.get(x-1,y-1)+biMatrix.get(x,y-1)+biMatrix.get(x+1,y-1)+biMatrix.get(x-1,y)+biMatrix.get(x+1,y)+biMatrix.get(x-1,y+1)+biMatrix.get(x,y+1)+biMatrix.get(x+1,y+1);
        //System.out.println("isSinglePoint:"+sum);
        return sum>=6;
    }
}
