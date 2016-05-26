package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 16/5/26.
 */
public class ReedSolomonEncode {
    static {
        System.loadLibrary("Reed-Solomon");
    }
    public static native int[] encode(int[] array,int length);
    public static native int[] decode(int[] array,int length);
}
