package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 16/4/22.
 */
public class YUVMatrix extends Matrix {
    public YUVMatrix(int dimension) {
        super(dimension, dimension);
    }

    public YUVMatrix(int imgWidth, int imgHeight) {
        super(imgWidth, imgHeight);
    }

    public YUVMatrix(byte[] pixels, int imgWidth, int imgHeight, int[] border) throws NotFoundException {
        super(pixels, imgWidth, imgHeight, border);
    }

    /**
     * YUV格式的灰度计算方法
     * 获取(x,y)点灰度值
     *
     * @param x x轴,即列
     * @param y y轴,即行
     * @return 返回灰度值
     */
    @Override
    public int getGray(int x, int y) {
        return pixels[y * imgWidth + x] & 0xff;
    }
}
