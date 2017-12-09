package cn.edu.nju.cs.screencamera;

import com.google.gson.JsonObject;

/**
 * Created by zhantong on 2016/11/24.
 */

public interface Block {
    int getBitsPerUnit();

    float[] getSamplePoints();

    int getNumSamplePoints();

    int[] getChannels();

    Block fromJson(JsonObject jsonRoot);
}
