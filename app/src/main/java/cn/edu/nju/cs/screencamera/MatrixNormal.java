package cn.edu.nju.cs.screencamera;

import android.util.Log;

import java.util.BitSet;

/**
 * Created by zhantong on 16/4/22.
 */
public class MatrixNormal extends Matrix {
    public MatrixNormal(int dimension) {
        super(dimension);
    }

    public MatrixNormal(int imgWidth, int imgHeight) {
        super(imgWidth,imgHeight);
    }

    public MatrixNormal(byte[] pixels, int imgWidth, int imgHeight,int[] initBorder) throws NotFoundException {
        super(pixels,imgWidth,imgHeight,initBorder);
    }
    public void initGrayMatrix(int dimensionX, int dimensionY){
        grayMatrix=new GrayMatrixNormal(dimensionX,dimensionY);
        float[] points = new float[2 * dimensionX];
        int max = points.length;
        for (int y = 0; y < dimensionY; y++) {
            float iValue = (float) y + 0.5f;
            for (int x = 0; x < max; x += 2) {
                points[x] = (float) (x / 2) + 0.5f;
                points[x + 1] = iValue;
            }
            transform.transformPoints(points);
            for (int x = 0; x < max; x += 2) {
                int gray = getGray(Math.round(points[x]), Math.round(points[x + 1]));
                grayMatrix.set(x / 2, y, gray,Math.round(points[x]),Math.round(points[x + 1]));
            }
        }
    }
    public boolean isMixed(int dimensionX,int dimensionY,int[] posX,int topY,int bottomY){
        if(grayMatrix==null){
            initGrayMatrix(dimensionX,dimensionY);
        }
        int threshold=35;
        for(int x:posX){
            int sub=minSub(x,topY,bottomY);
            //System.out.println("img color bar sub:"+sub);
            if(sub>threshold){
                return true;
            }
        }
        return false;
    }
    public int minSub(int x,int topY,int bottomY){
        int max=-1;
        int min=256;
        for(int y=topY;y<bottomY;y++){
            int current=grayMatrix.get(x,y);
            if(current>max){
                max=current;
            }
            else if(current<min){
                min=current;
            }
        }
        return max-min;
    }
    public BitSet getRawContent(){
        if(grayMatrix.get(1,2)>grayMatrix.get(barCodeWidth - 2, 2)){
            ordered =false;
        }
        int index=0;
        BitSet bitSet=new BitSet();
        for(int y=frameBlackLength;y<frameBlackLength+contentLength;y++){
            int blackValue=grayMatrix.get(0, y);
            int whiteValue=grayMatrix.get(0, y - 1);
            if(blackValue>=whiteValue){
                int temp=blackValue;
                blackValue=whiteValue;
                whiteValue=temp;
            }
            if(VERBOSE){
                Log.d(TAG,"line black value: "+blackValue+"\twhite value: "+whiteValue);}
            for(int x=frameBlackLength+frameVaryLength+frameVaryTwoLength;x<frameBlackLength+frameVaryLength+frameVaryTwoLength+contentLength;x++){
                if(VERBOSE){Log.d(TAG,"point ("+x+" "+y+") value:"+grayMatrix.get(x, y)+"\torigin ("+grayMatrix.pixels[y * barCodeWidth + x].x+" "+grayMatrix.pixels[y * barCodeWidth + x].y+") value:"+grayMatrix.pixels[y * barCodeWidth + x].value);}
                if(toBinary(x, y, blackValue, whiteValue)==1){
                    bitSet.set(index);
                }
                index++;
            }
        }
        return bitSet;
    }
    public BitSet getRawContentSimple(){
        BitSet bitSet=new BitSet();
        int index=0;
        for(int y=frameBlackLength;y<frameBlackLength+contentLength;y++){
            int blackValue=grayMatrix.get(0, y);
            int whiteValue=grayMatrix.get(0, y - 1);
            int threshold=(blackValue+whiteValue)/2;
            if(VERBOSE){Log.d(TAG,"line black value: "+blackValue+"\twhite value: "+whiteValue);}
            for(int x=frameBlackLength+frameVaryLength+frameVaryTwoLength;x<frameBlackLength+frameVaryLength+frameVaryTwoLength+contentLength;x++){
                if(grayMatrix.get(x,y)>=threshold){
                    bitSet.set(index);
                }
                index++;
            }
        }
        return bitSet;
    }
    public BitSet getContent(int dimensionX, int dimensionY,int[] firstColorX,int[] secondColorX,int topY,int bottomY){
        barCodeWidth=dimensionX;
        if (grayMatrix == null) {
            initGrayMatrix(dimensionX,dimensionY);
            isMixed=isMixed(dimensionX,dimensionY,new int[]{firstColorX[0],secondColorX[0]},topY,bottomY);
            Log.i(TAG,"frame mixed:"+isMixed);
        }
        if(VERBOSE){Log.d(TAG,"color reversed:"+reverse);}
        if(isMixed){
            if(bars==null){
                bars=sampleVary(firstColorX,secondColorX,topY,bottomY);
            }
            return getRawContent();
        }
        else {
            return getRawContentSimple();
        }
    }
}
