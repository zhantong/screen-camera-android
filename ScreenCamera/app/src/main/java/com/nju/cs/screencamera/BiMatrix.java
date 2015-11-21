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
    public byte get(int x,int y){
        int offset=y*width+x;
        return pixels[offset];
    }
    public void set(int x,int y,byte pixel){
        int offset=y*width+x;
        pixels[offset]=pixel;
    }
    public void set(int location,byte pixel){
        pixels[location]=pixel;
    }
}
