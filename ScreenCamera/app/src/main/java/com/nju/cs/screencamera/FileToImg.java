package com.nju.cs.screencamera;


import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.encoder.DataEncoder;
import net.fec.openrq.encoder.SourceBlockEncoder;
import net.fec.openrq.parameters.FECParameters;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * 此类目前仅作为与JAVA版本生成二维码参数作同步用
 * 主要供ImgToFile类继承,获得二维码布局等信息
 */
public class FileToImg {
    int frameWhiteLength=10;
    int frameBlackLength=1;
    int frameVaryLength=1;
    int frameVaryTwoLength=1;
    int contentLength=44;
    int blockLength=4;
    int ecByteNum=42;
    int fileByteNum;
    public List<int[]> readFile(String filePath){
        List<byte[]> buffer=new LinkedList<>();
        List<int[]> out=new LinkedList<>();
        File file = new File(filePath);
        int size = (int) file.length();
        byte[] byteData = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(byteData, 0, byteData.length);
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileByteNum=byteData.length;
        System.out.println("file byte number:"+fileByteNum);
        int length=contentLength*contentLength/8-ecByteNum-8;
        FECParameters parameters= FECParameters.newParameters(byteData.length, length, byteData.length / (length * 10) + 1);
        System.out.println(parameters.toString());
        System.out.println("length:"+byteData.length+"\tblock length:"+length+"\tblocks:"+parameters.numberOfSourceBlocks());
        DataEncoder dataEncoder= OpenRQ.newEncoder(byteData, parameters);
        int count=0;
        for(SourceBlockEncoder sourceBlockEncoder:dataEncoder.sourceBlockIterable()){
            for(EncodingPacket encodingPacket:sourceBlockEncoder.sourcePacketsIterable()){
                byte[] encode=encodingPacket.asArray();
                buffer.add(encode);
                System.out.println("packet length:"+encode.length);
            }
            System.out.println(++count);
        }
        buffer.remove(buffer.size()-1);
        buffer.add(dataEncoder.sourceBlock(dataEncoder.numberOfSourceBlocks()-1).repairPacket(dataEncoder.sourceBlock(dataEncoder.numberOfSourceBlocks()-1).numberOfSourceSymbols()).asArray());
        for(int i=1;i<=5;i++){
            for(SourceBlockEncoder sourceBlockEncoder:dataEncoder.sourceBlockIterable()){
                byte[] encode=sourceBlockEncoder.repairPacket(sourceBlockEncoder.numberOfSourceSymbols()+i).asArray();
                buffer.add(encode);
                System.out.println("packet length:"+encode.length);
            }
        }
        ReedSolomonEncoder encoder=new ReedSolomonEncoder(GenericGF.QR_CODE_FIELD_256);
        StringBuffer stringBuffer=new StringBuffer();
        for(byte[] b:buffer){
            int[] c=new int[contentLength*contentLength/8];
            for(int i=0;i<b.length;i++){
                c[i]=b[i]&0xff;
            }
            encoder.encode(c,ecByteNum);
            out.add(c);
        }
        return out;
    }
    public LinkedList<int[]> reading(String filePath){
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
}
