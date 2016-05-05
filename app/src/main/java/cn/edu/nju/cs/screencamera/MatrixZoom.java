package cn.edu.nju.cs.screencamera;

import android.util.Log;

import java.util.BitSet;
import java.util.List;

/**
 * Created by zhantong on 16/4/22.
 */
public class MatrixZoom extends Matrix{

    private final int mBitsPerBlock=2;
    private final int mFrameBlackLength=1;
    private final int mFrameVaryLength=1;
    private final int mFrameVaryTwoLength=1;
    private final int mContentLength=40;
    private final int mEcNum=40;
    private final int mEcLength=10;

    public MatrixZoom(){
        super();
        super.bitsPerBlock=mBitsPerBlock;
        super.frameBlackLength=mFrameBlackLength;
        super.frameVaryLength=mFrameVaryLength;
        super.frameVaryTwoLength=mFrameVaryTwoLength;
        super.contentLength=mContentLength;
        super.ecNum=mEcNum;
        super.ecLength=mEcLength;
    }
    public MatrixZoom(byte[] pixels,int imgColorType, int imgWidth, int imgHeight,int[] initBorder) throws NotFoundException {
        super(pixels,imgColorType,imgWidth,imgHeight,initBorder);
        super.bitsPerBlock=mBitsPerBlock;
        super.frameBlackLength=mFrameBlackLength;
        super.frameVaryLength=mFrameVaryLength;
        super.frameVaryTwoLength=mFrameVaryTwoLength;
        super.contentLength=mContentLength;
        super.ecNum=mEcNum;
        super.ecLength=mEcLength;
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
                points[cur+5]=(float) y + 0.87f;

                points[cur+6]=(float) x + 0.1f;
                points[cur+7]=(float) y + 0.5f;

                points[cur+8]=(float) x + 0.87f;
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
        int mean=mean(samples,1,4);
        int count=0;
        int[] temp=new int[4];
        int maxIndex=-1;
        for(int i=1;i<5;i++){
            temp[i-1]=-1;
            if(samples[i]>mean){
                count++;
                temp[i-1]=1;
                maxIndex=i;
            }
        }
        if(count==1){
            return maxIndex;
        }
        else if(count==2){
            if(temp[0]==1&&temp[1]==1){
                float[] points=new float[]{x+0.5f,y+0.3f,x+0.5f,y+0.7f};
                transform.transformPoints(points);
                int up=getGray(Math.round(points[0]), Math.round(points[1]));
                int down=getGray(Math.round(points[2]), Math.round(points[3]));
                if(up<down){
                    return 2;
                }
                else{
                    return 1;
                }
            }
            else if(temp[0]==1&&temp[2]==1){
                float[] points=new float[]{x+0.5f,y+0.7f,x+0.7f,y+0.5f};
                transform.transformPoints(points);
                int down=getGray(Math.round(points[0]), Math.round(points[1]));
                int right=getGray(Math.round(points[2]), Math.round(points[3]));
                if(down<right){
                    return 1;
                }
                else{
                    return 3;
                }
            }
            else if(temp[0]==1&&temp[3]==1){
                float[] points=new float[]{x+0.5f,y+0.7f,x+0.3f,y+0.5f};
                transform.transformPoints(points);
                int down=getGray(Math.round(points[0]), Math.round(points[1]));
                int left=getGray(Math.round(points[2]), Math.round(points[3]));
                if(down<left){
                    return 1;
                }
                else{
                    return 4;
                }
            }
            else if(temp[1]==1&&temp[2]==1){
                float[] points=new float[]{x+0.5f,y+0.3f,x+0.7f,y+0.5f};
                transform.transformPoints(points);
                int up=getGray(Math.round(points[0]), Math.round(points[1]));
                int right=getGray(Math.round(points[2]), Math.round(points[3]));
                if(up<right){
                    return 2;
                }
                else{
                    return 3;
                }
            }
            else if(temp[1]==1&&temp[3]==1){
                float[] points=new float[]{x+0.5f,y+0.3f,x+0.3f,y+0.5f};
                transform.transformPoints(points);
                int up=getGray(Math.round(points[0]), Math.round(points[1]));
                int left=getGray(Math.round(points[2]), Math.round(points[3]));
                if(up<left){
                    return 2;
                }
                else{
                    return 4;
                }
            }
            else if(temp[2]==1&&temp[3]==1){
                float[] points=new float[]{x+0.3f,y+0.5f,x+0.7f,y+0.5f};
                transform.transformPoints(points);
                int left=getGray(Math.round(points[0]), Math.round(points[1]));
                int right=getGray(Math.round(points[2]), Math.round(points[3]));
                if(left<right){
                    return 4;
                }
                else{
                    return 3;
                }
            }
            else{
                return 0;
            }
        }
        else{
            return 0;
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
