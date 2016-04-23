package cn.edu.nju.cs.screencamera;

import android.util.Log;

import java.util.BitSet;
import java.util.HashMap;

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

    public MatrixNormal(byte[] pixels,int imgColorType, int imgWidth, int imgHeight,int[] initBorder) throws NotFoundException {
        super(pixels,imgColorType,imgWidth,imgHeight,initBorder);
        super.bitsPerBlock=1;
        super.frameBlackLength=1;
        super.frameVaryLength=1;
        super.frameVaryTwoLength=1;
        super.contentLength=80;
        super.ecNum=80;
        super.ecLength=10;
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
    public BitSet getRawHead(){
        int black=grayMatrix.get(0,0);
        grayMatrix.get(0,0);
        int white=grayMatrix.get(0,1);
        grayMatrix.get(0,1);
        int threshold=(black+white)/2;
        System.out.println("black:"+black+"\twhite:"+white+"\tthreshold:"+threshold);
        int length=(frameBlackLength+frameVaryLength+frameVaryTwoLength)*2+contentLength;
        BitSet bitSet=new BitSet();
        for(int i=0;i<length;i++){
            if(grayMatrix.get(i,0)>threshold){
                bitSet.set(i);
            }
        }
        return bitSet;
    }
    public BitSet getHead(int dimensionX, int dimensionY){
        barCodeWidth=dimensionX;
        if(grayMatrix==null){
            initGrayMatrix(dimensionX,dimensionY);
        }
        return getRawHead();
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
    public BitSet getContent(int dimensionX, int dimensionY){
        int[] firstColorX={frameBlackLength,frameBlackLength+frameVaryLength+frameVaryTwoLength+contentLength};
        int[] secondColorX={frameBlackLength+frameVaryLength,frameBlackLength+frameVaryLength+frameVaryTwoLength+contentLength+frameVaryLength};
        int topY=frameBlackLength;
        int bottomY=frameBlackLength+contentLength;
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
    public int toBinary(int x,int y,int blackValue,int whiteValue){
        Point orig=grayMatrix.getPoint(x,y);
        int value=orig.value;
        int origY=orig.y;
        int left=bars[0].get(origY);
        int right=bars[1].get(origY);
        int minDistance=10000;
        int index=-1;
        int distance=Math.abs(value-blackValue);
        if(distance<minDistance){
            minDistance=distance;
            index=0;
        }
        distance=Math.abs(value-whiteValue);
        if(distance<minDistance){
            minDistance=distance;
            index=1;
        }
        distance=Math.abs(value-left);
        if(distance<minDistance){
            minDistance=distance;
            if((ordered&&!reverse)||(!ordered&&reverse)){
                index=0;
            }else {
                index=1;
            }
        }
        distance=Math.abs(value-right);
        if(distance<minDistance){
            minDistance=distance;
            if((ordered&&!reverse)||(!ordered&&reverse)){
                index=1;
            }else {
                index=0;
            }
        }
        return index;
    }
    public HashMap<Integer,Integer>[] sampleVary(int[] firstColorX, int[] secondColorX, int topY, int bottomY){
        HashMap<Integer,Integer> firstColorMap=new HashMap<>();
        for(int x:firstColorX){
            getVary(firstColorMap,x,topY,bottomY);
        }
        HashMap<Integer,Integer> secondColorMap=new HashMap<>();
        for(int x:secondColorX){
            getVary(secondColorMap,x,topY,bottomY);
        }
        HashMap<Integer,Integer>[] colorBars=new HashMap[2];
        colorBars[0]=firstColorMap;
        colorBars[1]=secondColorMap;
        return colorBars;
    }
    public void getVary(HashMap<Integer,Integer> map,int posX,int topY,int bottomY){
        int length=bottomY-topY;
        Point[] points=new Point[length];

        int index=0;
        for(int y=topY;y<bottomY;y++){
            points[index]=grayMatrix.getPoint(posX,y);
            index++;
        }
        for(int y=points[0].y;y<=points[length-1].y;y++){
            if(!map.containsKey(y)) {
                int x = getX(points, y);
                map.put(y, getGray(x, y));
            }
        }
    }
    public int getX(Point[] points,int y){
        int i;
        for(i=0;i<points.length-1;i++){
            if(y<points[i].y){
                break;
            }
        }
        Point before=points[i-1];
        Point after=points[i];
        if(before.x==after.x){
            return before.x;
        }
        float res=(float)(y-before.y)/(after.y-before.y)*(after.x-before.x)+before.x;
        return Math.round(res);
    }
}
