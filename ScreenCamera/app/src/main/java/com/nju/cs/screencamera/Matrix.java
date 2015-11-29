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
}
