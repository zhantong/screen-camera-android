package cn.edu.nju.cs.screencamera;

import android.util.Pair;
import android.util.SparseIntArray;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * Created by zhantong on 2016/11/24.
 */

public class Zone {
    public int widthInBlock;
    public int heightInBlock;
    public int baseOffsetInBlockX;
    public int baseOffsetInBlockY;
    private int[] content;
    private Block block;
    private float[] standardSamplePoints=null;
    private int[] realSamplePoints=null;
    public Zone(int widthInBlock, int heightInBlock,int baseOffsetInBlockX,int baseOffsetInBlockY){
        this(widthInBlock,heightInBlock,baseOffsetInBlockX,baseOffsetInBlockY,null);
    }
    public Zone(int widthInBlock, int heightInBlock,int baseOffsetInBlockX,int baseOffsetInBlockY,Block block){
        this.widthInBlock=widthInBlock;
        this.heightInBlock=heightInBlock;
        this.baseOffsetInBlockX=baseOffsetInBlockX;
        this.baseOffsetInBlockY=baseOffsetInBlockY;
        this.block=block;
    }
    public void addBlock(Block block){
        this.block=block;
    }
    public Block getBlock(){
        return block;
    }
    public float[] getStandardSamplePoints(){
        if(standardSamplePoints==null) {
            float[] blockSamplePoints = block.getSamplePoints();
            int blockSamplePointsLength = blockSamplePoints.length;
            standardSamplePoints = new float[widthInBlock * heightInBlock * blockSamplePointsLength];
            int pos = 0;
            for (int y = 0; y < heightInBlock; y++) {
                for (int x = 0; x < widthInBlock; x++) {
                    for (int i = 0; i < blockSamplePointsLength; i += 2) {
                        float column = baseOffsetInBlockX + x + blockSamplePoints[i];
                        float row = baseOffsetInBlockY + y + blockSamplePoints[i + 1];
                        standardSamplePoints[pos] = column;
                        pos++;
                        standardSamplePoints[pos] = row;
                        pos++;
                    }
                }
            }
        }
        return standardSamplePoints;
    }
    public int[] getRealSamplePoints(PerspectiveTransform transform){
        if(realSamplePoints==null){
            float[] standard=getStandardSamplePoints();
            float[] real=Arrays.copyOf(standard,standard.length);
            transform.transformPoints(real);
            realSamplePoints=new int[real.length];
            for(int i=0;i<real.length;i++){
                realSamplePoints[i]=Math.round(real[i]);
            }
        }
        return realSamplePoints;
    }
    public int[] getContent(PerspectiveTransform transform,RawImage rawImage){
        if(content==null){
            int[] real=getRealSamplePoints(transform);
            int numBlockSamplePoints=block.getNumSamplePoints();
            content=new int[widthInBlock*heightInBlock*numBlockSamplePoints];
            for(int i=0,pos=0;i<real.length;i+=2,pos++){
                int x=real[i];
                int y=real[i+1];
                int value=rawImage.getGray(x,y);
                content[pos]=value;
            }
        }
        return content;
    }
    public void scanColumn(int x,SparseIntArray map,PerspectiveTransform transform,RawImage rawImage){
        for(int y=0;y<heightInBlock-1;y++){
            float standardXPrev=baseOffsetInBlockX+x+0.5f;
            float standardYPrev=baseOffsetInBlockY+y+0.5f;
            float[] realCoodPrev=transform.transformPoint(standardXPrev,standardYPrev);
            int realXPrev=Math.round(realCoodPrev[0]);
            int realYPrev=Math.round(realCoodPrev[1]);

            float standardXNext=baseOffsetInBlockX+x+0.5f;
            float standardYNext=baseOffsetInBlockY+y+0.5f+1;
            float[] realCoodNext=transform.transformPoint(standardXNext,standardYNext);
            int realXNext=Math.round(realCoodNext[0]);
            int realYNext=Math.round(realCoodNext[1]);

            List<Pair> line=Utils.findLine(realXPrev,realYPrev,realXNext,realYNext);
            for(Pair<Integer,Integer> pair:line){
                int column=pair.first;
                int row=pair.second;
                int grayValue=rawImage.getGray(column,row);
                map.put(row,grayValue);
            }
        }
    }
    public int startInBlockX(){
        return baseOffsetInBlockX;
    }
    public int startInBlockY(){
        return baseOffsetInBlockY;
    }
    public int endInBlockX(){
        return baseOffsetInBlockX+widthInBlock;
    }
    public int endInBlockY(){
        return baseOffsetInBlockY+heightInBlock;
    }
    public JsonObject toJson(){
        Gson gson=new Gson();
        JsonObject root= new JsonObject();
        root.addProperty("samplesPerUnit",block.getNumSamplePoints());
        root.add("real",gson.toJsonTree(realSamplePoints));
        root.add("content",gson.toJsonTree(content));
        return root;
    }
}
