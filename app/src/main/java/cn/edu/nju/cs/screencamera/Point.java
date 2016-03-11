package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 16/2/25.
 */
public class Point {
    int x;
    int y;
    int value;
    public  Point(int x,int y,int value){
        this.x=x;
        this.y=y;
        this.value=value;
    }
    public void print(){
        System.out.println(x+" "+y+" "+value);
    }
}
