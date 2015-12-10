package com.nju.cs.screencamera;

/**
 * Created by zhantong on 15/11/21.
 */
public final class BiMatrix {
    private final int width;
    private final int height;
    private final byte[] pixels;
    private int threshold =0;
    private int[] borders;
    private GridSampler gs;

    public BiMatrix(int dimension){
        this(dimension,dimension);
    }
    public BiMatrix(int width,int height){
        this.width=width;
        this.height=height;
        this.pixels=new byte[width*height];
    }
    public BiMatrix(byte[] pixels,int width,int height) throws NotFoundException{
        this.pixels=pixels;
        this.width=width;
        this.height=height;
        this.threshold=Binarizer.threshold(this);
        this.borders=FindBoarder.findBoarder(this);
    }
    public void perspectiveTransform(float p1ToX, float p1ToY,
                                float p2ToX, float p2ToY,
                                float p3ToX, float p3ToY,
                                float p4ToX, float p4ToY){
        gs=new GridSampler(p1ToX, p1ToY, p2ToX, p2ToY, p3ToX, p3ToY, p4ToX, p4ToY, borders[0], borders[1], borders[2], borders[3], borders[4], borders[5], borders[6], borders[7]);
    }
    public String sampleRow(int dimensionX, int dimensionY, int row){
        return gs.sampleRow(this,dimensionX,dimensionY,row);
    }
    public Matrix sampleGrid(int dimensionX,int dimensionY){
        return gs.sampleGrid(this,dimensionX,dimensionY);
    }
    public int getGray(int x,int y){
        return pixels[y*width+x]&0xff;
    }
    public int get(int x,int y){
        int offset=y*width+x;
        int gray = pixels[offset]&0xff;
        if(gray<= threshold){
            return 0;
        }
        if(gray> threshold){
            return 1;
        }
        return 0;
    }
    public void setThreshold(int threshold){
        this.threshold = threshold;
    }
    public int getThreshold(){
        return threshold;
    }
    public int get(int location){
        return pixels[location];
    }
    /*
    public void set(int x,int y,int pixel){
        int offset=y*width+x;
        pixels[offset]=pixel;
    }
    public void set(int location,int pixel){
        pixels[location]=pixel;
    }
    */
    public boolean pixelEquals(int x,int y,int pixel){
        int res=get(x,y);
        return res==pixel;
    }
    public boolean pixelEqualsBack(int x,int y,int pixel){
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
