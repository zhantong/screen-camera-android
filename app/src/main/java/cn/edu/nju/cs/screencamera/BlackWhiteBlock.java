package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 2016/11/24.
 */

public class BlackWhiteBlock implements Block{
    public int getBitsPerUnit() {
        return 1;
    }
    public float[] getSamplePoints(){
        return new float[]{0.5f,0.5f};
    }
}
