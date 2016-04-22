package cn.edu.nju.cs.screencamera;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by zhantong on 16/4/22.
 */
public class MediaToFileZoom extends MediaToFile {
    private static final String TAG = "MediaToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log
    public MediaToFileZoom(TextView debugView, TextView infoView, Handler handler) {
        super(debugView,infoView,handler);
    }
    public boolean checkBitSet(BitSet con, MatrixZoom matrix){
        if(realBitSetList==null){
            String filePath= Environment.getExternalStorageDirectory() + "/bitsets.txt";
            realBitSetList= (LinkedList<BitSet>)readCheckFile(filePath);
            if(VERBOSE){
                Log.d(TAG,"load check file success:"+filePath);}
            return false;
        }
        //Log.d(TAG,"check content bit size:"+con.length());
        BitSet least=null;
        int leastCount=10000;
        for(BitSet current:realBitSetList){
            BitSet clone=(BitSet)con.clone();
            //Log.d(TAG,"current check bit size:"+clone.length());
            clone.xor(current);
            if(clone.cardinality()<leastCount){
                least=clone;
                leastCount=clone.cardinality();
            }
            if(clone.cardinality()==0){
                break;
            }
        }
        if(leastCount!=0){
            Log.d(TAG, "check least count:" + leastCount);
            printContentBitSet(least);
            List<Integer> test=new ArrayList<>();
            for(int i=least.nextSetBit(0);i>=0;i=least.nextSetBit(i+1)){
                int real=i/2;
                if(!test.contains(real)) {
                    test.add(real);
                }
            }
            matrix.check(test);
            return false;
        }
        return true;
    }
}
