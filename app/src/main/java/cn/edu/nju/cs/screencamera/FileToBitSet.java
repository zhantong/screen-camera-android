package cn.edu.nju.cs.screencamera;

import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.encoder.DataEncoder;
import net.fec.openrq.encoder.SourceBlockEncoder;
import net.fec.openrq.parameters.FECParameters;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

import cn.edu.nju.cs.screencamera.ReedSolomon.GenericGF;
import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonEncoder;

/**
 * Created by zhantong on 16/4/29.
 */
public class FileToBitSet {
    private int bitsPerBlock;
    private int contentBlock;
    private int ecSymbol;
    private int ecSymbolBitLength;
    private int lastESI;
    private BitSet[] packets;
    public FileToBitSet(BarcodeFormat barcodeFormat,String filePath){
        Matrix matrix = MatrixFactory.createMatrix(barcodeFormat);
        bitsPerBlock=matrix.bitsPerBlock;
        contentBlock=matrix.contentLength;
        ecSymbol=matrix.ecNum;
        ecSymbolBitLength=matrix.ecLength;
        packets=RSEncode(readFile(filePath));
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
    private List<byte[]> readFile(String filePath) {
        //一个二维码实际存储的文件信息,最后的8byte为RaptorQ头部
        final int realByteLength = bitsPerBlock*contentBlock * contentBlock / 8 - ecSymbol * ecSymbolBitLength / 8 - 8;
        List<byte[]> buffer = new LinkedList<>();
        File file=new File(filePath);
        byte[] byteData = null;
        try{
            byteData=fullyReadFileToBytes(file);
        }catch (IOException e){
            e.printStackTrace();
        }
        int fileByteNum=byteData.length;
        System.out.println(String.format("file is %d bytes", fileByteNum));
        FECParameters parameters = FECParameters.newParameters(fileByteNum, realByteLength, 1);//只有1个source block
        assert byteData != null;
        DataEncoder dataEncoder = OpenRQ.newEncoder(byteData, parameters);
        System.out.println(String.format("RaptorQ: total %d bytes; %d source blocks; %d bytes per frame",
                parameters.dataLength(), dataEncoder.numberOfSourceBlocks(), parameters.symbolSize()));
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
        int repairNum = buffer.size() / 2;
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
    private BitSet[] RSEncode(List<byte[]> byteBuffer) {
        ReedSolomonEncoder encoder = new ReedSolomonEncoder(GenericGF.AZTEC_DATA_12);
        BitSet[] bitSets=new BitSet[lastESI+1];
        for (byte[] b : byteBuffer) {
            int[] ordered = new int[(int)Math.ceil((double)bitsPerBlock*contentBlock * contentBlock / ecSymbolBitLength)];
            for (int i = 0; i < b.length * 8; i++) {
                if ((b[i / 8] & (1 << (i % 8))) > 0) {
                    ordered[i / ecSymbolBitLength] |= 1 << (i % ecSymbolBitLength);
                }
            }
            encoder.encode(ordered, ecSymbol);
            BitSet current=toBitSet(ordered, ecSymbolBitLength,bitsPerBlock*contentBlock*contentBlock-ecSymbol*ecSymbolBitLength);
            int esi=extractEncodingSymbolID(getFecPayloadID(current));
            bitSets[esi]=current;
        }
        return bitSets;
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
