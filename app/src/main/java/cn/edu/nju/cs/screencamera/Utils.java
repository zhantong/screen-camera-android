package cn.edu.nju.cs.screencamera;

import java.io.File;
import java.util.BitSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zhantong on 2016/9/29.
 */

public final class Utils {
    public static String combinePaths(String ... paths){
        if(paths.length==0){
            return "";
        }
        File combined=new File(paths[0]);
        int i=1;
        while(i<paths.length){
            combined=new File(combined,paths[i]);
            i++;
        }
        return combined.getPath();
    }
    public static int calculateMean(int[] array,int low,int high){
        int sum=0;
        for(int i=low;i<=high;i++){
            sum+=array[i];
        }
        return sum/(high-low+1);
    }
    public static int[] extractResolution(String string){
        Pattern pattern= Pattern.compile(".*?(\\d+)x(\\d+).*");
        Matcher matcher=pattern.matcher(string);
        if(matcher.find()){
            int width=Integer.parseInt(matcher.group(1));
            int height=Integer.parseInt(matcher.group(2));
            return new int[]{width,height};
        }
        return null;
    }
    public static int bitsToInt(BitSet bitSet, int length, int offset){
        int value=0;
        for(int i=0;i<length;i++){
            value+=bitSet.get(offset+i)?(1<<i):0;
        }
        return value;
    }
    public static void crc8Check(int data,int check) throws CRCCheckException{
        CRC8 crc8=new CRC8();
        crc8.reset();
        crc8.update(data);
        int real=(int)crc8.getValue();
        if(check!=real||data<0){
            throw CRCCheckException.getNotFoundInstance();
        }
    }
}
