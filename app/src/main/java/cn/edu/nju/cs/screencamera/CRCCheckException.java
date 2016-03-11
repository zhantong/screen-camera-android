package cn.edu.nju.cs.screencamera;

/**
 * CRC校验异常类
 * CRC校验值不匹配时抛出此异常
 */
public class CRCCheckException extends Exception {
    private static final CRCCheckException INSTANCE = new CRCCheckException();
    protected static final StackTraceElement[] NO_TRACE = new StackTraceElement[0];

    static {
        INSTANCE.setStackTrace(NO_TRACE);
    }

    private CRCCheckException() {
    }

    public static CRCCheckException getNotFoundInstance() {
        return INSTANCE;
    }
}
