package cn.edu.nju.cs.screencamera;

import com.google.gson.JsonObject;

/**
 * Created by zhantong on 2016/11/24.
 */

public class ShiftBlock implements Block {
    private float[] samplePoints = new float[]{0.5f, 0.5f, 0.1f, 0.5f, 0.5f, 0.1f, 0.9f, 0.5f, 0.5f, 0.9f};

    public int getBitsPerUnit() {
        return 2;
    }

    public float[] getSamplePoints() {
        return samplePoints;
    }

    @Override
    public int getNumSamplePoints() {
        return samplePoints.length / 2;
    }

    @Override
    public int[] getChannels() {
        return null;
    }

    public static Block fromJson(JsonObject jsonRoot) {
        return new ShiftBlock();
    }

    private static int indexToValue(int index) {
        switch (index) {
            case 1:
                return 2;
            case 2:
                return 0;
            case 3:
                return 3;
            case 4:
                return 1;
        }
        throw new IllegalArgumentException();
    }

    public static int getClear(boolean isWhite, int x, int y, int[] points, int offset) {
        int target = -1;
        if ((isWhite && ((x + y) % 2 == 0)) || (!isWhite && ((x + y) % 2 == 1))) {
            int max = 0;
            for (int i = 1; i < 5; i++) {
                int current = points[offset + i];
                if (max < current) {
                    max = current;
                    target = i;
                }
            }
        } else {
            int min = 255;
            for (int i = 1; i < 5; i++) {
                int current = points[offset + i];
                if (min > current) {
                    min = current;
                    target = i;
                }
            }
        }
        return indexToValue(target);
    }

    public static int[] getMixed(boolean isFormerWhite, int threshold, int x, int y, int[] points, int offset) {
        int min = 255, max = 0;
        int minIndex = -1, maxIndex = -1;
        for (int i = 1; i < 5; i++) {
            int current = points[offset + i];
            if (min > current) {
                min = current;
                minIndex = i;
            }
            if (max < current) {
                max = current;
                maxIndex = i;
            }
        }
        int center = points[offset];
        int minOffset = Math.abs(center - min);
        int maxOffset = Math.abs(center - max);
        if (minOffset < threshold || maxOffset < threshold) {
            int target = (minOffset < maxOffset) ? minIndex : maxIndex;
            int value = indexToValue(target);
            return new int[]{value, value};
        }
        int minValue = indexToValue(minIndex);
        int maxValue = indexToValue(maxIndex);
        if ((isFormerWhite && ((x + y) % 2 == 0)) || (!isFormerWhite && ((x + y) % 2 == 1))) {
            return new int[]{maxValue, minValue};
        } else {
            return new int[]{minValue, maxValue};
        }
    }
}
