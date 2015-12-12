package com.nju.cs.screencamera;

/**
 * 存储二进制化的二维码
 * pixels中每个元素的值为0或1,目前没有太大价值,但以后分析可能需要.
 * 注意这里width和height不同于Matrix中.
 */
public class BinaryMatrix {
    private final int width;//二维码宽度
    private final int height;//二维码高度
    private final int[] pixels;//二进制数据

    /**
     * 基本构造函数,作为正方形,且无二进制数据,生成默认值
     *
     * @param dimension 二维码边长
     */
    public BinaryMatrix(int dimension) {
        this(dimension, dimension);
    }

    /**
     * 构造函数,无二进制数据,生成默认值
     *
     * @param width  二维码宽度
     * @param height 二维码高度
     */
    public BinaryMatrix(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixels = new int[width * height];
    }

    /**
     * 构造函数,有二进制数据
     *
     * @param pixels 二进制数据
     * @param width  二维码宽度
     * @param height 二维码高度
     */
    public BinaryMatrix(int[] pixels, int width, int height) {
        this.pixels = pixels;
        this.width = width;
        this.height = height;
    }

    /**
     * 获取(x,y)点二进制值
     *
     * @param x x轴,即列
     * @param y y轴,即行
     * @return 返回(x, y)点二进制值
     */
    public int get(int x, int y) {
        int offset = y * width + x;
        return pixels[offset];
    }

    /**
     * 设置点(x,y)的二进制值,注意值应当是0或1
     *
     * @param x     x轴,即列
     * @param y     y轴,即行
     * @param pixel 设置的值
     */
    public void set(int x, int y, int pixel) {
        int offset = y * width + x;
        pixels[offset] = pixel;
    }

    /**
     * 检测指定点值是否与指定二进制值一致
     *
     * @param x     x轴,即列
     * @param y     y轴,即行
     * @param pixel 指定二进制值
     * @return 若相同返回true, 否则false
     */
    public boolean pixelEquals(int x, int y, int pixel) {
        int offset = y * width + x;
        return pixels[offset] == pixel;
    }

    /**
     * 得到二维码宽度
     *
     * @return 返回二维码宽度
     */
    public int width() {
        return width;
    }

    /**
     * 得到二维码高度
     *
     * @return 返回二维码高度
     */
    public int height() {
        return height;
    }

    /**
     * 将指定区域的二进制值,转换为int类型数组
     * 数组每个元素低8位中,每位代表一个二进制值
     *
     * @param left  区域左边界值
     * @param top   区域上边界值
     * @param right 区域右边界值
     * @param down  区域下边界值
     * @param array 保存返回值的数组
     */
    public void toArray(int left, int top, int right, int down, int[] array) {
        int index = 0;
        for (int j = top; j < down; j++) {
            for (int i = left; i < right; i++) {
                array[index / 8] <<= 1;
                if (get(i, j) == 1) {
                    array[index / 8] |= 0x01;
                }
                index++;
            }
        }
    }
}
