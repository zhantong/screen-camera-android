package cn.edu.nju.cs.screencamera;

import android.util.Log;

import java.util.BitSet;
import java.util.List;

/**
 * Created by zhantong on 16/4/24.
 */
public class MatrixZoomVary extends Matrix{
    private RawContent rawContent;
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
    private Overlap maxIndex(int x,int y){
        int[] samples=grayMatrix.getSamples(x,y);
        int index;
        if((x+y)%2==0){
            int maxIndex=-1;
            int max=-1;
            for(int i=1;i<5;i++){
                if(samples[i]>max){
                    maxIndex=i;
                    max=samples[i];
                }
            }
            index=maxIndex;
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
            index=minIndex;
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
                //int[] samples=grayMatrix.getSamples(x,y);
                //int maxIndex=maxIndex(samples,1)-1;
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
                switch (getOverlapSituation(x,y)) {
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
        int[] samples=grayMatrix.getSamples(x,y);
        int mean=mean(samples,1,4);
        int countGreat=0;
        int countLess=0;
        int[] temp=new int[samples.length];
        int maxIndex=-1;
        int minIndex=-1;
        for(int i=1;i<5;i++){
            if(samples[i]>mean){
                countGreat++;
                temp[i]=1;
                maxIndex=i;
            }else{
                countLess++;
                temp[i]=-1;
                minIndex=i;
            }
        }
        if(countGreat==1||countLess==1){
            int index=(countGreat==1)?maxIndex:minIndex;
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
            if((x+y)%2==0) {
                if (temp[1]==1&&temp[2]==1) {
                    if (samples[1] > samples[2]) {
                        return Overlap.UTOD;
                    } else {
                        return Overlap.DTOU;
                    }
                }
                if (temp[1]==1&&temp[3]==1) {
                    if (samples[1] > samples[3]) {
                        return Overlap.UTOL;
                    } else {
                        return Overlap.LTOU;
                    }
                }
                if (temp[1]==1&&temp[4]==1) {
                    if (samples[1] > samples[4]) {
                        return Overlap.UTOR;
                    } else {
                        return Overlap.RTOU;
                    }
                }
                if (temp[2]==1&&temp[3]==1) {
                    if (samples[2] > samples[3]) {
                        return Overlap.DTOL;
                    } else {
                        return Overlap.LTOD;
                    }
                }
                if (temp[2]==1&&temp[4]==1) {
                    if (samples[2] > samples[4]) {
                        return Overlap.DTOR;
                    } else {
                        return Overlap.RTOD;
                    }
                }
                if (temp[3]==1&&temp[4]==1) {
                    if (samples[3] > samples[4]) {
                        return Overlap.LTOR;
                    } else {
                        return Overlap.RTOL;
                    }
                }
            }else{
                if (temp[1]==-1&&temp[2]==-1) {
                    if (samples[1] < samples[2]) {
                        return Overlap.UTOD;
                    } else {
                        return Overlap.DTOU;
                    }
                }
                if (temp[1]==-1&&temp[3]==-1) {
                    if (samples[1] < samples[3]) {
                        return Overlap.UTOL;
                    } else {
                        return Overlap.LTOU;
                    }
                }
                if (temp[1]==-1&&temp[4]==-1) {
                    if (samples[1] < samples[4]) {
                        return Overlap.UTOR;
                    } else {
                        return Overlap.RTOU;
                    }
                }
                if (temp[2]==-1&&temp[3]==-1) {
                    if (samples[2] < samples[3]) {
                        return Overlap.DTOL;
                    } else {
                        return Overlap.LTOD;
                    }
                }
                if (temp[2]==-1&&temp[4]==-1) {
                    if (samples[2] < samples[4]) {
                        return Overlap.DTOR;
                    } else {
                        return Overlap.RTOD;
                    }
                }
                if (temp[3]==-1&&temp[4]==-1) {
                    if (samples[3] < samples[4]) {
                        return Overlap.LTOR;
                    } else {
                        return Overlap.RTOL;
                    }
                }
            }
        }
        System.out.println("wrong");
        return Overlap.RTOL;
    }
    public BitSet getContent(){
        if(rawContent==null){
            sampleContent(getBarCodeWidth(),getBarCodeHeight());
        }
        if(reverse){
            return rawContent.getRawContent(true);
        }
        else{
            return rawContent.getRawContent(false);
        }
    }
    public void sampleContent(int dimensionX, int dimensionY){
        if (grayMatrix == null) {
            initGrayMatrix(dimensionX,dimensionY);
            isMixed=isMixed();
            Log.i(TAG,"frame mixed:"+isMixed);
        }
        rawContent=new RawContent(bitsPerBlock*contentLength*contentLength);
        if(VERBOSE){Log.d(TAG,"color reversed:"+reverse);}
        if(isMixed){
            getRawContent();
        }
        else {
            getRawContentSimple();
        }
    }
    public boolean isMixed(){
        int x=frameBlackLength+frameVaryLength;
        for(int y=frameBlackLength;y<frameBlackLength+contentLength;y++){
            int[] current=grayMatrix.getSamples(x,y);
            int mean=mean(current,1,4);
            int count=0;
            for(int i=1;i<5;i++){
                if(current[i]>mean){
                    count++;
                }
            }
            if(count>1){
                return true;
            }
        }
        return false;
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
