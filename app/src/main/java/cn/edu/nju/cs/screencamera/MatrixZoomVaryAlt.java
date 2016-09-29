package cn.edu.nju.cs.screencamera;

import android.util.Log;

import java.util.Arrays;
import java.util.BitSet;

/**
 * Created by zhantong on 16/5/5.
 */
public class MatrixZoomVaryAlt extends Matrix {
    private RawContent rawContent;

    private final int mBitsPerBlock=2;
    private final int mFrameBlackLength=1;
    private final int mFrameVaryLength=0;
    private final int mFrameVaryTwoLength=0;
    private final int mContentLength=40;
    private final int mEcLength=12;
    private final double mEcLevel=0.1;
    private final int mEcNum=calcEcNum(mEcLevel);

    private static final boolean OVERLAP_WHITE_TO_BLACK=false;
    private static final boolean OVERLAP_BLACK_TO_WHITE=true;

    private int grayThreshold;
    private int refWhite;
    private int refBlack;

    private boolean overlapCon;
    private boolean isWhiteBackground;

    enum Overlap{
        UP,
        DOWN,
        LEFT,
        RIGHT,
        UTOD,
        UTOL,
        UTOR,
        DTOU,
        DTOL,
        DTOR,
        LTOU,
        LTOD,
        LTOR,
        RTOU,
        RTOD,
        RTOL
    }
    private int calcEcNum(double ecLevel){
        return ((int)((mBitsPerBlock*mContentLength*mContentLength/mEcLength)*ecLevel))/2*2;
    }
    public MatrixZoomVaryAlt(){
        super();
        super.bitsPerBlock=mBitsPerBlock;
        super.frameBlackLength=mFrameBlackLength;
        super.frameVaryLength=mFrameVaryLength;
        super.frameVaryTwoLength=mFrameVaryTwoLength;
        super.contentLength=mContentLength;
        super.ecNum=mEcNum;
        super.ecLength=mEcLength;
    }
    public MatrixZoomVaryAlt(byte[] pixels,int imgColorType, int imgWidth, int imgHeight,int[] initBorder) throws NotFoundException {
        super(pixels,imgColorType,imgWidth,imgHeight,initBorder);
        super.bitsPerBlock=mBitsPerBlock;
        super.frameBlackLength=mFrameBlackLength;
        super.frameVaryLength=mFrameVaryLength;
        super.frameVaryTwoLength=mFrameVaryTwoLength;
        super.contentLength=mContentLength;
        super.ecNum=mEcNum;
        super.ecLength=mEcLength;
    }
    public RawContent getRaw(){
        return rawContent;
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
            }
        }
        refWhite=getRefWhite();
        refBlack=getRefBlack();
        grayThreshold=getGrayThreshold();
        //grayThreshold=6;
        Log.i(TAG,"allowance in gray scale is "+grayThreshold);
        //isMixed=isMixed();
        isMixed=true;
    }
    private int getRefWhite(){
        int sum=0;
        for(int i=1;i<contentLength;i+=2){
            sum+=grayMatrix.get(frameBlackLength+contentLength,i);
        }
        return sum/contentLength;
    }
    private int getRefBlack(){
        int sum=0;
        for(int i=2;i<contentLength;i+=2){
            sum+=grayMatrix.get(frameBlackLength+contentLength,i);
        }
        return sum/contentLength;
    }
    private int getGrayThreshold(){
        int numBlock=15;
        int max=0;
        int min=255;
        for(int i=1;i<1+numBlock*2;i+=2){
            int grayScale=grayMatrix.get(frameBlackLength+contentLength,i);
            //System.out.println(grayScale);
            if(grayScale>max){
                max=grayScale;
            }
            if(grayScale<min){
                min=grayScale;
            }
        }
        int thresholdWhite=max-min;
        max=0;
        min=255;
        for(int i=2;i<1+numBlock*2;i+=2){
            int grayScale=grayMatrix.get(frameBlackLength+contentLength,i);
            if(grayScale>max){
                max=grayScale;
            }
            if(grayScale<min){
                min=grayScale;
            }
        }
        int thresholdBlack=max-min;
        int threshold=(thresholdBlack+thresholdWhite)/2;
        /*
        if(threshold>100){
            for(int i=1;i<1+numBlock*2;i+=2) {
                Point center=grayMatrix.getPoints(frameBlackLength + contentLength, i)[0];
                System.out.println(center.x+"\t"+center.y+"\t"+center.value);
            }
        }
        */
        return threshold;
    }
    private boolean isMixed(){
        int checkPointOne=grayMatrix.get(0,1);
        int checkPointTwo=grayMatrix.get(0,contentLength);
        if(VERBOSE){Log.d(TAG,"white:"+refWhite+"\tblack:"+refBlack+"\tone:"+checkPointOne+"\ttwo:"+checkPointTwo);}
        if(((checkPointOne>refWhite-grayThreshold)&&(checkPointTwo>refWhite-grayThreshold))||((checkPointOne<refBlack+grayThreshold)&&(checkPointTwo<refBlack+grayThreshold))){
            if(checkPointOne>(refWhite+refBlack)/2){
                isWhiteBackground=true;
            }else{
                isWhiteBackground=false;
            }
            return false;
        }

        if(checkPointOne>checkPointTwo){
            overlapCon=OVERLAP_WHITE_TO_BLACK;
        }else{
            overlapCon=OVERLAP_BLACK_TO_WHITE;
        }
        Log.d(TAG,"reverse:"+reverse);
        return true;
    }
    public BitSet getHead(){
        if(grayMatrix==null){
            initGrayMatrix();
        }
        return getRawHead();
    }
    public BitSet getRawHead(){
        int black=grayMatrix.get(0,0);
        grayMatrix.get(0,0);
        int white=grayMatrix.get(0,1);
        grayMatrix.get(0,1);
        int threshold=(black+white)/2;
        if(VERBOSE){Log.d(TAG,"black:"+black+"\twhite:"+white+"\tthreshold:"+threshold);}
        int length=(frameBlackLength+frameVaryLength+frameVaryTwoLength)*2+contentLength;
        BitSet bitSet=new BitSet();
        for(int i=0;i<length;i++){
            if(grayMatrix.get(i,0)>threshold){
                bitSet.set(i);
            }
        }
        return bitSet;
    }
    public BitSet getContent(){
        if(rawContent==null){
            sampleContent();
        }
        if(reverse){
            return rawContent.getRawContent(true);
        }
        else{
            return rawContent.getRawContent(false);
        }
    }
    public BitSet getOverlapSituation(){
        return rawContent.getOverlapSituation();
    }
    public void sampleContent(){
        if (grayMatrix == null) {
            initGrayMatrix(getBarCodeWidth(),getBarCodeHeight());
        }
        rawContent=new RawContent(bitsPerBlock*contentLength*contentLength);
        if(VERBOSE){Log.d(TAG,"color reversed:"+reverse);}
        if(isMixed){
            rawContent.isMixed=true;
            getRawContent();
        }
        else {
            getRawContentSimple();
        }
    }
    private Overlap maxIndex(int x,int y){
        int[] samples=grayMatrix.getSamples(x,y);
        int index;
        int maxIndex=-1;
        int minIndex=-1;
        int max=-1;
        int min=256;
        for(int i=1;i<5;i++){
            int current=samples[i];
            if(current>max){
                maxIndex=i;
                max=current;
            }
            if(current<min){
                minIndex=i;
                min=current;
            }
        }
        if((x+y)%2==0){
            if(isWhiteBackground){
                index=minIndex;
            }else{
                index=maxIndex;
            }
        }else{
            if(isWhiteBackground){
                index=maxIndex;
            }else{
                index=minIndex;
            }
        }
        switch (index){
            case 1:
                return Overlap.UP;
            case 2:
                return Overlap.DOWN;
            case 3:
                return Overlap.LEFT;
            case 4:
                return Overlap.RIGHT;
        }
        return Overlap.UP;
    }
    public void getRawContentSimple(){
        int index=0;
        for(int y=frameBlackLength;y<frameBlackLength+contentLength;y++){
            for(int x=frameBlackLength+frameVaryLength+frameVaryTwoLength;x<frameBlackLength+frameVaryLength+frameVaryTwoLength+contentLength;x++){
                switch (maxIndex(x,y)){
                    case UP:
                        index++;
                        break;
                    case DOWN:
                        index++;
                        rawContent.clear.set(index);
                        break;
                    case LEFT:
                        rawContent.clear.set(index);
                        index++;
                        break;
                    case RIGHT:
                        rawContent.clear.set(index);
                        index++;
                        rawContent.clear.set(index);
                }
                index++;
            }
        }

    }
    public void getRawContent(){
        int index=0;

        for(int y=frameBlackLength;y<frameBlackLength+contentLength;y++){
            for(int x=frameBlackLength+frameVaryLength+frameVaryTwoLength;x<frameBlackLength+frameVaryLength+frameVaryTwoLength+contentLength;x++){
                Overlap overlap=getOverlapSituation(x,y);
                //System.out.println("("+x+","+y+") overlap:"+overlap.name());
                switch (overlap) {
                    case UP://00
                        rawContent.clearTag.set(index);
                        index++;
                        rawContent.clearTag.set(index);
                        break;
                    case DOWN://01
                        rawContent.clearTag.set(index);
                        index++;
                        rawContent.clear.set(index);
                        rawContent.clearTag.set(index);
                        break;
                    case LEFT://10
                        rawContent.clearTag.set(index);
                        rawContent.clear.set(index);
                        index++;
                        rawContent.clearTag.set(index);
                        break;
                    case RIGHT://11
                        rawContent.clearTag.set(index);
                        rawContent.clear.set(index);
                        index++;
                        rawContent.clear.set(index);
                        rawContent.clearTag.set(index);
                        break;
                    case UTOD:
                        index++;
                        rawContent.bTow.set(index);
                        break;
                    case UTOL:
                        rawContent.bTow.set(index);
                        index++;
                        break;
                    case UTOR:
                        rawContent.bTow.set(index);
                        index++;
                        rawContent.bTow.set(index);
                        break;
                    case DTOU:
                        index++;
                        rawContent.wTob.set(index);
                        break;
                    case DTOL:
                        rawContent.bTow.set(index);
                        index++;
                        rawContent.wTob.set(index);
                        break;
                    case DTOR:
                        rawContent.bTow.set(index);
                        index++;
                        rawContent.clear.set(index);
                        break;
                    case LTOU:
                        rawContent.wTob.set(index);
                        index++;
                        break;
                    case LTOD:
                        rawContent.wTob.set(index);
                        index++;
                        rawContent.bTow.set(index);
                        break;
                    case LTOR:
                        rawContent.clear.set(index);
                        index++;
                        rawContent.bTow.set(index);
                        break;
                    case RTOU:
                        rawContent.wTob.set(index);
                        index++;
                        rawContent.wTob.set(index);
                        break;
                    case RTOD:
                        rawContent.wTob.set(index);
                        index++;
                        rawContent.clear.set(index);
                        break;
                    case RTOL:
                        rawContent.clear.set(index);
                        index++;
                        rawContent.wTob.set(index);
                        break;
                }
                index++;
            }
        }
    }
    public Overlap getOverlapSituation(int x, int y){
        boolean isEvenBlock=(x+y)%2==0;
        int[] samples=grayMatrix.getSamples(x,y);
        int maxIndex=-1;
        int minIndex=-1;
        int max=-1;
        int min=256;
        int countGreat=0;
        int mean=Utils.calculateMean(samples,1,4);
        for(int i=1;i<5;i++){
            int current=samples[i];
            if(current>max){
                maxIndex=i;
                max=current;
            }
            if(current<min){
                minIndex=i;
                min=current;
            }
            if(current>mean){
                countGreat++;
            }
        }
        int center=samples[0];
        if((Math.abs(center-min)<grayThreshold)||(Math.abs(center-max)<grayThreshold)){
/*
            int closestDistance=256;
            int closestIndex=-1;
            for(int i=1;i<5;i++){
                int current=samples[i];
                int distance=Math.abs(center-current);
                if(distance<closestDistance){
                    closestDistance=distance;
                    closestIndex=i;
                }
            }
            int index=closestIndex;
*/
/*
            int index;
            if(overlapCon==booIndex){
                index=maxIndex;
            }else{
                index=minIndex;
            }
            */
            /*
            int index;
            int disMax=0;
            int disMin=0;
            for(int i=1;i<5;i++){
                if(i!=maxIndex&&i!=minIndex){
                    int current=samples[i];
                    disMax+=(max-current)*(max-current);
                    disMin+=(min-current)*(min-current);
                }
            }
            if(disMax>disMin){
                index=maxIndex;
            }else{
                index=minIndex;
            }
            */
            int index=(Math.abs(center-min)<grayThreshold)?minIndex:maxIndex;
            switch (index){
                case 1:
                    return Overlap.UP;
                case 2:
                    return Overlap.DOWN;
                case 3:
                    return Overlap.LEFT;
                case 4:
                    return Overlap.RIGHT;
            }
        }else{
            if(minIndex==1&&maxIndex==2){
                if(overlapCon==isEvenBlock){
                    return Overlap.DTOU;
                }else{
                    return Overlap.UTOD;
                }
            }
            if(minIndex==1&&maxIndex==3){
                if(overlapCon==isEvenBlock){
                    return Overlap.LTOU;
                }else{
                    return Overlap.UTOL;
                }
            }
            if(minIndex==1&&maxIndex==4){
                if(overlapCon==isEvenBlock){
                    return Overlap.RTOU;
                }else{
                    return Overlap.UTOR;
                }
            }
            if(minIndex==2&&maxIndex==1){
                if(overlapCon==isEvenBlock){
                    return Overlap.UTOD;
                }else{
                    return Overlap.DTOU;
                }
            }
            if(minIndex==2&&maxIndex==3){
                if(overlapCon==isEvenBlock){
                    return Overlap.LTOD;
                }else{
                    return Overlap.DTOL;
                }
            }
            if(minIndex==2&&maxIndex==4){
                if(overlapCon==isEvenBlock){
                    return Overlap.RTOD;
                }else{
                    return Overlap.DTOR;
                }
            }
            if(minIndex==3&&maxIndex==1){
                if(overlapCon==isEvenBlock){
                    return Overlap.UTOL;
                }else{
                    return Overlap.LTOU;
                }
            }
            if(minIndex==3&&maxIndex==2){
                if(overlapCon==isEvenBlock){
                    return Overlap.DTOL;
                }else{
                    return Overlap.LTOD;
                }
            }
            if(minIndex==3&&maxIndex==4){
                if(overlapCon==isEvenBlock){
                    return Overlap.RTOL;
                }else{
                    return Overlap.LTOR;
                }
            }
            if(minIndex==4&&maxIndex==1){
                if(overlapCon==isEvenBlock){
                    return Overlap.UTOR;
                }else{
                    return Overlap.RTOU;
                }
            }
            if(minIndex==4&&maxIndex==2){
                if(overlapCon==isEvenBlock){
                    return Overlap.DTOR;
                }else{
                    return Overlap.RTOD;
                }
            }
            if(minIndex==4&&maxIndex==3){
                if(overlapCon==isEvenBlock){
                    return Overlap.LTOR;
                }else{
                    return Overlap.RTOL;
                }
            }
        }
        throw new RuntimeException("unrecognized overlap situation: minIndex "+minIndex+"\tmaxIndex "+maxIndex+"\tsamples "+ Arrays.toString(samples));
    }
}
