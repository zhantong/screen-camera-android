package cn.edu.nju.cs.screencamera;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhantong on 16/5/5.
 */
public class BitErrorCount {
    private static final String TAG = "BitErrorCount";
    Map<Integer,Integer> map;
    public BitErrorCount(){
        map=new HashMap<>();
    }
    public void put(int esi,int bitError){
        if(map.containsKey(esi)){
            int old=map.get(esi);
            if(old>bitError){
                map.put(esi,bitError);
            }
        }else{
            map.put(esi,bitError);
        }
    }
    public double averageBitError(){
        int sum=0;
        for(int bitError:map.values()){
            sum+=bitError;
        }
        return 1.0*sum/map.size();
    }
    public void logAverageBitError(){
        Log.d(TAG,"for total "+map.size()+" items, average bit error is "+averageBitError());
    }
}
