package cn.edu.nju.cs.screencamera;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Created by zhantong on 16/4/22.
 */
abstract class GrayMatrix {
    Point[] pixels;

    abstract int get(int x, int y);
    abstract Point getPoint(int x, int y);
    abstract Point[] getPoints(int x, int y);
    abstract int[] getSamples(int x,int y);
    abstract void set(int x,int y,int pixel,int origX,int origY);
    abstract void set(int x,int y,Point[] samples);
    abstract void print();
    abstract void print(int x,int y);
    abstract JsonNode toJSON();
}
