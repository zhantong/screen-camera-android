package cn.edu.nju.cs.screencamera;

import java.util.BitSet;

/**
 * Created by zhantong on 16/4/27.
 */
public class RawContent {
    public int length;
    public BitSet clear;
    public BitSet wTob;
    public BitSet bTow;
    public RawContent(int length){
        this.length=length;
        clear=new BitSet();
        wTob=new BitSet();
        bTow=new BitSet();
    }
    public BitSet getRawContent(boolean reversed){
        BitSet res=(BitSet)clear.clone();
        if(reversed){
            res.or(bTow);
        }
        else{
            res.or(wTob);
        }
        return res;
    }
}
