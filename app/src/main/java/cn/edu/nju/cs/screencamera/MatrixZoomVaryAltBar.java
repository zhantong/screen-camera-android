package cn.edu.nju.cs.screencamera;

import android.util.SparseIntArray;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhantong on 2016/11/5.
 */

public class MatrixZoomVaryAltBar extends MatrixZoomVaryAlt{
    private final int mBitsPerBlock=2;
    private final int mFrameBlackLength=1;
    private final int mFrameVaryLength=1;
    private final int mFrameVaryTwoLength=1;
    private final int mContentLength=40;
    private final int mEcLength=12;
    private final double mEcLevel=0.1;
    private final int mEcNum=calcEcNum(mEcLevel);
    public MatrixZoomVaryAltBar(){
        super();
        super.bitsPerBlock=mBitsPerBlock;
        super.frameBlackLength=mFrameBlackLength;
        super.frameVaryLength=mFrameVaryLength;
        super.frameVaryTwoLength=mFrameVaryTwoLength;
        super.contentLength=mContentLength;
        super.ecNum=mEcNum;
        super.ecLength=mEcLength;
    }
    public MatrixZoomVaryAltBar(byte[] pixels,int imgColorType, int imgWidth, int imgHeight,int[] initBorder) throws NotFoundException {
        super(pixels,imgColorType,imgWidth,imgHeight,initBorder);
        super.bitsPerBlock=mBitsPerBlock;
        super.frameBlackLength=mFrameBlackLength;
        super.frameVaryLength=mFrameVaryLength;
        super.frameVaryTwoLength=mFrameVaryTwoLength;
        super.contentLength=mContentLength;
        super.ecNum=mEcNum;
        super.ecLength=mEcLength;
    }
    private int calcEcNum(double ecLevel){
        return ((int)((mBitsPerBlock*mContentLength*mContentLength/mEcLength)*ecLevel))/2*2;
    }
    public void initGrayMatrix(int dimensionX, int dimensionY){
        super.initGrayMatrix(dimensionX,dimensionY);
        int x=frameBlackLength;
        for(int y=frameBlackLength;y<frameBlackLength+contentLength;y++){
            Point[] points=grayMatrix.getPoints(x,y);
            points[0].print();
        }
        SparseIntArray line=scanColumn(frameBlackLength,frameBlackLength,frameBlackLength+contentLength);
        for(int i=0;i<line.size();i++){
            int key=line.keyAt(i);
            int value=line.get(key);
            int pixel=getGray(key,value);
            System.out.println("out:"+key+" "+value+" "+pixel);
        }
    }
    private SparseIntArray scanColumn(int x,int yStart,int yEnd){
        SparseIntArray array=new SparseIntArray();
        for(int y=yStart;y<yEnd;y++){
            Point pointPrev=grayMatrix.getPoints(x,y)[0];
            Point pointNext=grayMatrix.getPoints(x,y+1)[0];
            int xPrev=pointPrev.x;
            int yPrev=pointPrev.y;
            int xNext=pointNext.x;
            int yNext=pointNext.y;
            SparseIntArray line=findLine(xPrev,yPrev,xNext,yNext);
            for(int i=0;i<line.size();i++){
                int key=line.keyAt(i);
                int value=line.get(key);
                array.put(key,value);
            }
        }
        return array;
    }
    private SparseIntArray findLine(int x0,int y0,int x1,int y1){
        SparseIntArray line=new SparseIntArray();
        int dx=Math.abs(x1-x0);
        int dy=Math.abs(y1-y0);

        int sx=x0<x1?1:-1;
        int sy=y0<y1?1:-1;

        int err=dx-dy;
        int e2;
        int currentX=x0;
        int currentY=y0;

        while(true){
            line.append(currentX,currentY);

            if(currentX==x1&&currentY==y1){
                break;
            }
            e2=2*err;
            if(e2>-1*dy){
                err-=dy;
                currentX+=sx;
            }
            if(e2<dx){
                err+=dx;
                currentY+=sy;
            }
        }
        return line;
    }
}
