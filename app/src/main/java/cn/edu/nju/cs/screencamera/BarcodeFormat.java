package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 16/4/23.
 */
public enum BarcodeFormat{
    NORMAL("普通二维码"),
    ZOOM("小方块移动二维码");

    private String friendlyName;
    private BarcodeFormat(String friendlyName){
        this.friendlyName=friendlyName;
    }
    public String toString(){
        return friendlyName;
    }
}
