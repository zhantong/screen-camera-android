package cn.edu.nju.cs.screencamera;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhantong on 2016/11/24.
 */

public class DistrictConfig<T> {
    private List<T> configs = new ArrayList<T>();

    public DistrictConfig() {
        for (int i = 0; i < District.NUM_TYPES; i++) {
            configs.add(null);
        }
    }

    public DistrictConfig(T t) {
        this();
        for (int i = 0; i < District.NUM_TYPES; i++) {
            configs.set(i, t);
        }
    }

    public DistrictConfig(T left, T up, T right, T down) {
        this();
        configs.set(District.LEFT, left);
        configs.set(District.UP, up);
        configs.set(District.RIGHT, right);
        configs.set(District.DOWN, down);
    }

    public DistrictConfig(T left, T up, T right, T down, T leftUp, T rightUp, T rightDown, T leftDown) {
        this();
        configs.set(District.LEFT, left);
        configs.set(District.UP, up);
        configs.set(District.RIGHT, right);
        configs.set(District.DOWN, down);
        configs.set(District.LEFT_UP, leftUp);
        configs.set(District.RIGHT_UP, rightUp);
        configs.set(District.RIGHT_DOWN, rightDown);
        configs.set(District.LEFT_DOWN, leftDown);
    }

    public T get(int part) {
        return configs.get(part);
    }

    public void set(int part, T t) {
        configs.set(part, t);
    }

    JsonElement toJson() {
        return new Gson().toJsonTree(configs);
    }
}
