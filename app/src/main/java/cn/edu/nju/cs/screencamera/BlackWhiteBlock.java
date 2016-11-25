package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 2016/11/24.
 */

public class BlackWhiteBlock implements Block{
    private float[] samplePoints=new float[]{0.5f,0.5f};
    public int getBitsPerUnit() {
        return 1;
    }
    public float[] getSamplePoints(){
        return samplePoints;
    }
    public int getNumSamplePoints(){
        return samplePoints.length/2;
    }
}
