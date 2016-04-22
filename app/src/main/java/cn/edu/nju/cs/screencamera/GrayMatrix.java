package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 15/12/27.
 */
public class GrayMatrix{
    public Point[] pixels;
    public int width;
    public int height;
    public GrayMatrix(int dimensionX,int dimensionY){
        pixels=new Point[dimensionX*dimensionY];
        width=dimensionX;
        height=dimensionY;
    }
    public int get(int x, int y) {
        int offset = y * width + x;
        return pixels[offset].value;
    }
    public Point getPoint(int x, int y) {
        int offset = y * width + x;
        return pixels[offset];
    }
    public Point[] getPoints(int x, int y) {
        return null;
    }
    public int[] getSamples(int x,int y){
        return null;
    }
    public void set(int x,int y,int pixel,int origX,int origY){
        int offset = y * width + x;
        pixels[offset] = new Point(origX,origY,pixel);
    }
    public void set(int x,int y,Point[] samples){
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
