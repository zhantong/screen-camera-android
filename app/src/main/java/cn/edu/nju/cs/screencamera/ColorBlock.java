package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 2017/5/15.
 */

public class ColorBlock implements Block {
    private float[] samplePoints = new float[]{0.5f, 0.5f};
    private int bitsPerUnit;

    public ColorBlock(int bitsPerUnit) {
        this.bitsPerUnit = bitsPerUnit;
    }

    public int getBitsPerUnit() {
        return bitsPerUnit;
    }

    public float[] getSamplePoints() {
        return samplePoints;
    }

    public int getNumSamplePoints() {
        return samplePoints.length / 2;
    }

    @Override
    public int[] getChannels() {
        return null;
    }
}
