package com.nju.cs.screencamera;

import java.util.HashMap;

/**
 * Created by zhantong on 15/12/27.
 */
public class GrayMatrix extends FileToImg{
    public Point[] pixels;
    public int width;
    public int height;
    private boolean ordered =true;
    public boolean reverse=false;
    public int blackValue;
    public int whiteValue;
    public HashMap<Integer,Point>[] bars;
    public GrayMatrix(int dimension){
        pixels=new Point[dimension*dimension];
        width=dimension;
        height=dimension;
    }
    public int get(int x, int y) {
        int offset = y * width + x;
        return pixels[offset].value;
    }
    public void set(int x, int y, int pixel) {
        int offset = y * width + x;
        pixels[offset] = new Point(x,y,pixel);
    }
    public void set(int x,int y,int pixel,int origX,int origY){
        int offset = y * width + x;
        pixels[offset] = new Point(origX,origY,pixel);
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

    public int toBinary(int x,int y){
        int value=get(x,y);
        int origY=pixels[y * width + x].y;
        int left;
        int right;

        if(bars[0].containsKey(origY)){
            left=bars[0].get(origY).value;
        }
        else {
            left=bars[2].get(origY).value;
        }

        if(bars[1].containsKey(origY)){
            right=bars[1].get(origY).value;
        }
        else {
            right=bars[3].get(origY).value;
        }

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

    /*
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
    */
    public byte[] getHead(){
        int index=0;
        byte[] array=new byte[contentLength/8];
        for(int x=0;x<array.length*8;x++){
            //System.out.print(get(x, 0));
            array[index/8]<<=1;
            if(get(x,0)>130){
                array[index / 8] |= 0x01;
            }
            index++;
        }
        //System.out.println();
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
                int b=toBinary(x,y);
                binaryMatrix.set(x,y,b);
                //System.out.print(b);
            }
            //System.out.println();
        }
        //System.out.println("whiteValue:"+whiteValue+"\tblackValue:"+blackValue);
        return binaryMatrix;
    }

    public byte[] getContent(){
        //whiteValue=get(frameBlackLength+frameVaryLength,frameBlackLength);
        //blackValue=get(frameBlackLength+frameVaryLength+1,frameBlackLength);
        if(get(1,2)>get(width-2,2)){
            ordered =false;
        }
        int index=0;
        byte[] array=new byte[contentLength*contentLength/8];
        for(int y=frameBlackLength+frameVaryLength+frameVaryTwoLength;y<frameBlackLength+frameVaryLength+frameVaryTwoLength+contentLength;y++){
            if(get(0,y)<125){
                blackValue=get(0,y);
                whiteValue=get(0,y+1);
            }
            else{
                blackValue=get(0,y+1);
                whiteValue=get(0,y);
            }
            //System.out.println("black value:"+blackValue+"\twhite value:"+whiteValue);
            //int left=get(1,y);
            //int right=get(width-2,y);
            int left=0;
            int right=0;
            //System.out.println("left:"+left+"\tright:"+right);
            for(int x=frameBlackLength+frameVaryLength+frameVaryTwoLength;x<frameBlackLength+frameVaryLength+frameVaryTwoLength+contentLength;x++){

                //System.out.println(x+" "+y+" "+get(x,y)+" "+pixels[y * width + x].x+" "+pixels[y * width + x].y+" "+pixels[y * width + x].value);
                /*
                int origY=pixels[y * width + x].y;
                if(bars[0].containsKey(origY)){
                    left=bars[0].get(origY).value;
                }
                else {
                    left=bars[2].get(origY).value;
                }
                if(bars[1].containsKey(origY)){
                    right=bars[1].get(origY).value;
                }
                else {
                    right=bars[3].get(origY).value;
                }
                */
                array[index/8]<<=1;
                if(toBinary(x,y)==1){
                    array[index / 8] |= 0x01;
                    if(false&&y==frameBlackLength+frameVaryLength+frameVaryTwoLength) {
                        System.out.print("1 ");
                    }
                }
                else{
                    if(false&&y==frameBlackLength+frameVaryLength+frameVaryTwoLength) {
                        System.out.print("0 ");
                    }
                }
                index++;
            }
            if(false&&y==frameBlackLength+frameVaryLength+frameVaryTwoLength) {
                System.out.println();
            }
        }
        return array;
    }
    public void print(){
        System.out.println("width:"+width+"\theight:"+height);
        for(int y=0;y<height;y++){
            for(int x=0;x<width;x++){
                System.out.print(get(x,y)+" ");
            }
            System.out.println();
        }
    }
}
