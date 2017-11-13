package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 2016/11/24.
 */

public interface Block {
    int getBitsPerUnit();

    float[] getSamplePoints();

    int getNumSamplePoints();

    int[] getChannels();
}
