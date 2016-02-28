package com.nju.cs.screencamera;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.BitSet;
import java.util.LinkedList;
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
    int barCodeHeight=2*frameBlackLength + contentLength;
    FileToImg fileToImg=new FileToImg();
    //ReedSolomonDecoder decoder = new ReedSolomonDecoder(GenericGF.DATA_MATRIX_FIELD_256);
    ReedSolomonDecoder decoder = new ReedSolomonDecoder(GenericGF.AZTEC_DATA_10);
    public List<int[]> out=fileToImg.reading(Environment.getExternalStorageDirectory() + "/test.txt");
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
        BitSet head=matrix.getHead(barCodeWidth, barCodeHeight);
        int intLength=32;
        int byteLength=8;
        int index=0;
        for(int i=0;i<intLength;i++){
            if(head.get(i)){
                index|=1<<(intLength-i-1);
            }
        }
        int crc=0;
        for(int i=0;i<byteLength;i++){
            if(head.get(intLength+i)){
                crc|=1<<i;
            }
        }
        int truth=CRC8.calcCrc8(index);
        Log.d(TAG, "CRC check: index:" + index + " CRC:" + crc + " truth:" + truth);
        /*
        Log.d(TAG, "head:");
        System.out.println("getFileByteNum:");
        for(byte b:head){
            Log.d(TAG,Byte.toString(b));
        }
        */
        if(crc!=truth){
            throw CRCCheckException.getNotFoundInstance();
        }
        return index;
    }
    public byte[] getContent(Matrix matrix) throws ReedSolomonException{
        int[] posX={frameBlackLength,frameBlackLength+frameVaryLength,frameBlackLength+frameVaryLength+frameVaryTwoLength+contentLength,frameBlackLength+frameVaryLength+frameVaryTwoLength+contentLength+frameVaryLength};
        BitSet content=matrix.getContent(barCodeWidth, barCodeHeight,posX,frameBlackLength,frameBlackLength+contentLength);
        int[] con=new int[contentLength*contentLength/ecLength];
        for(int i=0;i<con.length*ecLength;i++){
            if(content.get(i)){
                con[i/ecLength]|=1<<(i%ecLength);
            }
        }
        int[] orig=new int[contentLength*contentLength/ecLength];
        System.arraycopy(con, 0, orig, 0, contentLength*contentLength/ecLength);
        decoder.decode(con, ecNum);
        System.out.println("check:" + check(con,orig));
        int realByteNum=contentLength*contentLength/8-ecNum*ecLength/8;
        byte[] res=new byte[realByteNum];
        for(int i=0;i<res.length*8;i++){
            if((con[i/ecLength]&(1<<(i%ecLength)))>0){
                res[i/8]|=1<<(i%8);
            }
        }
        System.out.println("content length:"+con.length+"\tecByteNum:"+ecNum+"\treal byte num:"+realByteNum);
        return res;
    }

    public boolean check(int[] con,int[] orig){
        //System.out.println("con length:"+con.length);
        int maxCount=-1;
        int realLength=contentLength*contentLength/ecLength-ecNum;
        for (int[] current:out){
            //System.out.println("current length:"+current.length);
            int count=0;
            for(int i=0;i<realLength;i++){
                if(con[i]==current[i]){
                    count++;
                }
            }
            if(count==realLength){
                return true;
            }
            if(count>realLength-100){
                for(int i=0;i<realLength;i++){
                    if(con[i]!=current[i]){
                        System.out.println("l:"+i+"\tw:"+con[i]+"\tr:"+current[i]+"\to:"+orig[i]);
                    }
                }
            }
            if(count>maxCount){
                maxCount=count;
            }
            //System.out.println("length:"+con.length+"same:"+count);
        }
        System.out.println("max count:"+maxCount);
        return false;
    }

}
