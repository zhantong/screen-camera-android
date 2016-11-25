package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 2016/11/23.
 */

public class RawImage {
    public static final int COLOR_TYPE_YUV=0;
    public static final int COLOR_TYPE_RGB=1;
    private byte[] pixels;
    private int width;
    private int height;
    private int colorType;
    private int grayThreshold=-1;

    public RawImage(byte[] pixels,int width,int height,int colorType) throws NotFoundException{
        this.pixels=pixels;
        this.width=width;
        this.height=height;
        this.colorType=colorType;
        getGrayThreshold();
    }
    public int getGray(int x, int y) {
        switch (colorType){
            case COLOR_TYPE_YUV:
                return getYUVGray(x,y);
            case COLOR_TYPE_RGB:
                return getRGBGray(x,y);
        }
        throw new IllegalArgumentException("unknown color type "+colorType);
    }
    private int getYUVGray(int x, int y) {
        return pixels[y * width + x] & 0xff;
    }
    private int getRGBGray(int x, int y) {
        int offset = (y * width + x) * 4;
        int r = pixels[offset] & 0xFF;
        int g = pixels[offset + 1] & 0xFF;
        int b = pixels[offset + 2] & 0xFF;
        return ((b * 29 + g * 150 + r * 77 + 128) >> 8);
    }
    public int getGrayThreshold() throws NotFoundException {
        if(grayThreshold==-1){
            grayThreshold=getGrayThreshold1();
        }
        return grayThreshold;
    }
    private int getGrayThreshold1() throws NotFoundException {
        int[] buckets = new int[256];

        for (int y = 1; y < 5; y++) {
            int row = height * y / 5;
            int right = (width * 4) / 5;
            for (int column = width / 5; column < right; column++) {
                int gray = getGray(column, row);
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
    public int[] getBarcodeVertexes() throws NotFoundException{
        int[] whiteRectangle=findWhiteRectangle(null);
        //return findVertexesFromWhiteRectangle1(whiteRectangle);
        return findVertexesFromWhiteRectangle2(whiteRectangle);
    }
    private int[] genInitBorder(){
        int init = 60;
        int left = width / 2 - init;
        int right = width / 2 + init;
        int up = height / 2 - init;
        int down = height / 2 + init;
        return new int[] {left,up,right,down};
    }
    private int[] findWhiteRectangle(int[] initRectangle) throws NotFoundException {
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
            while (right < width && containsBlack(up, down, right, false)) {
                right++;
                flag = true;

            }
            while (down < height && containsBlack(left, right, down, true)) {
                down++;
                flag = true;
            }
            while (left > 0 && containsBlack(up, down, left, false)) {
                left--;
                flag = true;
            }
            while (up > 0 && containsBlack(left, right, up, true)) {
                up--;
                flag = true;
            }
            if (!flag) {
                break;
            }
        }
        if ((left == 0 || up == 0 || right == width || down == height) || (left == leftOrig && right == rightOrig && up == upOrig && down == downOrig)) {
            throw new NotFoundException("didn't find any possible bar code");
        }
        return new int[]{left,up,right,down};

    }
    private int[] findVertexesFromWhiteRectangle1(int[] whiteRectangle) throws NotFoundException{
        int left=whiteRectangle[0];
        int up=whiteRectangle[1];
        int right=whiteRectangle[2];
        int down=whiteRectangle[3];
        int[] vertexes = new int[8];
        left = findVertexes(up, down, left, vertexes, 0, 3, false, false);
        up = findVertexes(left, right, up, vertexes, 0, 1, true, false);
        right = findVertexes(up, down, right, vertexes, 1, 2, false, true);
        down = findVertexes(left, right, down, vertexes, 3, 2, true, true);
        if (vertexes[0] == 0 || vertexes[2] == 0 || vertexes[4] == 0 || vertexes[6] == 0) {
            throw new NotFoundException("vertexes error");
        }
        return vertexes;
    }
    private int findVertexes(int b1, int b2, int fixed, int[] vertexs, int p1, int p2, boolean horizontal, boolean sub) throws NotFoundException {
        int mid = (b2 - b1) / 2;
        boolean checkP1 = vertexs[p1 * 2] == 0;
        boolean checkP2 = vertexs[p2 * 2] == 0;

        if (horizontal) {
            while (true) {
                if (checkP1) {
                    for (int i = 1; i <= mid; i++) {
                        if (pixelIsBlack(b1 + i, fixed) && !isSinglePoint(b1 + i, fixed)) {
                            vertexs[p1 * 2] = b1 + i;
                            vertexs[p1 * 2 + 1] = fixed;
                            return fixed;
                        }
                    }
                }
                if (checkP2) {
                    for (int i = 1; i <= mid; i++) {
                        if (pixelIsBlack(b2 - i, fixed) && !isSinglePoint(b2 - i, fixed)) {
                            vertexs[p2 * 2] = b2 - i;
                            vertexs[p2 * 2 + 1] = fixed;
                            return fixed;
                        }
                    }
                }
                if (sub) {
                    fixed--;
                    if (fixed <= 0) {
                        throw new NotFoundException("didn't find any possible bar code");
                    }
                } else {
                    fixed++;
                    if (fixed >= height) {
                        throw new NotFoundException("didn't find any possible bar code");
                    }
                }
            }
        } else {
            while (true) {
                if (checkP1) {
                    for (int i = 1; i <= mid; i++) {
                        if (pixelIsBlack(fixed, b1 + i) && !isSinglePoint(fixed, b1 + i)) {
                            vertexs[p1 * 2] = fixed;
                            vertexs[p1 * 2 + 1] = b1 + i;
                            return fixed;
                        }
                    }
                }
                if (checkP2) {
                    for (int i = 1; i <= mid; i++) {
                        if (pixelIsBlack(fixed, b2 - i) && !isSinglePoint(fixed, b2 - i)) {
                            vertexs[p2 * 2] = fixed;
                            vertexs[p2 * 2 + 1] = b2 - i;
                            return fixed;
                        }
                    }
                }
                if (sub) {
                    fixed--;
                    if (fixed <= 0) {
                        throw new NotFoundException("didn't find any possible bar code");
                    }
                } else {
                    fixed++;
                    if (fixed >= width) {
                        throw new NotFoundException("didn't find any possible bar code");
                    }
                }
            }
        }
    }
    private int[] findVertexesFromWhiteRectangle2(int[] whiteRectangle){
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
                if(pixelIsBlack(currentX,currentY)&&!isSinglePoint(currentX,currentY)){
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
                if(pixelIsBlack(currentX,currentY)&&!isSinglePoint(currentX,currentY)){
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
                if(pixelIsBlack(currentX,currentY)&&!isSinglePoint(currentX,currentY)){
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
                if(pixelIsBlack(currentX,currentY)&&!isSinglePoint(currentX,currentY)){
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
    private boolean isSinglePoint(int x, int y) {
        int sum = getBinary(x - 1, y - 1) + getBinary(x, y - 1) + getBinary(x + 1, y - 1) + getBinary(x - 1, y) + getBinary(x + 1, y) + getBinary(x - 1, y + 1) + getBinary(x, y + 1) + getBinary(x + 1, y + 1);
        return sum >= 6;
    }
    private boolean containsBlack(int start, int end, int fixed, boolean horizontal) {
        if (horizontal) {
            for (int x = start; x <= end; x++) {
                if (pixelIsBlack(x, fixed)) {
                    return true;
                }

            }
        } else {
            for (int y = start; y <= end; y++) {
                if (pixelIsBlack(fixed, y)) {
                    return true;
                }
            }
        }
        return false;
    }
    private boolean pixelEquals(int x, int y, int pixel) {
        return getBinary(x, y) == pixel;
    }
    private boolean pixelIsBlack(int x, int y){
        return pixelEquals(x,y,0);
    }
    public int getBinary(int x, int y) {
        if (getGray(x, y) <= grayThreshold) {
            return 0;
        } else {
            return 1;
        }
    }
    @Override
    public String toString() {
        return width+"x"+height+" color type "+colorType;
    }
}
