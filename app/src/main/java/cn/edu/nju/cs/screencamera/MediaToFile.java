package cn.edu.nju.cs.screencamera;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.BitSet;

/**
 * 从相机,视频,图片识别二维码的基础类
 * 包含通用的UI通信显示方法,和图像处理方法等
 */
public class MediaToFile{
    private static final String TAG = "MediaToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log
    protected Handler handler;//与UI进程通信
    private CRC8 crcCheck;
    /**
     * 构造函数,获取必须的参数
     *
     * @param handler   实例
     */
    public MediaToFile(Handler handler) {
        this.handler = handler;
        crcCheck=new CRC8();
    }
    /**
     * 更新处理信息,即将此线程的信息输出到UI
     *
     * @param lastSuccessIndex 最后识别成功的帧编号
     * @param frameAmount      二维码帧总数
     * @param count            已处理的帧个数
     */
    protected void updateDebug(int lastSuccessIndex, int frameAmount, int count) {
        //final String text = "当前:" + index + "已识别:" + lastSuccessIndex + "帧总数:" + frameAmount + "已处理:" + count;
        String text = "已处理:" + count;
        Message msg=handler.obtainMessage();
        msg.what=MainActivity.MESSAGE_UI_DEBUG_VIEW;
        msg.obj=text;
        handler.sendMessage(msg);
    }
    /**
     * 更新全局信息,即将此线程的信息输出到UI
     *
     * @param text 信息
     */
    protected void updateInfo(String text) {
        Message msg=handler.obtainMessage();
        msg.what=MainActivity.MESSAGE_UI_INFO_VIEW;
        msg.obj=text;
        handler.sendMessage(msg);
    }
    public int getFileByteNum(Matrix matrix) throws CRCCheckException{
        BitSet head=matrix.getHead();
        int intLength=32;
        int byteLength=8;
        int index=0;
        for(int i=0;i<intLength;i++){
            if(head.get(i)){
                index|=1<<(intLength-i-1);
            }
        }
        int crcLength=8;
        int crc=0;
        for(int i=0;i<byteLength;i++){
            if(head.get(intLength+i)){
                crc|=1<<(crcLength-i-1);
            }
        }
        crcCheck.reset();
        crcCheck.update(index);
        int truth=(int)crcCheck.getValue();
        Log.d(TAG, "CRC check: index:" + index + " CRC:" + crc + " truth:" + truth);
        if(crc!=truth||index<0){
            throw CRCCheckException.getNotFoundInstance();
        }
        return index;
    }

    public int[] smallBorder(int[] origBorder){
        int[] border=new int[origBorder.length];
        int horizonSub=origBorder[2]-origBorder[0];
        int verticalSub=origBorder[3]-origBorder[1];
        border[0]=origBorder[0]+horizonSub/10;
        border[1]=origBorder[1]+verticalSub/10;
        border[2]=origBorder[2]-horizonSub/10;
        border[3]=origBorder[3]-verticalSub/10;
        return border;
    }
}
