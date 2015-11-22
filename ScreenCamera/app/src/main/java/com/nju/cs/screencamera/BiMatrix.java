package com.nju.cs.screencamera;

/**
 * Created by zhantong on 15/11/21.
 */
public final class BiMatrix {
    private final int width;
    private final int height;
    private final byte[] pixels;

    public BiMatrix(int dimension){
        this(dimension,dimension);
    }
    public BiMatrix(int width,int height){
        this.width=width;
        this.height=height;
        this.pixels=new byte[width*height];
    }
    public int get(int x,int y){
        int offset=y*width+x;
        return (int)pixels[offset];
    }
    public int get(int location){
        return (int)pixels[location];
    }
    public void set(int x,int y,int pixel){
        int offset=y*width+x;
        pixels[offset]=(byte)pixel;
    }
    public void set(int location,int pixel){
        pixels[location]=(byte)pixel;
    }
    public boolean pixelEquals(int x,int y,int pixel){
        int offset=y*width+x;
        return pixels[offset]==(byte)pixel;
    }
    public int width(){
        return width;
    }
    public int height(){
        return height;
    }
}
