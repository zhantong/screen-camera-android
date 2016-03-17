package cn.edu.nju.cs.screencamera;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import cn.edu.nju.cs.screencamera.ReedSolomon.GenericGF;
import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonDecoder;
import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
    protected Handler handler;//与UI进程通信
    int barCodeWidth;
    int barCodeHeight;
    ReedSolomonDecoder decoder;
    public List<int[]> out;
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
        barCodeWidth = (frameBlackLength + frameVaryLength+frameVaryTwoLength) * 2 + contentLength;
        barCodeHeight=2*frameBlackLength + contentLength;
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
        //final String text = "当前:" + index + "已识别:" + lastSuccessIndex + "帧总数:" + frameAmount + "已处理:" + count;
        final String text = "已处理:" + count;
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
        int crcLength=8;
        int crc=0;
        for(int i=0;i<byteLength;i++){
            if(head.get(intLength+i)){
                crc|=1<<(crcLength-i-1);
            }
        }
        int truth=CRC8.calcCrc8(index);
        Log.d(TAG, "CRC check: index:" + index + " CRC:" + crc + " truth:" + truth);
        if(crc!=truth||index<0){
            throw CRCCheckException.getNotFoundInstance();
        }
        return index;
    }
    public int[] getRawContent(Matrix matrix){
        int[] firstColorX={frameBlackLength,frameBlackLength+frameVaryLength+frameVaryTwoLength+contentLength};
        int[] secondColorX={frameBlackLength+frameVaryLength,frameBlackLength+frameVaryLength+frameVaryTwoLength+contentLength+frameVaryLength};
        BitSet content=matrix.getContent(barCodeWidth, barCodeHeight,firstColorX,secondColorX,frameBlackLength,frameBlackLength+contentLength);
        int[] con=new int[contentLength*contentLength/ecLength];
        for(int i=0;i<con.length*ecLength;i++){
            if(content.get(i)){
                con[i/ecLength]|=1<<(i%ecLength);
            }
        }
        return con;
    }
    public int[] decode(int[] raw,int ecNum) throws ReedSolomonException {
        if(decoder==null){
            decoder = new ReedSolomonDecoder(GenericGF.AZTEC_DATA_10);
            //decoder = new ReedSolomonDecoder(GenericGF.DATA_MATRIX_FIELD_256);
        }
        decoder.decode(raw, ecNum);
        return raw;
    }
    public byte[] getContent(Matrix matrix) throws ReedSolomonException{
        boolean checkDecode=false;
        int[] rawContent=getRawContent(matrix);
        int[] originContent=null;
        if(checkDecode){
            originContent=new int[rawContent.length];
            System.arraycopy(rawContent, 0, originContent, 0, rawContent.length);
        }
        int[] decodedContent=decode(rawContent,ecNum);
        if(checkDecode){
            boolean checkResult=check(decodedContent,originContent);
            if(VERBOSE){Log.d(TAG,"check if decode correct:"+checkResult);}
        }
        int realByteNum=contentLength*contentLength/8-ecNum*ecLength/8;
        byte[] res=new byte[realByteNum];
        for(int i=0;i<res.length*8;i++){
            if((decodedContent[i/ecLength]&(1<<(i%ecLength)))>0){
                res[i/8]|=1<<(i%8);
            }
        }
        return res;
    }
    public boolean check(int[] con,int[] orig){
        if(VERBOSE){Log.d(TAG, "checking decoded data: data length:" + con.length);}
        String filePath=Environment.getExternalStorageDirectory() + "/test.txt";
        if(out==null){
            out= readCheckFile(filePath);
            if(VERBOSE){Log.d(TAG,"load check file success:"+filePath);}
        }
        int maxCount=-1;
        int realLength=contentLength*contentLength/ecLength-ecNum;
        for (int[] current:out){
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
                if(VERBOSE){Log.d(TAG,"wrong data:");}
                for(int i=0;i<realLength;i++){
                    if(con[i]!=current[i]){
                        if(VERBOSE){Log.d(TAG,"position: "+i+"\torigin: "+orig[i]+"\tdecoded: "+con[i]+"\ttruth: "+current[i]);}
                    }
                }
            }
            if(count>maxCount){
                maxCount=count;
            }
        }
        if(VERBOSE){Log.d(TAG,"max check correct count: "+maxCount);}
        return false;
    }
    public LinkedList<int[]> readCheckFile(String filePath){
        ObjectInputStream inputStream;
        LinkedList<int[]> d=new LinkedList<>();
        try {
            inputStream = new ObjectInputStream(new FileInputStream(filePath));
            d=(LinkedList<int[]>)inputStream.readObject();
        }catch (IOException e){
            e.printStackTrace();
        }
        catch (ClassNotFoundException e){
            e.printStackTrace();
        }
        return d;
    }
    public boolean bytesToFile(byte[] bytes,String fileName){
        if(fileName.isEmpty()){
            Log.i(TAG, "file name is empty");
            updateInfo("文件名为空！ ");
            return false;
        }
        File file = new File(Environment.getExternalStorageDirectory() + "/Download/" + fileName);
        OutputStream os;
        try {
            os = new FileOutputStream(file);
            os.write(bytes);
            os.close();
        } catch (FileNotFoundException e) {
            Log.i(TAG, "file path error, cannot create file:" + e.toString());
            return false;
        }catch (IOException e){
            Log.i(TAG, "IOException:" + e.toString());
            return false;
        }
        Log.i(TAG,"file created successfully: "+file.getAbsolutePath());
        updateInfo("文件写入成功！");
        return true;
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
