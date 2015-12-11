package com.nju.cs.screencamera;

import android.util.Log;

/**
 * Created by zhantong on 15/11/15.
 */
public class FindBoarder {
    private static final String TAG = "FindBoarder";
    private static final boolean VERBOSE = false;
    public static boolean containsBlack(Matrix matrix,int a,int b,int fixed,boolean horizontal){
        if(horizontal){
            for(int x=a;x<=b;x++){
                if(matrix.pixelEquals(x,fixed,0)){
                    return true;
                }

            }
        }
        else{
            for(int y=a;y<=b;y++){
                if(matrix.pixelEquals(fixed,y,0)){
                    return true;
                }
            }
        }
        return false;
    }
    public static int[] findBoarder(Matrix matrix) throws NotFoundException{
        int width= matrix.width();
        int height= matrix.height();
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
            while(containsBlack(matrix,up,down,right,false)&&right<width){
                right++;
                flag=true;

            }
            while(containsBlack(matrix,left,right,down,true)&&down<height){
                down++;
                flag=true;
            }
            while(containsBlack(matrix,up,down,left,false)&&left>0){
                left--;
                flag=true;
            }
            while(containsBlack(matrix,left,right,up,true)&&up>0){
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
        left=findVertex(matrix,up,down,left,leftOrig,vertexs,0,3,false,false);
        if(VERBOSE){Log.d(TAG,"found 1 vertex");}
        up=findVertex(matrix,left,right,up,upOrig,vertexs,0,1,true,false);
        if(VERBOSE){Log.d(TAG,"found 2 vertex");}
        right=findVertex(matrix,up,down,right,rightOrig,vertexs,1,2,false,true);
        if(VERBOSE){Log.d(TAG,"found 3 vertex");}
        down=findVertex(matrix,left,right,down,downOrig,vertexs,3,2,true,true);
        if(VERBOSE){Log.d(TAG,"found 4 vertex");}
        if(VERBOSE){
            Log.d(TAG,"vertexes: ("+vertexs[0]+","+vertexs[1]+")\t("+vertexs[2]+","+vertexs[3]+")\t("+vertexs[4]+","+vertexs[5]+")\t("+vertexs[6]+","+vertexs[7]+")");
        }
        if(vertexs[0]==0||vertexs[2]==0||vertexs[4]==0||vertexs[6]==0){
            throw new NotFoundException("vertexs error");
        }
        return vertexs;
    }
    public static int findVertex(Matrix matrix,int b1,int b2,int fixed,int fixedOrig,int[] vertexs,int p1,int p2,boolean horizontal,boolean sub) throws NotFoundException{
        int mid=(b2-b1)/2;
        if(horizontal){
            while(true){
                for (int i = 1; i <= mid; i++) {
                    if (matrix.pixelEquals(b1+i,fixed,0)) {
                        if (!isSinglePoint(matrix, b1 + i, fixed)) {
                            vertexs[p1 * 2] = b1 + i;
                            vertexs[p1 * 2 + 1] = fixed;
                            return fixed;
                        }
                    }
                    if (matrix.pixelEquals(b2-i,fixed,0)) {
                        if (!isSinglePoint(matrix, b2 - i, fixed)) {
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
                    if (matrix.pixelEquals(fixed,b1+i,0)) {
                        if (!isSinglePoint(matrix, fixed, b1 + i)) {
                            vertexs[p1 * 2] = fixed;
                            vertexs[p1 * 2 + 1] = b1 + i;
                            return fixed;
                        }
                    }
                    if (matrix.pixelEquals(fixed,b2-i,0)) {
                        if (!isSinglePoint(matrix, fixed, b2 - i)) {
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
    public static boolean isSinglePoint(Matrix matrix,int x,int y){
        int sum= matrix.get(x-1,y-1)+ matrix.get(x,y-1)+ matrix.get(x+1,y-1)+ matrix.get(x-1,y)+ matrix.get(x+1,y)+ matrix.get(x-1,y+1)+ matrix.get(x,y+1)+ matrix.get(x+1,y+1);
        //System.out.println("isSinglePoint:"+sum);
        return sum>=6;
    }
}
