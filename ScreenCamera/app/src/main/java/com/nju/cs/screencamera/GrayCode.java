package com.nju.cs.screencamera;

/**
 * Created by zhantong on 15/11/30.
 */
public class GrayCode {
    private String pad;
    public GrayCode(int length){
        pad=String.format("%0"+length+'d',0);
    }
    public String toGray(int x){
        int gray=x^(x>>1);
        String s=Integer.toBinaryString(gray);
        return pad.substring(s.length())+s;
    }
    public static int toInt(String gray){
        int intGray=Integer.parseInt(gray,2);
        int result=intGray;
        while((intGray>>=1)!=0){
            result^=intGray;
        }
        return result;
    }
}
