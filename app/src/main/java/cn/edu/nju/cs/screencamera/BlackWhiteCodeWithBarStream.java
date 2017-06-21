package cn.edu.nju.cs.screencamera;


/**
 * Created by zhantong on 2017/5/24.
 */

public class BlackWhiteCodeWithBarStream extends BlackWhiteCodeStream{

    BarcodeConfig getBarcodeConfigInstance(){
        return new BlackWhiteCodeWithBarConfig();
    }
    BlackWhiteCode getBarcodeInstance(MediateBarcode mediateBarcode){
        return new BlackWhiteCodeWithBar(mediateBarcode);
    }
    @Override
    int[][] getContents(BlackWhiteCode blackWhiteCode){
        return ((BlackWhiteCodeWithBar)blackWhiteCode).getMixedRawContent();
    }
}
