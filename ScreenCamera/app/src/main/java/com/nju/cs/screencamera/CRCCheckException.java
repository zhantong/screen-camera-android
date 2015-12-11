package com.nju.cs.screencamera;

/**
 * Created by zhantong on 15/12/11.
 */
public class CRCCheckException extends Exception{
    private static final CRCCheckException INSTANCE=new CRCCheckException();
    protected static final StackTraceElement[] NO_TRACE=new StackTraceElement[0];
    static {
        INSTANCE.setStackTrace(NO_TRACE);
    }
    private CRCCheckException(){
    }
    public static CRCCheckException getNotFoundInstance(){
        return INSTANCE;
    }
}
