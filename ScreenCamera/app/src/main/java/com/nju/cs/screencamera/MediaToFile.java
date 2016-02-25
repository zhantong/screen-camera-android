package com.nju.cs.screencamera;

import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 从相机,视频,图片识别二维码的基础类
 * 包含通用的UI通信显示方法,和图像处理方法等
 */
public class MediaToFile extends FileToImg {
    private static final String TAG = "MediaToFile";//log tag
    private static final boolean VERBOSE = false;//是否记录详细log
    private TextView debugView;//输出处理信息的TextView
    private TextView infoView;//输出全局信息的TextView
    private Handler handler;//与UI进程通信
    int barCodeWidth = (frameBlackLength + frameVaryLength+frameVaryTwoLength) * 2 + contentLength;//二维码边长
    /**
     * 构造函数,获取必须的参数
     *
     * @param debugView 实例
     * @param infoView  实例
     * @param handler   实例
     */
    public MediaToFile(TextView debugView, TextView infoView, Handler handler) {
        this.debugView = debugView;
        this.infoView = infoView;
        this.handler = handler;
    }
    /**
     * 更新处理信息,即将此线程的信息输出到UI
     *
     * @param index            帧编号
     * @param lastSuccessIndex 最后识别成功的帧编号
     * @param frameAmount      二维码帧总数
     * @param count            已处理的帧个数
     */
    protected void updateDebug(int index, int lastSuccessIndex, int frameAmount, int count) {
        final String text = "当前:" + index + "已识别:" + lastSuccessIndex + "帧总数:" + frameAmount + "已处理:" + count;
        handler.post(new Runnable() {
            @Override
            public void run() {
                debugView.setText(text);
            }
        });
    }
    /**
     * 更新全局信息,即将此线程的信息输出到UI
     *
     * @param msg 信息
     */
    protected void updateInfo(String msg) {
        final String text = msg;
        handler.post(new Runnable() {
            @Override
            public void run() {
                infoView.setText(text);
            }
        });
    }
    public int getFileByteNum(Matrix matrix) throws CRCCheckException{
        byte[] head=matrix.getHead(barCodeWidth, barCodeWidth);
        int index=(head[0]&0xff)<<24|(head[1]&0xff)<<16|(head[2]&0xff)<<8|(head[3]&0xff);
        int crc=head[4]&0xff;
        int truth=CRC8.calcCrc8(index);
        Log.d(TAG, "CRC check: index:" + index + " CRC:" + crc + " truth:" + truth);
        Log.d(TAG, "head:");
        System.out.println("getFileByteNum:");
        for(byte b:head){
            Log.d(TAG,Byte.toString(b));
        }
        if(crc!=truth){
            throw CRCCheckException.getNotFoundInstance();
        }
        return index;
    }
    public byte[] getContent(Matrix matrix) throws ReedSolomonException{
        byte[] content=matrix.getContent(barCodeWidth, barCodeWidth);
        int[] con=new int[content.length];
        for(int i=0;i<con.length;i++){
            con[i]=content[i]&0xff;
        }
        ReedSolomonDecoder decoder = new ReedSolomonDecoder(GenericGF.QR_CODE_FIELD_256);
        decoder.decode(con, ecByteNum);
        int realByteNum=contentLength*contentLength/8-ecByteNum;
        byte[] res = new byte[realByteNum];
        for (int i = 0; i < realByteNum; i++) {
            res[i] = (byte) con[i];
        }
        return res;
    }
}
