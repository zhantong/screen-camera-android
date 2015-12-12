package com.nju.cs.screencamera;

/**
 * Created by zhantong on 15/12/11.
 */
public class RGBMatrix extends Matrix {
    public RGBMatrix(int dimension){
        super(dimension,dimension);
    }
    public RGBMatrix(int width, int height){
        super(width, height);
    }
    public RGBMatrix(byte[] pixels, int width, int height) throws NotFoundException{
        super(pixels,width,height);
    }
    public int getGray(int x,int y){
        int offset=(y*width+x)*4;
        int p=pixels[offset];
        int r=pixels[offset]&0xFF;
        int g=pixels[offset+1]&0xFF;
        int b=pixels[offset+2]&0xFF;
        int gray=((b*29+g*150+r*77+128)>>8);
        return gray;
    }
}
