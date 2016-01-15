package com.nju.cs.screencamera;

/**
 * Created by zhantong on 15/12/27.
 */
public class GrayMatrix extends FileToImg{
    public int[] pixels;
    public int width;
    public int height;
    private boolean ordered =true;
    public boolean reverse=false;
    public int blackValue;
    public int whiteValue;
    public GrayMatrix(int dimension){
        pixels=new int[dimension*dimension];
        width=dimension;
        height=dimension;
    }
    public int get(int x, int y) {
        int offset = y * width + x;
        return pixels[offset];
    }
    public void set(int x, int y, int pixel) {
        int offset = y * width + x;
        pixels[offset] = pixel;
    }
    public int toBinary(int value,int left,int right){
        int minDistance=10000;
        int index=-1;
        int distance=Math.abs(value-blackValue);
        if(distance<minDistance){
            minDistance=distance;
            index=0;
        }
        distance=Math.abs(value-whiteValue);
        if(distance<minDistance){
            minDistance=distance;
            index=1;
        }
        distance=Math.abs(value-left);
        if(distance<minDistance){
            minDistance=distance;
            if((ordered&&!reverse)||(!ordered&&reverse)){
                index=0;
            }else {
                index=1;
            }
        }
        distance=Math.abs(value-right);
        if(distance<minDistance){
            minDistance=distance;
            if((ordered&&!reverse)||(!ordered&&reverse)){
                index=1;
            }else {
                index=0;
            }
        }
        return index;
    }
    public String getRow(int[] row){
        whiteValue=row[2];
        blackValue=row[3];
        int left=row[1];
        int right=row[width-2];
        if(left>right){
            ordered =false;
        }else {
            ordered=true;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for(int v:row){
            int b=toBinary(v,left,right);
            stringBuilder.append(b);
        }
        return stringBuilder.toString();
    }
    public byte[] getHead(){
        whiteValue=get(frameBlackLength+frameVaryLength,frameBlackLength);
        //System.out.println("white value:"+whiteValue);
        blackValue=get(frameBlackLength+frameVaryLength+1,frameBlackLength);
        //System.out.println("black value:"+blackValue);
        int index=0;
        int y=frameBlackLength;
        int left=get(1,y);
        int right=get(width-2,y);
        if(left>right){
            ordered =false;
        }else {
            ordered=true;
        }
        byte[] array=new byte[contentLength/8];
        for(int x=frameBlackLength+frameVaryLength+1;x<frameBlackLength+frameVaryLength+1+array.length*8;x++){
            array[index/8]<<=1;
            if(toBinary(get(x,y),left,right)==1){
                array[index / 8] |= 0x01;
            }
            index++;
        }
        return array;
    }
    public BinaryMatrix toBinaryMatrix(){
        BinaryMatrix binaryMatrix=new BinaryMatrix(width,height);
        /*
        whiteValue=get(width-2,height-2);
        blackValue=get(width-3,height-2);
        */
        if(get(1,2)>get(width-2,2)){
            ordered =false;
        }

        for(int y=0;y<height;y++){
            int left=get(1,y);
            int right=get(width-2,y);
            for(int x=0;x<width;x++){
                int b=toBinary(get(x,y),left,right);
                binaryMatrix.set(x,y,b);
                //System.out.print(b);
            }
            //System.out.println();
        }
        //System.out.println("whiteValue:"+whiteValue+"\tblackValue:"+blackValue);
        return binaryMatrix;
    }
    public byte[] getContent(){
        whiteValue=get(frameBlackLength+frameVaryLength,frameBlackLength);
        blackValue=get(frameBlackLength+frameVaryLength+1,frameBlackLength);
        if(get(1,2)>get(width-2,2)){
            ordered =false;
        }
        int index=0;
        byte[] array=new byte[contentLength*contentLength/8];
        for(int y=frameBlackLength+frameVaryLength;y<frameBlackLength+frameVaryLength+contentLength;y++){
            int left=get(1,y);
            int right=get(width-2,y);
            for(int x=frameBlackLength+frameVaryLength;x<frameBlackLength+frameVaryLength+contentLength;x++){
                array[index/8]<<=1;
                if(toBinary(get(x,y),left,right)==1){
                    array[index / 8] |= 0x01;
                }
                index++;
            }
        }
        return array;
    }
}
