package cn.edu.nju.cs.screencamera;

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
    public BitContent getContent(){
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
