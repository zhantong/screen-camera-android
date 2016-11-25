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
    private BitContent content;
    private Block block;
    public Zone(int widthInBlock, int heightInBlock,int baseOffsetInBlockX,int baseOffsetInBlockY,Block block){
        this(widthInBlock,heightInBlock,baseOffsetInBlockX,baseOffsetInBlockY,block,null);
    }
    public Zone(int widthInBlock, int heightInBlock,int baseOffsetInBlockX,int baseOffsetInBlockY,BitContent content){
        this(widthInBlock,heightInBlock,baseOffsetInBlockX,baseOffsetInBlockY,null,content);
    }
    public Zone(int widthInBlock, int heightInBlock,int baseOffsetInBlockX,int baseOffsetInBlockY){
        this(widthInBlock,heightInBlock,baseOffsetInBlockX,baseOffsetInBlockY,null,null);
    }
    public Zone(int widthInBlock, int heightInBlock,int baseOffsetInBlockX,int baseOffsetInBlockY,Block block,BitContent content){
        this.widthInBlock=widthInBlock;
        this.heightInBlock=heightInBlock;
        this.baseOffsetInBlockX=baseOffsetInBlockX;
        this.baseOffsetInBlockY=baseOffsetInBlockY;
        this.block=block;
        this.content=content;
    }
    public void addContent(BitContent content){
        this.content=content;
    }
    public void addBlock(Block block){
        this.block=block;
    }
    public BitSet getContent(PerspectiveTransform transform,RawImage rawImage){
        float[] blockSamplePoints=block.getSamplePoints();
        int numBlockSamplePoints=blockSamplePoints.length;
        float[] points=new float[widthInBlock*heightInBlock*numBlockSamplePoints];
        int pos=0;
        for(int y=0;y<heightInBlock;y++){
            for(int x=0;x<widthInBlock;x++){
                for(int i=0;i<numBlockSamplePoints;i+=2){
                    float column=baseOffsetInBlockX+x+blockSamplePoints[i];
                    float row=baseOffsetInBlockY+y+blockSamplePoints[i+1];
                    points[pos]=column;
                    pos++;
                    points[pos]=row;
                    pos++;
                }
            }
        }
        System.out.println("standard: "+ Arrays.toString(points));
        transform.transformPoints(points);
        System.out.println("transformed: "+ Arrays.toString(points));
        BitSet bitSet=new BitSet();
        for(int i=0,bitSetPos=0;i<points.length;i+=2,bitSetPos++){
            int x=(int)points[i];
            int y=(int)points[i+1];
            int value=rawImage.getBinary(x,y);
            if(value==1){
                bitSet.set(bitSetPos);
            }
        }
        return bitSet;
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
