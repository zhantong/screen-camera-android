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

    public RawImage(byte[] pixels,int width,int height,int colorType){
        this.pixels=pixels;
        this.width=width;
        this.height=height;
        this.colorType=colorType;
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
    private int getThreshold() throws NotFoundException {
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

    @Override
    public String toString() {
        return width+"x"+height+" color type "+colorType;
    }
}
