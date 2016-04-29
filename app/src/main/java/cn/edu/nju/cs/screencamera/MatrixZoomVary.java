package cn.edu.nju.cs.screencamera;

import android.util.Log;

import java.util.BitSet;
import java.util.List;

/**
 * Created by zhantong on 16/4/24.
 */
public class MatrixZoomVary extends Matrix{
    public MatrixZoomVary(){
        super();
        super.bitsPerBlock=2;
        super.frameBlackLength=1;
        super.frameVaryLength=1;
        super.frameVaryTwoLength=1;
        super.contentLength=40;
        super.ecNum=40;
        super.ecLength=10;
    }
    public MatrixZoomVary(byte[] pixels,int imgColorType, int imgWidth, int imgHeight,int[] initBorder) throws NotFoundException {
        super(pixels,imgColorType,imgWidth,imgHeight,initBorder);
        super.bitsPerBlock=2;
        super.frameBlackLength=1;
        super.frameVaryLength=1;
        super.frameVaryTwoLength=1;
        super.contentLength=40;
        super.ecNum=40;
        super.ecLength=10;
    }
    public void initGrayMatrix(){
        initGrayMatrix(getBarCodeWidth(),getBarCodeHeight());
    }
    public void initGrayMatrix(int dimensionX, int dimensionY){
        int samplePerBlock=5;//不能在这里设置
        grayMatrix=new GrayMatrixZoom(dimensionX,dimensionY);
        float[] points = new float[2 * dimensionX*samplePerBlock];
        int max = points.length;
        for (int y = 0; y < dimensionY; y++) {
            for (int x = 0; x < dimensionX; x ++) {
                int cur=x*samplePerBlock*2;
                points[cur] = (float) x + 0.5f;
                points[cur+1]=(float) y + 0.5f;

                points[cur+2]=(float) x + 0.5f;
                points[cur+3]=(float) y + 0.1f;

                points[cur+4]=(float) x + 0.5f;
                points[cur+5]=(float) y + 0.9f;

                points[cur+6]=(float) x + 0.1f;
                points[cur+7]=(float) y + 0.5f;

                points[cur+8]=(float) x + 0.9f;
                points[cur+9]=(float) y + 0.5f;
            }
            transform.transformPoints(points);
            for (int x = 0; x < dimensionX; x ++) {
                Point[] gray=new Point[samplePerBlock];
                int cur=x*samplePerBlock*2;
                for(int i=0;i<samplePerBlock*2;i+=2){
                    int pixel=getGray(Math.round(points[cur+i]), Math.round(points[cur+i+1]));
                    gray[i/2]=new Point(Math.round(points[cur+i]), Math.round(points[cur+i+1]),pixel);
                }
                grayMatrix.set(x,y,gray);
                //int gray = getGray(Math.round(points[x]), Math.round(points[x + 1]));
                //grayMatrix.set(x / 2, y, gray,Math.round(points[x]),Math.round(points[x + 1]));
            }
        }
        //grayMatrix.print();
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
    public BitSet getHead(){
        if(grayMatrix==null){
            initGrayMatrix();
        }
        return getRawHead();
    }
    public int mean(int[] array,int low,int high){
        int sum=0;
        for(int i=low;i<=high;i++){
            sum+=array[i];
        }
        return sum/(high-low+1);
    }
    private int maxIndex(int x,int y){
        int[] samples=grayMatrix.getSamples(x,y);
        if((x+y)%2==0){
            int maxIndex=-1;
            int max=-1;
            for(int i=1;i<5;i++){
                if(samples[i]>max){
                    maxIndex=i;
                    max=samples[i];
                }
            }
            return maxIndex;
        }
        else{
            int minIndex=-1;
            int min=1000;
            for(int i=1;i<5;i++){
                if(samples[i]<min){
                    minIndex=i;
                    min=samples[i];
                }
            }
            return minIndex;
        }
    }
    public BitSet getRawContentSimple(){
        BitSet bitSet=new BitSet();
        int index=0;
        for(int y=frameBlackLength;y<frameBlackLength+contentLength;y++){
            int blackValue=grayMatrix.get(0, y);
            int whiteValue=grayMatrix.get(0, y - 1);
            int threshold=(blackValue+whiteValue)/2;
            //if(VERBOSE){Log.d(TAG,"line black value: "+blackValue+"\twhite value: "+whiteValue);}
            for(int x=frameBlackLength+frameVaryLength+frameVaryTwoLength;x<frameBlackLength+frameVaryLength+frameVaryTwoLength+contentLength;x++){
                //int[] samples=grayMatrix.getSamples(x,y);
                //int maxIndex=maxIndex(samples,1)-1;
                int maxIndex=maxIndex(x,y)-1;
                switch (maxIndex){
                    case 0:
                        index++;
                        break;
                    case 1:
                        index++;
                        bitSet.set(index);
                        break;
                    case 2:
                        bitSet.set(index);
                        index++;
                        break;
                    case 3:
                        bitSet.set(index);
                        index++;
                        bitSet.set(index);
                }
                index++;
            }
        }
        return bitSet;
    }
    public BitSet getContent(){
        return getContent(getBarCodeWidth(),getBarCodeHeight());
    }
    public BitSet getContent(int dimensionX, int dimensionY) {
        if (grayMatrix == null) {
            initGrayMatrix(dimensionX,dimensionY);
        }
        if(VERBOSE){
            Log.d(TAG,"color reversed:"+reverse);}
        isMixed=false;
        Log.i(TAG,"frame mixed:"+isMixed);
        if(isMixed){
            return null;
        }
        else {
            return getRawContentSimple();
        }
    }
    public void check(List<Integer> points){
        int baseX=frameBlackLength+frameVaryLength+frameVaryTwoLength;
        int baseY=frameBlackLength;
        for(int point:points){
            int x=point%contentLength;
            int y=point/contentLength;
            Point[] samples=grayMatrix.getPoints(baseX+x,baseY+y);
            System.out.println("("+x+","+y+") ("+samples[1].x+","+samples[1].y+" "+samples[1].value+") ("+samples[2].x+","+samples[2].y+" "+samples[2].value+") ("+samples[3].x+","+samples[3].y+" "+samples[3].value+") ("+samples[4].x+","+samples[4].y+" "+samples[4].value+")");
        }
    }
}
