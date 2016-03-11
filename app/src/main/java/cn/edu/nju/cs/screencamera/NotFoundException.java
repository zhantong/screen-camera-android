package cn.edu.nju.cs.screencamera;

/**
 * 二维码未找到异常类
 * 出现解码过程中可以判定二维码无法找到,即抛出此异常
 */
public class NotFoundException extends Exception {
    private static final NotFoundException INSTANCE = new NotFoundException();
    protected static final StackTraceElement[] NO_TRACE = new StackTraceElement[0];

    static {
        INSTANCE.setStackTrace(NO_TRACE);
    }

    private NotFoundException() {
    }

    public static NotFoundException getNotFoundInstance() {
        return INSTANCE;
    }

    public NotFoundException(String msg) {
        super(msg);
    }
}
