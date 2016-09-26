package cn.edu.nju.cs.screencamera;

import android.os.Environment;

import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.encoder.DataEncoder;
import net.fec.openrq.encoder.SourceBlockEncoder;
import net.fec.openrq.parameters.FECParameters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

import cn.edu.nju.cs.screencamera.ReedSolomon.GenericGF;
import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonEncoder;

/**
 * Created by zhantong on 16/4/29.
 */
public class FileToBitSet {
    private int lastESI;
    private int bitsPerBlock;
    private int contentLength;
    private int ecNum;
    private int ecLength;
    private BitSet[] packets;
    final double REPAIR_PERCENT;
    public FileToBitSet(BarcodeFormat barcodeFormat,String filePath){
        Matrix matrix = MatrixFactory.createMatrix(barcodeFormat);
        bitsPerBlock=matrix.bitsPerBlock;
        contentLength=matrix.contentLength;
        ecNum=matrix.ecNum;
        ecLength=matrix.ecLength;
        packets=RSEncode(filePath);

        PropertiesReader propertiesReader=new PropertiesReader();
        REPAIR_PERCENT=Double.parseDouble(propertiesReader.getProperty("RaptorQ.repair.percent"));
    }
    public BitSet getPacket(int esi){
        if(esi>=0&&esi<packets.length) {
            return packets[esi];
        }
        return null;
    }
    public int packetNum(){
        return packets.length;
    }
    private List<byte[]> readFile(byte[] byteData) {


        //一个二维码实际存储的文件信息,最后的8byte为RaptorQ头部
        final int realByteLength = bitsPerBlock* contentLength * contentLength / 8 - ecNum * ecLength / 8 - 8;
        final int NUMBER_OF_SOURCE_BLOCKS=1;

        List<byte[]> buffer = new LinkedList<>();


        int fileByteNum=byteData.length;
        System.out.println(String.format("file is %d bytes", fileByteNum));
        FECParameters parameters = FECParameters.newParameters(fileByteNum, realByteLength, NUMBER_OF_SOURCE_BLOCKS);
        assert byteData != null;
        DataEncoder dataEncoder = OpenRQ.newEncoder(byteData, parameters);
        System.out.println("RaptorQ parameters: "+parameters.toString());
        for (SourceBlockEncoder sourceBlockEncoder : dataEncoder.sourceBlockIterable()) {
            System.out.println(String.format("source block %d: contains %d source symbols",
                    sourceBlockEncoder.sourceBlockNumber(), sourceBlockEncoder.numberOfSourceSymbols()));
            for (EncodingPacket encodingPacket : sourceBlockEncoder.sourcePacketsIterable()) {
                byte[] encode = encodingPacket.asArray();
                buffer.add(encode);
            }
        }
        //因RaptorQ不保证最后一个source symbol的大小为指定大小,而二维码需要指定大小的内容,所以把最后一个source symbol用repair symbol替代
        buffer.remove(buffer.size() - 1);
        SourceBlockEncoder lastSourceBlock = dataEncoder.sourceBlock(dataEncoder.numberOfSourceBlocks() - 1);
        buffer.add(lastSourceBlock.repairPacket(lastSourceBlock.numberOfSourceSymbols()).asArray());
        int repairNum = (int)(buffer.size()*REPAIR_PERCENT);
        for (int i = 1; i <= repairNum; i++) {
            for (SourceBlockEncoder sourceBlockEncoder : dataEncoder.sourceBlockIterable()) {
                EncodingPacket encodingPacket=sourceBlockEncoder.repairPacket(sourceBlockEncoder.numberOfSourceSymbols() + i);
                byte[] encode = encodingPacket.asArray();
                lastESI=encodingPacket.encodingSymbolID();
                buffer.add(encode);
            }
        }
        System.out.println(String.format("generated %d symbols (the last 1 source symbol is dropped)", buffer.size()));
        return buffer;
    }
    private Object loadObjectFromFile(String filePath){
        ObjectInputStream inputStream;
        Object d=null;
        try {
            inputStream = new ObjectInputStream(new FileInputStream(filePath));
            d = inputStream.readObject();
        }catch (ClassNotFoundException ec){
            ec.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        return d;
    }
    private BitSet[] RSEncode(String filePath) {

        File file=new File(filePath);
        byte[] byteData = null;
        try{
            byteData=fullyReadFileToBytes(file);
        }catch (IOException e){
            e.printStackTrace();
        }
        File cacheFolder=new File(Environment.getExternalStorageDirectory()+"/ScreenCameraCache");
        if(!cacheFolder.exists()){
            cacheFolder.mkdir();
        }
        String fileSha1=FileVerification.bytesToSHA1(byteData);
        String cacheFileName=fileSha1+"_"+REPAIR_PERCENT+"_"+ecNum;
        File cacheFile=new File(cacheFolder,cacheFileName);
        if(cacheFile.exists()){
            return (BitSet[])loadObjectFromFile(cacheFile.getPath());
        }

        List<byte[]> byteBuffer=readFile(byteData);
        ReedSolomonEncoder encoder = new ReedSolomonEncoder(selectRSLengthParam(ecLength));
        BitSet[] bitSets=new BitSet[lastESI+1];
        for (byte[] b : byteBuffer) {
            int[] ordered = new int[(int)Math.ceil((double)bitsPerBlock* contentLength * contentLength / ecLength)];
            for (int i = 0; i < b.length * 8; i++) {
                if ((b[i / 8] & (1 << (i % 8))) > 0) {
                    ordered[i / ecLength] |= 1 << (i % ecLength);
                }
            }
            encoder.encode(ordered, ecNum);
            BitSet current=toBitSet(ordered, ecLength,bitsPerBlock*contentLength*contentLength-ecLength*ecNum);
            int esi=extractEncodingSymbolID(getFecPayloadID(current));
            bitSets[esi]=current;
        }

        ObjectOutputStream outputStream;
        try {
            outputStream = new ObjectOutputStream(new FileOutputStream(cacheFile.getPath()));
            outputStream.writeObject(bitSets);
        }catch (IOException e){
            throw new RuntimeException();
        }

        return bitSets;
    }
    private GenericGF selectRSLengthParam(int ecLength){
        switch (ecLength){
            case 8:
                return GenericGF.QR_CODE_FIELD_256;
            case 10:
                return GenericGF.AZTEC_DATA_10;
            case 12:
                return GenericGF.AZTEC_DATA_12;
        }
        return null;
    }
    private static BitSet toBitSet(int data[],int bitNum,int numRealBits){
        int cut=(numRealBits-1)/bitNum;
        int index=0;
        BitSet bitSet=new BitSet();
        for(int j=0;j<data.length;j++){
            int current=data[j];
            if(j==cut){
                for(int i=0;i<=(numRealBits-1)%bitNum;i++){
                    if((current&(1<<i))>0){
                        bitSet.set(index);
                    }
                    index++;
                }
            }else{
                for(int i=0;i<bitNum;i++){
                    if((current&(1<<i))>0){
                        bitSet.set(index);
                    }
                    index++;
                }
            }
        }
        return bitSet;
    }
    byte[] fullyReadFileToBytes(File file) throws IOException {
        int size = (int) file.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        FileInputStream fis= new FileInputStream(file);
        try {

            int read = fis.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        }  catch (IOException e){
            throw e;
        } finally {
            fis.close();
        }
        return bytes;
    }
    public int getFecPayloadID(BitSet bitSet){
        int value=0;
        for (int i = bitSet.nextSetBit(0); i <32; i = bitSet.nextSetBit(i + 1)) {
            value|=(1<<(i%8))<<(3-i/8)*8;
        }
        return value;
    }
    public int extractSourceBlockNumber(int fecPayloadID){
        return fecPayloadID>>24;
    }
    public int extractEncodingSymbolID(int fecPayloadID){
        return fecPayloadID&0x0FFF;
    }
}
