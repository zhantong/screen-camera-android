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

    // 因API 21不支持静态接口方法，故注释掉，但子类仍需实现
    // static Block fromJson(JsonObject jsonRoot);
}
