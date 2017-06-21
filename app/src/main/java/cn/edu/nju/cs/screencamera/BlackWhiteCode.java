package cn.edu.nju.cs.screencamera;

import com.google.gson.JsonObject;

import java.util.BitSet;
import java.util.Map;

import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

/**
 * Created by zhantong on 2017/6/18.
 */

public class BlackWhiteCode {
    public static final String KEY_SIZE_RS_ERROR_CORRECTION="SIZE_RS_ERROR_CORRECTION";
    public static final String KEY_LEVEL_RS_ERROR_CORRECTION="LEVEL_RS_ERROR_CORRECTION";
    public static final String KEY_NUMBER_RAPTORQ_SOURCE_BLOCKS="NUMBER_RAPTORQ_SOURCE_BLOCKS";

    public static final int OVERLAP_CLEAR_WHITE=0;
    public static final int OVERLAP_CLEAR_BLACK=1;
    public static final int OVERLAP_BLACK_TO_WHITE=2;
    public static final int OVERLAP_WHITE_TO_BLACK=3;

    MediateBarcode mediateBarcode;
    int refWhite;
    int refBlack;
    int expand;
    int binaryThreshold;
    int overlapSituation;

    public BlackWhiteCode(MediateBarcode mediateBarcode){
        this.mediateBarcode=mediateBarcode;
        if(mediateBarcode.rawImage==null){
            return;
        }
        processBorderRight();
        processBorderLeft();
        System.out.println("refWhite: "+refWhite+" refBlack: "+refBlack+" binary expand: "+binaryThreshold+" expand: "+ expand);
        System.out.println("overlap: "+overlapSituation);
    }
    public int getOverlapSituation(){
        return overlapSituation;
    }
    public int getTransmitFileLengthInBytes() throws CRCCheckException{
        return getTransmitFileLengthInBytes(RawImage.CHANNLE_Y);
    }
    public int getTransmitFileLengthInBytes(int channel) throws CRCCheckException{
        int[] content=mediateBarcode.getContent(mediateBarcode.districts.get(Districts.BORDER).get(District.UP),channel);
        BitSet data=new BitSet();
        for(int i=0;i<content.length;i++){
            if(content[i]>binaryThreshold){
                data.set(i);
            }
        }
        int transmitFileLengthInBytes=Utils.bitsToInt(data,32,0);
        int crc=Utils.bitsToInt(data,8,32);
        Utils.crc8Check(transmitFileLengthInBytes,crc);
        return transmitFileLengthInBytes;
    }
    public void processBorderRight(){
        processBorderRight(RawImage.CHANNLE_Y);
    }
    public void processBorderRight(int channel){
        int[] content=mediateBarcode.getContent(mediateBarcode.districts.get(Districts.BORDER).get(District.RIGHT),channel);
        int sumWhite=0,sumBlack=0;
        int maxWhite=0,minWhite=255;
        int maxBlack=0,minBlack=255;
        for(int i=0;i<content.length;i+=2){
            int currentWhite=content[i];
            int currentBlack=content[i+1];
            sumWhite+=currentWhite;
            sumBlack+=currentBlack;
            if(maxWhite<currentWhite){
                maxWhite=currentWhite;
            }
            if(minWhite>currentWhite){
                minWhite=currentWhite;
            }
            if(maxBlack<currentBlack){
                maxBlack=currentBlack;
            }
            if(minBlack>currentBlack){
                minBlack=currentBlack;
            }
        }
        int numHalfPoints=content.length/2;
        refWhite=sumWhite/numHalfPoints;
        refBlack=sumBlack/numHalfPoints;
        binaryThreshold=(refWhite+refBlack)/2;
        expand =(maxWhite+maxBlack-minWhite-minBlack)/2;
    }
    public void processBorderLeft(){
        processBorderLeft(RawImage.CHANNLE_Y);
    }
    public void processBorderLeft(int channel){
        int[] content=mediateBarcode.getContent(mediateBarcode.districts.get(Districts.BORDER).get(District.LEFT),channel);
        int mixIndicatorUp=content[0];
        int mixIndicatorDown=content[content.length-1];
        int refBlackExpand=refBlack+ expand;
        int refWhiteExpand=refWhite- expand;
        if((mixIndicatorUp<=refBlackExpand)&&(mixIndicatorDown<=refBlackExpand)){
            overlapSituation=OVERLAP_CLEAR_BLACK;
        }else if((mixIndicatorUp>=refWhiteExpand)&&(mixIndicatorDown>=refWhiteExpand)){
            overlapSituation=OVERLAP_CLEAR_WHITE;
        }else if(mixIndicatorUp>mixIndicatorDown){
            overlapSituation=OVERLAP_WHITE_TO_BLACK;
        }else{
            overlapSituation=OVERLAP_BLACK_TO_WHITE;
        }
        System.out.println("mix indicator up: "+mixIndicatorUp+" mix indicator down: "+mixIndicatorDown+" refWhiteEx: "+refWhiteExpand+" refBlackEx: "+refBlackExpand);
    }
    public int[] getContent(){
        return getContent(RawImage.CHANNLE_Y);
    }

    public int[] getContent(int channel){
        Zone zone=mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN);
        BlackWhiteBlock block=(BlackWhiteBlock) zone.getBlock();
        int[] content=mediateBarcode.getContent(zone,channel);
        int[] rawData=new int[zone.widthInBlock*zone.heightInBlock];
        int step=block.getNumSamplePoints();
        int offset=0;
        int rawDataPos=0;
        for(int y=0;y<zone.heightInBlock;y++) {
            for (int x = 0; x < zone.widthInBlock; x++) {
                int value=block.getClear(content[offset],binaryThreshold);
                offset+=step;
                rawData[rawDataPos]=value;
                rawDataPos++;
            }
        }
        return rawData;
    }

    public int[] rSDecode(int[] rawContent,Zone zone) throws ReedSolomonException {
        int ecSize=Integer.parseInt(mediateBarcode.config.hints.get(KEY_SIZE_RS_ERROR_CORRECTION).toString());
        float ecLevel=Float.parseFloat(mediateBarcode.config.hints.get(KEY_LEVEL_RS_ERROR_CORRECTION).toString());
        int rawBitsPerUnit=zone.getBlock().getBitsPerUnit();
        int numRS=calcNumRS(zone,ecSize);
        int numRSEc=calcNumRSEc(numRS,ecLevel);
        int numRSData=calcNumRSData(numRS,numRSEc);
        int rSDataLengthInUnit=numRSData*ecSize/rawBitsPerUnit;
        int rSRepairLengthInUnit=numRSEc*ecSize/rawBitsPerUnit;
        int[] rsEncodedDataPart=Utils.changeNumBitsPerInt(rawContent,0,rSDataLengthInUnit,rawBitsPerUnit,ecSize);
        int[] rsEncodeRepairPart=Utils.changeNumBitsPerInt(rawContent,rSDataLengthInUnit,rSRepairLengthInUnit,rawBitsPerUnit,ecSize);
        int[] rSEncodedData=Utils.concatIntArray(rsEncodedDataPart,rsEncodeRepairPart);
        Utils.rSDecode(rSEncodedData,numRSEc,ecSize);
        return rSEncodedData;
    }
    protected static int calcNumRSEc(int numRS,float rSEcLevel){
        return (int)Math.floor(numRS*rSEcLevel);
    }
    protected static int calcNumRS(Zone zone,int rSEcSize){
        return zone.getBlock().getBitsPerUnit()*zone.widthInBlock*zone.heightInBlock/rSEcSize;
    }
    protected static int calcNumRSData(int numRS,int numRSEc){
        return numRS-numRSEc;
    }
    protected static int calcNumDataBytes(int numRSData,int rSEcSize){
        return numRSData*rSEcSize/8-8;
    }

    public int calcRaptorQPacketSize(){
        int ecSize=Integer.parseInt(mediateBarcode.config.hints.get(KEY_SIZE_RS_ERROR_CORRECTION).toString());
        float ecLevel=Float.parseFloat(mediateBarcode.config.hints.get(KEY_LEVEL_RS_ERROR_CORRECTION).toString());
        int numRS=calcNumRS(mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN),ecSize);
        int numRSEc=calcNumRSEc(numRS,ecLevel);
        int numRSData=calcNumRSData(numRS,numRSEc);
        return numRSData*ecSize/8;
    }
    public int calcRaptorQSymbolSize(int raptorQPacketSize){
        return raptorQPacketSize-8;
    }
    JsonObject toJson(){
        JsonObject root=mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN).toJson();
        root.addProperty("referenceWhite",refWhite);
        root.addProperty("referenceBlack",refBlack);
        root.addProperty("expand",expand);
        root.addProperty("binaryThreshold",binaryThreshold);
        root.addProperty("overlapSituation",overlapSituation);
        return root;
    }
}
