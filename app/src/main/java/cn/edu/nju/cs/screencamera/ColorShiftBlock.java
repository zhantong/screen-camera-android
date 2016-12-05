package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 2016/12/2.
 */

public class ColorShiftBlock implements Block {
    private float[] samplePoints=new float[]{0.5f,0.5f,0.1f,0.5f,0.5f,0.1f,0.9f,0.5f,0.5f,0.9f};
    public int getBitsPerUnit() {
        return 4;
    }
    public float[] getSamplePoints(){
        return samplePoints;
    }

    public int getNumSamplePoints() {
        return samplePoints.length/2;
    }
    private static int indexToValue(int index){
        switch (index){
            case 1:
                return 2;
            case 2:
                return 0;
            case 3:
                return 3;
            case 4:
                return 1;
        }
        throw new IllegalArgumentException("wrong index "+index+" should be 1 - 4.");
    }
    public static int getClear(boolean isWhite,int x,int y,int[] rawPoints,int offset){
        int[] values=new int[2];
        for(int channel=0;channel<2;channel++) {
            int[] points=new int[rawPoints.length];
            switch (channel){
                case 0:
                    for(int j=0;j<rawPoints.length;j++){
                        points[j]=rawPoints[j]>>8;
                    }
                    break;
                case 1:
                    for(int j=0;j<rawPoints.length;j++){
                        points[j]=rawPoints[j]&0xff;
                    }
            }
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
            values[channel]=indexToValue(target);
        }
        return (values[0]<<2)|values[1];
    }
    public static int[] getMixed(boolean isFormerWhite,int[] thresholds,int x,int y,int[] rawPoints,int offset){
        int[][] values=new int[2][2];
        for(int channel=0;channel<2;channel++) {
            int[] points=new int[rawPoints.length];
            switch (channel){
                case 0:
                    for(int j=0;j<rawPoints.length;j++){
                        points[j]=rawPoints[j]>>8;
                    }
                    break;
                case 1:
                    for(int j=0;j<rawPoints.length;j++){
                        points[j]=rawPoints[j]&0xff;
                    }
            }
            int threshold=thresholds[channel];
            int min = 256, max = -1;
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
                values[0][channel]=value;
                values[1][channel]=value;
                continue;
            }
            int minValue = indexToValue(minIndex);
            int maxValue = indexToValue(maxIndex);
            if ((isFormerWhite && ((x + y) % 2 == 0)) || (!isFormerWhite && ((x + y) % 2 == 1))) {
                values[0][channel]=maxValue;
                values[1][channel]=minValue;
            } else {
                values[0][channel]=minValue;
                values[1][channel]=maxValue;
            }
        }
        return new int[]{(values[0][0]<<2)|values[0][1],(values[1][0]<<2)|values[1][1]};
    }
}
