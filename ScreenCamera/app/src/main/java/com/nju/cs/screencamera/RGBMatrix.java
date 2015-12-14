package com.nju.cs.screencamera;

/**
 * 保存RGBA格式图像的相关信息,如原始像素信息
 * 继承自Matrix,主要对获取像素点灰度值方法重载
 */
public class RGBMatrix extends Matrix {
    public RGBMatrix(int dimension) {
        super(dimension, dimension);
    }

    public RGBMatrix(int width, int height) {
        super(width, height);
    }

    public RGBMatrix(byte[] pixels, int width, int height) throws NotFoundException {
        super(pixels, width, height);
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
        int offset = (y * width + x) * 4;
        int r = pixels[offset] & 0xFF;
        int g = pixels[offset + 1] & 0xFF;
        int b = pixels[offset + 2] & 0xFF;
        return ((b * 29 + g * 150 + r * 77 + 128) >> 8);
    }
}
