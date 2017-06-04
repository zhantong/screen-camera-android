package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 16/4/23.
 */
public enum BarcodeFormat{
    SHIFTCODE("ShiftCode"),
    SHIFTCODECOLOR("ShiftCodeColor"),
    SHIFTCODEML("ShiftCodeML"),
    SHIFTCODECOLORML("ShiftCodeColorML"),
    BLACKWHITECODEML("BlackWhiteCodeML"),
    COLORCODEML("ColorCodeML"),
    BLACKWHITECODE("BlackWhiteCode"),
    RDCODEML("RDCodeML");
    private String friendlyName;
    private BarcodeFormat(String friendlyName){
        this.friendlyName=friendlyName;
    }
    public String toString(){
        return friendlyName;
    }
}
