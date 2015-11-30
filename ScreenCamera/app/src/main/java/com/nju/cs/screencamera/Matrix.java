package com.nju.cs.screencamera;

/**
 * Created by zhantong on 15/11/29.
 */
public class Matrix {
    private final int width;
    private final int height;
    private final int[] pixels;

    public Matrix(int dimension){
        this(dimension,dimension);
    }
    public Matrix(int width,int height){
        this.width=width;
        this.height=height;
        this.pixels=new int[width*height];
    }
    public Matrix(int[] pixels,int width,int height){
        this.pixels=pixels;
        this.width=width;
        this.height=height;
    }
    public int get(int x,int y){
        int offset=y*width+x;
        return pixels[offset];
    }
    public int get(int location){
        return pixels[location];
    }
    public void set(int x,int y,int pixel){
        int offset=y*width+x;
        pixels[offset]=pixel;
    }
    public void set(int location,int pixel){
        pixels[location]=pixel;
    }
    public boolean pixelEquals(int x,int y,int pixel){
        int offset=y*width+x;
        return pixels[offset]==pixel;
    }
    public int width(){
        return width;
    }
    public int height(){
        return height;
    }
    public void toArray(int left,int top,int right,int down,int[] array){
        int index=0;
        for(int j=top;j<down;j++){
            for(int i=left;i<right;i++){
                array[index/8]<<=1;
                if(get(i,j)==1){
                    array[index/8]|=0x01;
                }
                index++;
            }
        }
    }
}
