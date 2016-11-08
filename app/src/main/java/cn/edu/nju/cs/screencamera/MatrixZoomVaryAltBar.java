package cn.edu.nju.cs.screencamera;

import android.util.ArrayMap;
import android.util.Pair;
import android.util.SparseIntArray;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    }
    public JsonNode getSampleDataInJSON(){
        JsonNode root=super.getSampleDataInJSON();

        SparseIntArray varyOne=new SparseIntArray();
        scanVaryBar(frameBlackLength,varyOne);
        scanVaryBar(frameBlackLength+frameVaryLength+frameVaryTwoLength+contentLength,varyOne);
        SparseIntArray varyTwo=new SparseIntArray();
        scanVaryBar(frameBlackLength+frameVaryLength,varyTwo);
        scanVaryBar(frameBlackLength+frameVaryLength+frameVaryTwoLength+contentLength+frameVaryLength,varyTwo);

        ObjectMapper mapper=new ObjectMapper();
        JsonNode reference=mapper.createObjectNode();
        ((ObjectNode)reference).set("referenceColorOne",sparseArrayToJSON(varyOne,mapper));
        ((ObjectNode)reference).set("referenceColorTwo",sparseArrayToJSON(varyTwo,mapper));
        ((ObjectNode)root).set("reference",reference);
        return root;
    }
    private static JsonNode sparseArrayToJSON(SparseIntArray sparseIntArray,ObjectMapper mapper){
        JsonNode root=mapper.createObjectNode();
        ArrayNode keys=mapper.createArrayNode();
        ArrayNode values=mapper.createArrayNode();
        for(int i=0;i<sparseIntArray.size();i++){
            keys.add(IntNode.valueOf(sparseIntArray.keyAt(i)));
            values.add(IntNode.valueOf(sparseIntArray.valueAt(i)));
        }
        ((ObjectNode)root).set("keys",keys);
        ((ObjectNode)root).set("values",values);
        return root;
    }
    private void scanVaryBar(int columnX,SparseIntArray map){
        SparseIntArray column=scanColumn(columnX,frameBlackLength,frameBlackLength+contentLength);
        for(int i=0;i<column.size();i++){
            int y=column.keyAt(i);
            int x=column.get(y);
            int grayValue=getGray(x,y);
            map.put(y,grayValue);
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
            List<Pair> line=findLine(xPrev,yPrev,xNext,yNext);
            for(Pair pair:line){
                array.put((int)pair.second,(int)pair.first);
            }
        }
        return array;
    }
    private List<Pair> findLine(int x0,int y0,int x1,int y1){
        List<Pair> line=new ArrayList<>();
        int dx=Math.abs(x1-x0);
        int dy=Math.abs(y1-y0);

        int sx=x0<x1?1:-1;
        int sy=y0<y1?1:-1;

        int err=dx-dy;
        int e2;
        int currentX=x0;
        int currentY=y0;

        while(true){
            line.add(new Pair(currentX,currentY));

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
