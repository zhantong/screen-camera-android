package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 16/5/26.
 */
public class AndroidJni {
    static {
        System.loadLibrary("Android-Jni");
    }

    public static native int[] RSEncode(int[] array, int length);

    public static native int[] RSDecode(int[] array, int length);

    public static native int[] findBorder(byte[] array, int imgColorType, int threshold, int imgWidth, int imgHeight, int[] initBorder);
}
