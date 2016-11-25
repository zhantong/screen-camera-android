package cn.edu.nju.cs.screencamera;

import java.util.Arrays;
import java.util.BitSet;

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
    private float[] realSamplePoints=null;
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
    public float[] getRealSamplePoints(PerspectiveTransform transform){
        if(realSamplePoints==null){
            float[] standard=getStandardSamplePoints();
            realSamplePoints=Arrays.copyOf(standard,standard.length);
            transform.transformPoints(realSamplePoints);
        }
        return realSamplePoints;
    }
    public int[] getContent(PerspectiveTransform transform,RawImage rawImage){
        if(content==null){
            float[] real=getRealSamplePoints(transform);
            int numBlockSamplePoints=block.getNumSamplePoints();
            content=new int[widthInBlock*heightInBlock*numBlockSamplePoints];
            for(int i=0,pos=0;i<real.length;i+=2,pos++){
                int x=(int)real[i];
                int y=(int)real[i+1];
                int value=rawImage.getGray(x,y);
                content[pos]=value;
            }
        }
        return content;
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
}
