package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 16/4/23.
 */
public enum BarcodeFormat {
    SHIFT_CODE("ShiftCode"),
    SHIFT_CODE_COLOR("ShiftCodeColor"),
    SHIFT_CODE_ML("ShiftCodeML"),
    SHIFT_CODE_COLOR_ML("ShiftCodeColorML"),
    BLACK_WHITE_CODE_ML("BlackWhiteCodeML"),
    COLOR_CODE_ML("ColorCodeML"),
    BLACK_WHITE_CODE_WITH_BAR("BlackWhiteCodeWithBar"),
    RD_CODE_ML("RDCodeML"),
    BLACK_WHITE_CODE("BlackWhiteCode");
    private String friendlyName;

    BarcodeFormat(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String toString() {
        return friendlyName;
    }
}
