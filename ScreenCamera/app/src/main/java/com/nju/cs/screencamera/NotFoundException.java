package com.nju.cs.screencamera;

/**
 * Created by zhantong on 15/11/15.
 */
public class NotFoundException extends Exception{
    private static final NotFoundException INSTANCE=new NotFoundException();
    protected static final StackTraceElement[] NO_TRACE=new StackTraceElement[0];
    static {
        INSTANCE.setStackTrace(NO_TRACE);
    }
    private NotFoundException(){
    }
    public static NotFoundException getNotFoundInstance(){
        return INSTANCE;
    }
}
