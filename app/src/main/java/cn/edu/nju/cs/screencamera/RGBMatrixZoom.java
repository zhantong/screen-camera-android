package cn.edu.nju.cs.screencamera;

/**
 * Created by zhantong on 16/4/22.
 */
public class RGBMatrixZoom extends MatrixZoom {
    public RGBMatrixZoom(int dimension) {
        super(dimension, dimension);
    }

    public RGBMatrixZoom(int imgWidth, int imgHeight) {
        super(imgWidth, imgHeight);
    }

    public RGBMatrixZoom(byte[] pixels, int imgWidth, int imgHeight,int[] border) throws NotFoundException {
        super(pixels, imgWidth, imgHeight,border);
    }

    /**
     * Matrix存储的是YUV格式信息,对于RGBA格式需要新的灰度计算方法
     * 获取(x,y)点灰度值
     *
     * @param x x轴,即列
     * @param y y轴,即行
     * @return 返回灰度值
     */
    @Override
    public int getGray(int x, int y) {
        int offset = (y * imgWidth + x) * 4;
        int r = pixels[offset] & 0xFF;
        int g = pixels[offset + 1] & 0xFF;
        int b = pixels[offset + 2] & 0xFF;
        return ((b * 29 + g * 150 + r * 77 + 128) >> 8);
    }
}

