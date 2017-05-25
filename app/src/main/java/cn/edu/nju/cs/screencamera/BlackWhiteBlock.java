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

    @Override
    public int[] getChannels() {
        return null;
    }
    public static int[] getMixed(int value,int black,int white,int ref1,int ref2){
        int minDistance=10000;
        int[] result=null;
        int distance=Math.abs(value-black);
        if(distance<minDistance){
            minDistance=distance;
            result=new int[]{0,0};
        }
        distance=Math.abs(value-white);
        if(distance<minDistance){
            minDistance=distance;
            result=new int[]{1,1};
        }
        distance=Math.abs(value-ref1);
        if(distance<minDistance){
            minDistance=distance;
            result=new int[]{0,1};
        }
        distance=Math.abs(value-ref2);
        if(distance<minDistance){
            minDistance=distance;
            result=new int[]{1,0};
        }
        return result;
    }
}
