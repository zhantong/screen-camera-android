package cn.edu.nju.cs.screencamera;

import java.util.Arrays;

/**
 * Created by zhantong on 2016/11/23.
 */

public class RawImage {
    public static final int COLOR_TYPE_YUV=0;
    public static final int COLOR_TYPE_RGB=1;

    public static final int CHANNLE_R=0;
    public static final int CHANNLE_G=1;
    public static final int CHANNLE_B=2;
    public static final int CHANNLE_Y=0;
    public static final int CHANNLE_U=1;
    public static final int CHANNLE_V=2;

    private byte[] pixels;
    private int width;
    private int height;
    private int colorType;
    private int index;
    private long timestamp;

    private int[] thresholds;

    private int[] rectangle;

    private int offsetU;
    private int offsetV;
    public RawImage(){}
    public RawImage(byte[] pixels,int width,int height,int colorType){
        this(pixels,width,height,colorType,0,0);
    }
    public RawImage(byte[] pixels,int width,int height,int colorType,int index,long timestamp){
        this.pixels=pixels;
        this.width=width;
        this.height=height;
        this.colorType=colorType;
        this.index=index;
        this.timestamp=timestamp;
        thresholds=new int[3];
        offsetU=width*height;
        offsetV=width*height+width*height/4;
    }
    public long getTimestamp(){
        return timestamp;
    }
    public int getPixel(int x,int y,int channel){
        switch (channel){
            case CHANNLE_Y:
                return pixels[y * width + x] & 0xff;
            case CHANNLE_U:
                return pixels[offsetU+y/2*(width/2)+x/2]&0xff;
            case CHANNLE_V:
                return pixels[offsetV+y/2*(width/2)+x/2]&0xff;
            default:
                throw new IllegalArgumentException();
        }
    }
    public int[] getRectangle(){
        return rectangle;
    }
    public int getIndex(){
        return index;
    }
    public byte[] getPixels(){
        return pixels;
    }
    public void getThreshold() throws NotFoundException{
        getThreshold(new int[]{CHANNLE_Y,CHANNLE_U,CHANNLE_V});
    }
    public void getThreshold(int[] channels) throws NotFoundException{
        for(int channel:channels){
            thresholds[channel]=getThreshold2(channel);
        }
    }
    public void getThreshold(int channel) throws NotFoundException{
        thresholds[channel]=getThreshold2(channel);
    }
    private int getThreshold2(int channel) throws NotFoundException {
        int[] buckets = new int[256];

        for (int y = 1; y < 5; y++) {
            int row = height * y / 5;
            int right = (width * 4) / 5;
            for (int column = width / 5; column < right; column++) {
                int gray = getPixel(column, row,channel);
                buckets[gray]++;
            }
        }
        int numBuckets = buckets.length;
        int firstPeak = 0;
        int firstPeakSize = 0;
        for (int x = 0; x < numBuckets; x++) {
            if (buckets[x] > firstPeakSize) {
                firstPeak = x;
                firstPeakSize = buckets[x];
            }
        }
        int secondPeak = 0;
        int secondPeakScore = 0;
        for (int x = 0; x < numBuckets; x++) {
            int distanceToFirstPeak = x - firstPeak;
            int score = buckets[x] * distanceToFirstPeak * distanceToFirstPeak;
            if (score > secondPeakScore) {
                secondPeak = x;
                secondPeakScore = score;
            }
        }
        if (firstPeak > secondPeak) {
            int temp = firstPeak;
            firstPeak = secondPeak;
            secondPeak = temp;
        }
        if (secondPeak - firstPeak <= numBuckets / 16) {
            throw new NotFoundException("can't get proper binary threshold");
        }
        int bestValley = 0;
        int bestValleyScore = -1;
        for (int x = firstPeak + 1; x < secondPeak; x++) {
            int fromSecond = secondPeak - x;
            int score = (x - firstPeak) * fromSecond * fromSecond * (firstPeakSize - buckets[x]);
            //int score=fromSecond*fromSecond*(firstPeakSize-buckets[x]);
            if (score > bestValleyScore) {
                bestValley = x;
                bestValleyScore = score;
            }
        }
        return bestValley;
    }
    private int getThreshold1(int channel){
        int[] buckets = new int[256];
        int totalCounts=0;
        for (int y = 1; y < 5; y++) {
            int row = height * y / 5;
            int right = (width * 4) / 5;
            for (int column = width / 5; column < right; column++) {
                int gray=getPixel(column,row,channel);
                buckets[gray]++;
                totalCounts++;
            }
        }
        int countNinty=Math.round(totalCounts*0.9f);
        int ninty=0;
        int count=0;
        for (int x = 0; x < buckets.length; x++) {
            count+=buckets[x];
            if(count>countNinty){
                ninty=x;
                break;
            }
        }
        return ninty+20;
    }
    public int[] getBarcodeVertexes(int[] initRectangle,int channel) throws NotFoundException{
        getThreshold(channel);
        System.out.println("thresholds: "+ Arrays.toString(thresholds));
        int[] whiteRectangle=findWhiteRectangle(initRectangle,channel);
        rectangle=whiteRectangle;
        //int[]whiteRectangle=findWhiteRectangle1(null);
        //return findVertexesFromWhiteRectangle1(whiteRectangle);
        return findVertexesFromWhiteRectangle2(whiteRectangle,channel);
        //return findVertexesFromWhiteRectangle3(whiteRectangle);
    }
    private int[] genInitBorder(){
        int init = 300;
        int left = width / 2 - init;
        int right = width / 2 + init;
        int up = height / 2 - init;
        int down = height / 2 + init;
        return new int[] {left,up,right,down};
    }
    private int[] findWhiteRectangle(int[] initRectangle,int channel) throws NotFoundException {
        if(initRectangle==null){
            initRectangle=genInitBorder();
        }
        int left=initRectangle[0];
        int up=initRectangle[1];
        int right=initRectangle[2];
        int down=initRectangle[3];
        int leftOrig = left;
        int rightOrig = right;
        int upOrig = up;
        int downOrig = down;
        if (left < 0 || right >= width || up < 0 || down >= height) {
            throw new NotFoundException("frame size too small");
        }
        boolean flag;
        while (true) {
            flag = false;
            while (right < width && contains(up, down, right, false,channel,0)) {
                right++;
                flag = true;

            }
            while (down < height && contains(left, right, down, true,channel,0)) {
                down++;
                flag = true;
            }
            while (left > 0 && contains(up, down, left, false,channel,0)) {
                left--;
                flag = true;
            }
            while (up > 0 && contains(left, right, up, true,channel,0)) {
                up--;
                flag = true;
            }
            if (!flag) {
                break;
            }
        }
        if ((left == 0 || up == 0 || right == width || down == height) || (left == leftOrig && right == rightOrig && up == upOrig && down == downOrig)) {
            throw new NotFoundException("didn't find any possible bar code: "+left+" "+up+" "+right+" "+down);
        }
        return new int[]{left,up,right,down};
    }
    private int[] findWhiteRectangle1(int[] initRectangle) throws NotFoundException {
        int channel1=1;
        int shouldBe1=1;
        int channel2=2;
        int shouldBe2=1;
        if(initRectangle==null){
            initRectangle=genInitBorder();
        }
        int left=initRectangle[0];
        int up=initRectangle[1];
        int right=initRectangle[2];
        int down=initRectangle[3];
        int leftOrig = left;
        int rightOrig = right;
        int upOrig = up;
        int downOrig = down;
        if (left < 0 || right >= width || up < 0 || down >= height) {
            throw new NotFoundException("frame size too small");
        }
        boolean flag;
        int centerX=width/2;
        int centerY=height/2;
        boolean leftUpFlag=false,rightUpFlag=false,rightDownFlag=false,leftDownFlag=false;
        for(int scale=1;scale<height/2;scale++){
            int leftUpX1=centerX-scale;
            int leftUpY2=centerY-scale;

            int rightUpX1=centerX+scale;
            int rightUpY2=centerY-scale;

            int rightDownX1=centerX+scale;
            int rightDownY2=centerY+scale;

            int leftDownX1=centerX-scale;
            int leftDownY2=centerY+scale;

            boolean secondFlag=false;
            for(int step=0;step<scale;step++){
                int leftUpY1=centerY-step;
                int leftUpX2=centerX-step;
                if(!leftUpFlag&&pixelEquals(leftUpX1,leftUpY1,1,1)&&pixelEquals(leftUpX1,leftUpY1,2,1)&&!isSinglePoint(leftUpX1,leftUpY1,1)){
                    if(leftUpX1<left){
                        left=leftUpX1;
                    }
                    if(leftUpY1<up){
                        up=leftUpY1;
                    }
                    leftUpFlag=true;
                }
                if(!leftUpFlag&&pixelEquals(leftUpX2,leftUpY2,1,1)&&pixelEquals(leftUpX2,leftUpY2,2,1)&&!isSinglePoint(leftUpX2,leftUpY2,1)){
                    if(leftUpX2<left){
                        left=leftUpX2;
                    }
                    if(leftUpY2<up){
                        up=leftUpY2;
                    }
                    leftUpFlag=true;
                }

                int rightUpY1=centerY-step;
                int rightUpX2=centerX+step;
                if(!rightUpFlag&&pixelEquals(rightUpX1,rightUpY1,1,1)&&pixelEquals(rightUpX1,rightUpY1,2,1)&&!isSinglePoint(rightUpX1,rightUpY1,1)){
                    if(rightUpX1>right){
                        right=rightUpX1;
                    }
                    if(rightUpY1<up){
                        up=rightUpY1;
                    }
                    rightUpFlag=true;
                }
                if(!rightUpFlag&&pixelEquals(rightUpX2,rightUpY2,1,1)&&pixelEquals(rightUpX2,rightUpY2,2,1)&&!isSinglePoint(rightUpX2,rightUpY2,1)){
                    if(rightUpX2>right){
                        right=rightUpX2;
                    }
                    if(rightUpY2<up){
                        up=leftUpY2;
                    }
                    rightUpFlag=true;
                }


                int rightDownY1=centerY+step;
                int rightDownX2=centerX+step;
                if(!rightDownFlag&&pixelEquals(rightDownX1,rightDownY1,1,1)&&pixelEquals(rightDownX1,rightDownY1,2,1)&&!isSinglePoint(rightDownX1,rightDownY1,1)){
                    if(rightDownX1>right){
                        right=rightDownX1;
                    }
                    if(rightDownY1>down){
                        down=rightDownY1;
                    }
                    rightDownFlag=true;
                }
                if(!rightDownFlag&&pixelEquals(rightDownX2,rightDownY2,1,1)&&pixelEquals(rightDownX2,rightDownY2,2,1)&&!isSinglePoint(rightDownX2,rightDownY2,1)){
                    if(rightDownX2>right){
                        right=rightDownX2;
                    }
                    if(rightDownY2>down){
                        down=rightDownY2;
                    }
                    rightDownFlag=true;
                }


                int leftDownY1=centerY+step;
                int leftDownX2=centerX-step;
                if(!leftDownFlag&&pixelEquals(leftDownX1,leftDownY1,1,1)&&pixelEquals(leftDownX1,leftDownY1,2,1)&&!isSinglePoint(leftDownX1,leftDownY1,1)){
                    if(leftDownX1<left){
                        left=leftDownX1;
                    }
                    if(leftDownY1>down){
                        down=leftDownY1;
                    }
                    leftDownFlag=true;
                }
                if(!leftDownFlag&&pixelEquals(leftDownX2,leftDownY2,1,1)&&pixelEquals(leftDownX2,leftDownY2,2,1)&&!isSinglePoint(leftDownX2,leftDownY2,1)){
                    if(leftDownX2<left){
                        left=leftDownX2;
                    }
                    if(leftDownY2>down){
                        down=leftDownY2;
                    }
                    leftDownFlag=true;
                }
                if(leftUpFlag&&rightUpFlag&&rightDownFlag&&leftDownFlag){
                    secondFlag=true;
                    break;
                }
            }
            if(secondFlag){
                break;
            }
        }
        System.out.println("extend: "+left+" "+up+" "+right+" "+down);
        while (true) {
            flag = false;
            while (right < width && contains(up, down, right, false, channel1, shouldBe1)&&contains(up, down, right, false, channel2, shouldBe2)) {
                right++;
                flag = true;

            }
            while (down < height && contains(left, right, down, true, channel1, shouldBe1)&&contains(left, right, down, true, channel2, shouldBe2)) {
                down++;
                flag = true;
            }
            while (left > 0 && contains(up, down, left, false, channel1, shouldBe1)&&contains(up, down, left, false, channel2, shouldBe2)) {
                left--;
                flag = true;
            }
            while (up > 0 && contains(left, right, up, true, channel1, shouldBe1)&&contains(left, right, up, true, channel2, shouldBe2)) {
                up--;
                flag = true;
            }
            if (!flag) {
                break;
            }
        }
        if ((left == 0 || up == 0 || right == width || down == height) || (left == leftOrig && right == rightOrig && up == upOrig && down == downOrig)) {
            throw new NotFoundException("didn't find any possible bar code: "+left+" "+up+" "+right+" "+down);
        }
        return new int[]{left,up,right,down};
    }
    private int[] findVertexesFromWhiteRectangle2(int[] whiteRectangle,int channel){
        int left=whiteRectangle[0];
        int up=whiteRectangle[1];
        int right=whiteRectangle[2];
        int down=whiteRectangle[3];
        System.out.println(left+" "+up+" "+right+" "+down);

        int[] vertexes=new int[8];
        int length=Math.min(right-left,down-up);
        boolean flag=false;
        for(int startX=left,startY=up;startY-up<length;startY++){
            for(int currentX=startX,currentY=startY;currentY>=up;currentX++,currentY--){
                if(pixelEquals(currentX,currentY,channel,0)&&!isSinglePoint(currentX,currentY,channel)){
                    vertexes[0]=currentX;
                    vertexes[1]=currentY;
                    flag=true;
                    break;
                }
            }
            if(flag){
                break;
            }
        }
        flag=false;
        for(int startX=right,startY=up;right-startX<length;startX--){
            for(int currentX=startX,currentY=startY;currentX<=right;currentX++,currentY++){
                if(pixelEquals(currentX,currentY,channel,0)&&!isSinglePoint(currentX,currentY,channel)){
                    vertexes[2]=currentX;
                    vertexes[3]=currentY;
                    flag=true;
                    break;
                }
            }
            if(flag){
                break;
            }
        }
        flag=false;
        for(int startX=right,startY=down;right-startX<length;startX--){
            for(int currentX=startX,currentY=startY;currentX<=right;currentX++,currentY--){
                if(pixelEquals(currentX,currentY,channel,0)&&!isSinglePoint(currentX,currentY,channel)){
                    vertexes[4]=currentX;
                    vertexes[5]=currentY;
                    flag=true;
                    break;
                }
            }
            if(flag){
                break;
            }
        }
        flag=false;
        for(int startX=left,startY=down;down-startY<length;startY--){
            for(int currentX=startX,currentY=startY;currentY<=down;currentX++,currentY++){
                if(pixelEquals(currentX,currentY,channel,0)&&!isSinglePoint(currentX,currentY,channel)){
                    vertexes[6]=currentX;
                    vertexes[7]=currentY;
                    flag=true;
                    break;
                }
            }
            if(flag){
                break;
            }
        }
        return vertexes;
    }
    private int[] findVertexesFromWhiteRectangle3(int[] whiteRectangle){
        int channel=1;
        boolean greater=true;
        int left=whiteRectangle[0];
        int up=whiteRectangle[1];
        int right=whiteRectangle[2];
        int down=whiteRectangle[3];
        System.out.println(left+" "+up+" "+right+" "+down);

        int[] vertexes=new int[8];
        int length=Math.min(right-left,down-up);
        boolean flag=false;
        for(int startX=left,startY=up;startY-up<length;startY++){
            for(int currentX=startX,currentY=startY;currentY>=up;currentX++,currentY--){
                if(pixelEquals(currentX,currentY,0,0)&&pixelEquals(currentX,currentY,1,1)&&pixelEquals(currentX,currentY,2,1)){
                    vertexes[0]=currentX;
                    vertexes[1]=currentY;
                    flag=true;
                    break;
                }
            }
            if(flag){
                break;
            }
        }
        flag=false;
        for(int startX=right,startY=up;right-startX<length;startX--){
            for(int currentX=startX,currentY=startY;currentX<=right;currentX++,currentY++){
                if(pixelEquals(currentX,currentY,0,0)&&pixelEquals(currentX,currentY,1,1)&&pixelEquals(currentX,currentY,2,1)){
                    vertexes[2]=currentX;
                    vertexes[3]=currentY;
                    flag=true;
                    break;
                }
            }
            if(flag){
                break;
            }
        }
        flag=false;
        for(int startX=right,startY=down;right-startX<length;startX--){
            for(int currentX=startX,currentY=startY;currentX<=right;currentX++,currentY--){
                if(pixelEquals(currentX,currentY,0,0)&&pixelEquals(currentX,currentY,1,1)&&pixelEquals(currentX,currentY,2,1)){
                    vertexes[4]=currentX;
                    vertexes[5]=currentY;
                    flag=true;
                    break;
                }
            }
            if(flag){
                break;
            }
        }
        flag=false;
        for(int startX=left,startY=down;down-startY<length;startY--){
            for(int currentX=startX,currentY=startY;currentY<=down;currentX++,currentY++){
                if(pixelEquals(currentX,currentY,0,0)&&pixelEquals(currentX,currentY,1,1)&&pixelEquals(currentX,currentY,2,1)){
                    vertexes[6]=currentX;
                    vertexes[7]=currentY;
                    flag=true;
                    break;
                }
            }
            if(flag){
                break;
            }
        }
        return vertexes;
    }
    private boolean isSinglePoint(int x,int y,int channel){
        int countSame=0;
        int value=getBinary(x,y,channel);
        for(int i=-1;i<2;i++){
            for(int j=-1;j<2;j++){
                int get=getBinary(x+i,y+j,channel);
                if(value==get){
                    countSame++;
                }
            }
        }
        return countSame<=2;
    }
    private boolean contains(int start, int end, int fixed, boolean horizontal,int channel,int shouldBe) {
        if (horizontal) {
            for (int x = start; x <= end; x++) {
                if(pixelEquals(x,fixed,channel,shouldBe)){
                    return true;
                }
            }
        } else {
            for (int y = start; y <= end; y++) {
                if(pixelEquals(fixed,y,channel,shouldBe)){
                    return true;
                }
            }
        }
        return false;
    }
    private boolean pixelEquals(int x, int y,int channel, int pixel){
        return getBinary(x,y,channel)==pixel;
    }
    public int getBinary(int x,int y,int channel){
        if(getPixel(x,y,channel)>=thresholds[channel]){
            return 1;
        }else{
            return 0;
        }
    }
    @Override
    public String toString() {
        return width+"x"+height+" color type "+colorType+" index "+index;
    }
}
