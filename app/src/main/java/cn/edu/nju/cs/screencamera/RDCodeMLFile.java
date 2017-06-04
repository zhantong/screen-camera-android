package cn.edu.nju.cs.screencamera;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

/**
 * Created by zhantong on 2017/6/4.
 */

public class RDCodeMLFile {
    private static final String TAG="ShiftCodeMLFile";
    Map<DecodeHintType,?> hints;
    RDCodeMLConfig config=new RDCodeMLConfig();
    public RDCodeMLFile(String filePath,Map<DecodeHintType,?> hints){
        this.hints=hints;
        Gson gson=new Gson();
        JsonObject root=null;
        try {
            JsonParser parser=new JsonParser();
            root=(JsonObject) parser.parse(new FileReader(filePath));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        int[][] data=gson.fromJson(root.get("values"),int[][].class);
        stream(data);
    }
    public void stream(int[][] rawFrames){
        int numRSBytes=6;
        int numColors=4;
        int numRegionBytes=(config.regionWidth*config.regionHeight)/(8/(int)Math.sqrt(numColors));
        int numRegionDataBytes=numRegionBytes-numRSBytes;
        int numInterFrameParity=1;
        int numFramesPerWindow=8;
        int indexLastFrame=numFramesPerWindow-numInterFrameParity;
        int numBytesPerRegionLine=config.mainBlock.get(District.MAIN).getBitsPerUnit()*config.regionWidth/8;
        int numBytesPerRegion=numBytesPerRegionLine*config.regionHeight;
        int numRegions=config.numRegionVertical*config.numRegionHorizon;
        int numBytesPerFrame=numRegions*numBytesPerRegion;
        int indexCenterBlock=numRegions/2;
        Map<Integer,int[][][]> windows=new HashMap<>();
        for(int[] rawFrame:rawFrames){
            rawFrame=Utils.changeNumBitsPerInt(rawFrame,config.mainBlock.get(District.MAIN).getBitsPerUnit(),8);
            int[][] frame=new int[numRegions][];
            for(int indexRegionOffset=0,posOffset=0;indexRegionOffset<numRegions;indexRegionOffset+=config.numRegionHorizon,posOffset+=numBytesPerRegion*config.numRegionHorizon){
                for(int indexRegionInLine=0;indexRegionInLine<config.numRegionHorizon;indexRegionInLine++){
                    int indexRegion=indexRegionOffset+indexRegionInLine;
                    int pos=posOffset+indexRegionInLine*numBytesPerRegionLine;
                    int[] regionData=new int[numBytesPerRegion];
                    int regionDataPos=0;
                    for(int line=0;line<numBytesPerRegion;line+=numBytesPerRegionLine,pos+=numBytesPerRegionLine*config.numRegionHorizon){
                        System.arraycopy(rawFrame,pos,regionData,regionDataPos,numBytesPerRegionLine);
                        regionDataPos+=numBytesPerRegionLine;
                    }
                    try {
                        Utils.rSDecode(regionData,numRSBytes,8);
                        frame[indexRegion]=new int[numRegionDataBytes];
                        System.arraycopy(regionData,0,frame[indexRegion],0,numRegionDataBytes);
                    } catch (ReedSolomonException e) {
                        System.out.println("RS decode failed "+indexRegion);
                    }
                }
            }

            List<Integer>[] interRegionParity=new List[3];
            interRegionParity[0]=new ArrayList<>();
            for(int i=0;i<numRegions-2;i++){
                if(i!=indexCenterBlock&&((numRegions-i-3)%4<2)){
                    interRegionParity[0].add(i);
                }
            }
            interRegionParity[1]=new ArrayList<>();
            for(int i=0;i<numRegions;i++){
                if(i!=indexCenterBlock&&(i%2==numRegions%2)){
                    interRegionParity[1].add(i);
                }
            }
            interRegionParity[2]=new ArrayList<>();
            for(int i=0;i<numRegions;i++){
                if(i!=indexCenterBlock){
                    interRegionParity[2].add(i);
                }
            }

            if(frame[indexCenterBlock]==null){
                System.out.println("null center region");
            }else{
                int currentWindow=frame[indexCenterBlock][0];
                int currentFrame=frame[indexCenterBlock][1];
                System.out.println("window "+currentWindow+" frame "+currentFrame+" data:"+ Arrays.toString(frame));
                if(!windows.containsKey(currentWindow)){
                    windows.put(currentWindow,new int[8][numRegions][]);
                }
                int[][][] window=windows.get(currentWindow);
                for(int i=0;i<frame.length;i++){
                    if(frame[i]!=null) {
                        window[currentFrame][i]=frame[i];
                    }
                }
                if(currentWindow!=7) {
                    interRegionEC(interRegionParity,window,currentFrame,numRegionDataBytes);
                }else{
                    interFrameEC(window,currentFrame,currentWindow,numRegionDataBytes,interRegionParity);
                }
            }
        }
    }
    static void interRegionEC(List<Integer>[] interRegionParity,int[][][] window,int currentFrame,int numRegionDataBytes){
        for (int i = 0; i < interRegionParity.length; i++) {
            for (int j = 0; j < interRegionParity.length; j++) {
                int countErrorRegion = 0;
                int indexErrorRegion = -1;
                for (int k : interRegionParity[j]) {
                    if (window[currentFrame][k] == null) {
                        countErrorRegion++;
                        indexErrorRegion = k;
                    }
                }
                if (countErrorRegion == 1) {
                    int[] region = new int[numRegionDataBytes];
                    for (int k : interRegionParity[j]) {
                        if (k != indexErrorRegion) {
                            for (int pos = 0; pos < region.length; pos++) {
                                region[pos] ^= window[currentFrame][k][pos];
                            }
                        }
                    }
                    window[currentFrame][indexErrorRegion] = region;
                    System.out.println("inter region recover success index " + indexErrorRegion + " data:" + Arrays.toString(region));
                }
            }
        }
    }
    static void interFrameEC(int[][][] window,int currentFrame,int currentWindow,int numRegionDataBytes,List<Integer>[] interRegionParity){
        for(int i=0;i<window[currentFrame].length;i++){
            if(window[currentFrame][i]!=null){
                int countErrorRegion = 0;
                int indexErrorFrame = -1;
                for(int j=0;j<currentWindow;j++){
                    if(window[j][i]==null){
                        countErrorRegion++;
                        indexErrorFrame=j;
                    }
                }
                if(countErrorRegion==1){
                    int[] region=new int[numRegionDataBytes];
                    for(int j=0;j<currentWindow;j++){
                        if(j!=indexErrorFrame){
                            for(int pos=0;pos<region.length;pos++){
                                region[pos]^=window[j][i][pos];
                            }
                        }
                    }
                    window[indexErrorFrame][i]=region;
                    System.out.println("inter frame recover success frame " + indexErrorFrame+" region "+i + " data:" + Arrays.toString(region));
                }
            }
        }
    }
}
