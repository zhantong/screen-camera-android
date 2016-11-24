package cn.edu.nju.cs.screencamera;

import java.util.BitSet;

/**
 * Created by zhantong on 2016/11/24.
 */

public class BitContent {
    private BitSet content;
    public static final int ALL_ONES=0;
    public static final int ALL_ZEROS=1;
    private boolean isAllOnes=false;
    private boolean isAllZeros=false;
    public BitContent(int type){
        switch (type){
            case ALL_ONES:
                isAllOnes=true;
                break;
            case ALL_ZEROS:
                isAllZeros=true;
                break;
        }
    }
    public BitContent(BitSet content){
        this.content=content;
    }
    public int get(int pos,int length){
        if(content!=null) {
            int value = Utils.bitsToInt(content, length, pos);
            return value;
        }
        if(isAllOnes){
            return 1;
        }
        if(isAllZeros){
            return 0;
        }
        return 0;
    }
}
