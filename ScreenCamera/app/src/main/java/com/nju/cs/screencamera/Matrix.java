package com.nju.cs.screencamera;

import android.util.Log;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 保存YUV格式图像的相关信息,如原始像素信息
 * 以及一些对原始图像操作的方法
 */
public class Matrix {
    private static final boolean VERBOSE = false;//是否记录详细log
    private static final String TAG = "Matrix";//log tag
    protected final int width;//图像宽度
    protected final int height;//图像高度
    protected final byte[] pixels;//图像每个像素点原始值
    private int threshold = 0;//二值化阈值
    private int[] borders;//图像中二维码的四个顶点坐标值
    private PerspectiveTransform transform;//透视变换参数
    public int frameIndex;//此图像中二维码的帧编号
    public GrayMatrix grayMatrix;
    public boolean reverse;

    /**
     * 基本构造函数,作为正方形,且无原始像素数据,生成默认值
     *
     * @param dimension 图像边长
     */
    public Matrix(int dimension) {
        this(dimension, dimension);
    }

    /**
     * 构造函数,无原始像素数据,生成默认值
     *
     * @param width  图像宽度
     * @param height 图像高度
     */
    public Matrix(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixels = new byte[width * height];
    }

    /**
     * 构造函数,有原始数据
     *
     * @param pixels 原始像素数据
     * @param width  图像宽度
     * @param height 图像高度
     * @throws NotFoundException 未找到二维码异常
     */
    public Matrix(byte[] pixels, int width, int height) throws NotFoundException {
        this.pixels = pixels;
        this.width = width;
        this.height = height;
        this.threshold = getThreshold();
        this.borders = findBoarder();
    }

    /**
     * 透视变换
     * 指定透视变换后二维码的四个顶点坐标,结合找到的图像中的二维码顶点坐标进行透视变换
     * 透视变换相当于只是算出一些矩阵参数,不进行具体的像素数据操作
     *
     * @param p1ToX 左上角顶点x值
     * @param p1ToY 左上角顶点y值
     * @param p2ToX 右上角顶点x值
     * @param p2ToY 右上角顶点y值
     * @param p3ToX 右下角顶点x值
     * @param p3ToY 右下角顶点y值
     * @param p4ToX 左下角顶点x值
     * @param p4ToY 左下角顶点y值
     */
    public void perspectiveTransform(float p1ToX, float p1ToY,
                                     float p2ToX, float p2ToY,
                                     float p3ToX, float p3ToY,
                                     float p4ToX, float p4ToY) {
        transform = PerspectiveTransform.quadrilateralToQuadrilateral(p1ToX, p1ToY,
                p2ToX, p2ToY,
                p3ToX, p3ToY,
                p4ToX, p4ToY,
                borders[0], borders[1],
                borders[2], borders[3],
                borders[4], borders[5],
                borders[6], borders[7]);
    }

    /**
     * 获取指定坐标点灰度值
     *
     * @param x x轴,即列
     * @param y y轴,即行
     * @return 返回灰度值
     */
    public int getGray(int x, int y) {
        return pixels[y * width + x] & 0xff;
    }

    /**
     * 获取指定坐标点二值化值,即0或1
     * 根据阈值和灰度值判断二值化结果
     *
     * @param x x轴,即列
     * @param y y轴,即行
     * @return 返回二值化值, 即0或1
     */
    public int get(int x, int y) {
        if (getGray(x, y) <= threshold) {
            return 0;
        } else {
            return 1;
        }
    }

    /**
     * 判断指定坐标点二值化值是否与指定值相同
     *
     * @param x     x轴,即列
     * @param y     y轴,即行
     * @param pixel 指定值
     * @return 与指定值相同返回true, 否则返回false
     */
    public boolean pixelEquals(int x, int y, int pixel) {
        return get(x, y) == pixel;
    }

    /**
     * 获取图像宽度
     *
     * @return 图像宽度
     */
    public int width() {
        return width;
    }

    /**
     * 获取图像高度
     *
     * @return 图像高度
     */
    public int height() {
        return height;
    }

    public byte[] getHead(int dimensionX, int dimensionY){

        if(grayMatrix==null){
            initGrayMatrix(dimensionX,dimensionY);
        }
        grayMatrix.reverse=reverse;
        return grayMatrix.getHead();
    }
    public void initGrayMatrix(int dimensionX, int dimensionY){
        int length=80;
        grayMatrix=new GrayMatrix(dimensionX);
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
        HashMap<Integer,Point>[] bars=new HashMap[4];
        bars[0]=getVary(1.5f);
        bars[1]=getVary(2.5f);
        bars[2]=getVary((float)length+3.5f);
        bars[3]=getVary((float)length+4.5f);
        /*
        for(Point p:bars[3].values()){
            p.print();
        }
        */
        grayMatrix.bars=bars;
    }
    public HashMap<Integer,Point> getVary(float offsetX){
        int length=80;
        float[] points=new float[length*2];
        int index=0;
        for(int y=3;y<3+length;y++){
            points[index]=offsetX;
            index++;
            points[index]=(float)y+0.5f;
            index++;
        }
        transform.transformPoints(points);
        int[] a=new int[length];
        int[] b=new int[length];
        for(int x=0;x<length*2;x+=2){
            a[x/2]=Math.round(points[x]);
            b[x/2]=Math.round(points[x+1]);
        }
        for(int i=0;i<length;i++){
            ;
            //System.out.println(a[i]+" "+b[i]+" "+getGray(a[i],b[i]));
        }
        HashMap<Integer,Point> map=new HashMap<>();
        for(int y=b[0];y<=b[length-1];y++){
            int x=getX(a,b,y);
            //System.out.println(x+" "+y+" "+getGray(x,y));
            map.put(y,new Point(x,y,getGray(x,y)));
        }
        return map;
    }
    public int getX(int[] a,int[] b,int y){
        int i;
        for(i=0;i<b.length-1;i++){
            if(y<b[i]){
                break;
            }
        }
        if(a[i-1]==a[i]){
            return a[i];
        }
        float res=(float)(y-b[i-1])/(b[i]-b[i-1])*(a[i]-a[i-1])+a[i-1];
        //System.out.println(res);
        return Math.round(res);
    }
    public BitSet getContent(int dimensionX, int dimensionY){
        if(grayMatrix==null){
            initGrayMatrix(dimensionX,dimensionY);
        }
        grayMatrix.reverse=reverse;
        System.out.println("reverse:"+reverse);
        //grayMatrix.print();
        return grayMatrix.getContent();
    }
    /**
     * 获取图像的阈值
     * 阈值由灰度值确定,获得阈值的方法为双峰法
     * 对整个图像均匀采样5行,每行采样中间的3/5
     * 得到灰度值的分布,得到两个峰值,再取双峰之间最优值
     *
     * @return 灰度阈值
     * @throws NotFoundException 不能确定阈值则抛出未找到二维码异常
     */
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
        if (VERBOSE) {
            Log.d(TAG, "threshold:" + bestValley);
        }
        return bestValley;
    }

    /**
     * 确定边界时需要
     * 判断指定线段中像素是否全部为白色
     * 以下参数可以唯一确定一条线段
     *
     * @param start      线段起点
     * @param end        线段终点
     * @param fixed      线段保持不变的坐标值
     * @param horizontal 线段是否为水平
     * @return 若线段包含有黑点, 则返回true, 否则返回false
     */
    public boolean containsBlack(int start, int end, int fixed, boolean horizontal) {
        if (horizontal) {
            for (int x = start; x <= end; x++) {
                if (pixelEquals(x, fixed, 0)) {
                    return true;
                }

            }
        } else {
            for (int y = start; y <= end; y++) {
                if (pixelEquals(fixed, y, 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 寻找图像中二维码的四个顶点坐标值
     * 首先采用矩形放大,找到第一个4条边都是全是白色的矩形,这样认为二维码在矩形内
     * 然后对矩形的每条边,逐步向内收缩,同时检查边上的黑点,是否为噪点,若是噪点则忽略,继续收缩
     * 直到碰到第一个属于二维码的黑点,此时认为此黑点是二维码的一个顶点
     * 通过判断此顶点与矩形边界的关系,可以确定此顶点是二维码的具体哪个顶点
     * 这样即可确定出二维码的四个顶点
     *
     * @return 长度为8的数组, 分别存储每个顶点的x和y坐标
     * @throws NotFoundException 能够确定不可能发现二维码时,则抛出未找到二维码异常
     */
    public int[] findBoarder() throws NotFoundException {
        int init = 20;
        int left = width / 2 - init;
        int right = width / 2 + init;
        int up = height / 2 - init;
        int down = height / 2 + init;
        int leftOrig = left;
        int rightOrig = right;
        int upOrig = up;
        int downOrig = down;
        if (VERBOSE) {
            Log.d(TAG, "boarder init: up:" + up + "\t" + "right:" + right + "\t" + "down:" + down + "\t" + "left:" + left);
        }
        if (left < 0 || right >= width || up < 0 || down >= height) {
            throw new NotFoundException("frame size too small");
        }
        boolean flag;
        while (true) {
            flag = false;
            while (containsBlack(up, down, right, false) && right < width) {
                right++;
                flag = true;

            }
            while (containsBlack(left, right, down, true) && down < height) {
                down++;
                flag = true;
            }
            while (containsBlack(up, down, left, false) && left > 0) {
                left--;
                flag = true;
            }
            while (containsBlack(left, right, up, true) && up > 0) {
                up--;
                flag = true;
            }
            if (!flag) {
                break;
            }
        }
        if (VERBOSE) {
            Log.d(TAG, "find boarder: up:" + up + "\t" + "right:" + right + "\t" + "down:" + down + "\t" + "left:" + left);
        }
        if ((left == 0 || up == 0 || right == width || down == height) || (left == leftOrig && right == rightOrig && up == upOrig && down == downOrig)) {
            throw new NotFoundException("didn't find any possible bar code");
        }
        int[] vertexs = new int[8];
        left = findVertex(up, down, left, vertexs, 0, 3, false, false);
        if (VERBOSE) {
            Log.d(TAG, "found 1 vertex,left border now is:" + left);
            Log.d(TAG, "vertexes: (" + vertexs[0] + "," + vertexs[1] + ")\t(" + vertexs[2] + "," + vertexs[3] + ")\t(" + vertexs[4] + "," + vertexs[5] + ")\t(" + vertexs[6] + "," + vertexs[7] + ")");
        }
        up = findVertex(left, right, up, vertexs, 0, 1, true, false);

        if (VERBOSE) {
            Log.d(TAG, "found 2 vertex,up border now is:" + up);
            Log.d(TAG, "vertexes: (" + vertexs[0] + "," + vertexs[1] + ")\t(" + vertexs[2] + "," + vertexs[3] + ")\t(" + vertexs[4] + "," + vertexs[5] + ")\t(" + vertexs[6] + "," + vertexs[7] + ")");
        }
        right = findVertex(up, down, right, vertexs, 1, 2, false, true);
        if (VERBOSE) {
            Log.d(TAG, "found 3 vertex,right border now is:" + right);
            Log.d(TAG, "vertexes: (" + vertexs[0] + "," + vertexs[1] + ")\t(" + vertexs[2] + "," + vertexs[3] + ")\t(" + vertexs[4] + "," + vertexs[5] + ")\t(" + vertexs[6] + "," + vertexs[7] + ")");
        }
        down = findVertex(left, right, down, vertexs, 3, 2, true, true);
        if (VERBOSE) {
            Log.d(TAG, "found 4 vertex,down border now is:" + down);
        }
        if (VERBOSE) {
            Log.d(TAG, "vertexes: (" + vertexs[0] + "," + vertexs[1] + ")\t(" + vertexs[2] + "," + vertexs[3] + ")\t(" + vertexs[4] + "," + vertexs[5] + ")\t(" + vertexs[6] + "," + vertexs[7] + ")");
        }
        if (vertexs[0] == 0 || vertexs[2] == 0 || vertexs[4] == 0 || vertexs[6] == 0) {
            throw new NotFoundException("vertexs error");
        }
        return vertexs;
    }

    /**
     * 寻找矩形内二维码顶点坐标
     * 方法在findBoarder()中已经描述
     *
     * @param b1         较小边界坐标值
     * @param b2         较大边界坐标值
     * @param fixed      需要移动边界,在边界线段上不变的坐标值
     * @param vertexs    存储寻找到的顶点坐标
     * @param p1         可能的顶点编号
     * @param p2         可能的顶点编号
     * @param horizontal 此边是否为水平
     * @param sub        此边的移动方向,即对fix加还是减
     * @return 返回收缩后的矩形边界fixed值
     * @throws NotFoundException 能够确定不可能发现二维码时,则抛出未找到二维码异常
     */
    public int findVertex(int b1, int b2, int fixed, int[] vertexs, int p1, int p2, boolean horizontal, boolean sub) throws NotFoundException {
        int mid = (b2 - b1) / 2;
        boolean checkP1 = vertexs[p1 * 2] == 0;
        boolean checkP2 = vertexs[p2 * 2] == 0;

        if (horizontal) {
            while (true) {
                if (checkP1) {
                    for (int i = 1; i <= mid; i++) {
                        if (pixelEquals(b1 + i, fixed, 0) && !isSinglePoint(b1 + i, fixed)) {
                            vertexs[p1 * 2] = b1 + i;
                            vertexs[p1 * 2 + 1] = fixed;
                            return fixed;
                        }
                    }
                }
                if (checkP2) {
                    for (int i = 1; i <= mid; i++) {
                        if (pixelEquals(b2 - i, fixed, 0) && !isSinglePoint(b2 - i, fixed)) {
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
                        if (pixelEquals(fixed, b1 + i, 0) && !isSinglePoint(fixed, b1 + i)) {
                            vertexs[p1 * 2] = fixed;
                            vertexs[p1 * 2 + 1] = b1 + i;
                            return fixed;
                        }
                    }
                }
                if (checkP2) {
                    for (int i = 1; i <= mid; i++) {
                        if (pixelEquals(fixed, b2 - i, 0) && !isSinglePoint(fixed, b2 - i)) {
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

    /**
     * 判断此点是否为噪点
     * 此点一定是黑点,通过判断周围8个像素点,是否有超过5个像素点为白色,来确定此点是否为噪点
     * 即中值滤波
     *
     * @param x x轴,即列
     * @param y y轴,即行
     * @return 为噪点则返回true, 否则返回false
     */
    public boolean isSinglePoint(int x, int y) {
        int sum = get(x - 1, y - 1) + get(x, y - 1) + get(x + 1, y - 1) + get(x - 1, y) + get(x + 1, y) + get(x - 1, y + 1) + get(x, y + 1) + get(x + 1, y + 1);
        //System.out.println("isSinglePoint:"+sum);
        return sum >= 6;
    }
}