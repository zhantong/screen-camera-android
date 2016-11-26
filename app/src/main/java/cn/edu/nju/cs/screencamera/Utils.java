package cn.edu.nju.cs.screencamera;

import java.io.File;
import java.util.BitSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.edu.nju.cs.screencamera.ReedSolomon.GenericGF;
import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonDecoder;
import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

/**
 * Created by zhantong on 2016/9/29.
 */

public final class Utils {
    public static String combinePaths(String ... paths){
        if(paths.length==0){
            return "";
        }
        File combined=new File(paths[0]);
        int i=1;
        while(i<paths.length){
            combined=new File(combined,paths[i]);
            i++;
        }
        return combined.getPath();
    }
    public static int calculateMean(int[] array,int low,int high){
        int sum=0;
        for(int i=low;i<=high;i++){
            sum+=array[i];
        }
        return sum/(high-low+1);
    }
    public static int[] extractResolution(String string){
        Pattern pattern= Pattern.compile(".*?(\\d+)x(\\d+).*");
        Matcher matcher=pattern.matcher(string);
        if(matcher.find()){
            int width=Integer.parseInt(matcher.group(1));
            int height=Integer.parseInt(matcher.group(2));
            return new int[]{width,height};
        }
        return null;
    }
    public static int bitsToInt(BitSet bitSet, int length, int offset){
        int value=0;
        for(int i=0;i<length;i++){
            value+=bitSet.get(offset+i)?(1<<i):0;
        }
        return value;
    }
    public static void crc8Check(int data,int check) throws CRCCheckException{
        CRC8 crc8=new CRC8();
        crc8.reset();
        crc8.update(data);
        int real=(int)crc8.getValue();
        if(check!=real||data<0){
            throw CRCCheckException.getNotFoundInstance();
        }
    }
    public static int[] changeNumBitsPerInt(int[] originData,int originNumBits,int newNumBits){
        return changeNumBitsPerInt(originData,0,originData.length,originNumBits,newNumBits);
    }
    public static int[] changeNumBitsPerInt(int[] data,int dataOffset,int dataLength,int originBitsPerInt,int newBitsPerInt){
        int numDataBits=dataLength*originBitsPerInt;
        int[] array=new int[(int)Math.ceil((float) numDataBits/newBitsPerInt)];
        for(int i=0;i<numDataBits;i++){
            if((data[dataOffset+i/originBitsPerInt]&(1<<(i%originBitsPerInt)))>0){
                array[i/newBitsPerInt]|=1<<(i%newBitsPerInt);
            }
        }
        return array;
    }
    public static byte[] intArrayToByteArray(int[] data,int bitsPerInt){
        return intArrayToByteArray(data,data.length,bitsPerInt);
    }
    public static byte[] intArrayToByteArray(int[] data,int dataLength,int bitsPerInt){
        int numBitsPerByte=8;
        int numDataBits=dataLength*bitsPerInt;
        byte[] array=new byte[(int)Math.ceil((float) numDataBits/numBitsPerByte)];
        for(int i=0;i<numDataBits;i++){
            if((data[i/bitsPerInt]&(1<<(i%bitsPerInt)))>0){
                array[i/numBitsPerByte]|=1<<(i%numBitsPerByte);
            }
        }
        return array;
    }
    public static void rSDecode(int[] originData,int numEc,int ecSize) throws ReedSolomonException {
        GenericGF field;
        switch (ecSize){
            case 12:
                field=GenericGF.AZTEC_DATA_12;
                break;
            default:
                field=GenericGF.QR_CODE_FIELD_256;
        }
        rSDecode(originData,numEc,field);
    }
    public static void rSDecode(int[] originData,int numEc,GenericGF field) throws ReedSolomonException {
        ReedSolomonDecoder decoder=new ReedSolomonDecoder(field);
        decoder.decode(originData,numEc);
    }
}
