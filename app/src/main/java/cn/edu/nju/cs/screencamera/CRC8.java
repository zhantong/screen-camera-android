package cn.edu.nju.cs.screencamera;

import java.util.zip.Checksum;

/**
 * 计算CRC8校验值
 * https://github.com/ggrandes/sandbox/blob/master/src/CRC8.java
 */
public class CRC8 implements Checksum {
    private static final int poly = 0x0D5;
    private int crc = 0;

    @Override
    public void update(final byte[] input, final int offset, final int len) {
        for (int i = 0; i < len; i++) {
            update(input[offset + i]);
        }
    }

    public void update(final byte[] input) {
        update(input, 0, input.length);
    }
    private final void update(final byte b) {
        crc ^= b;
        for (int j = 0; j < 8; j++) {
            if ((crc & 0x80) != 0) {
                crc = ((crc << 1) ^ poly);
            } else {
                crc <<= 1;
            }
        }
        crc &= 0xFF;
    }

    @Override
    public void update(final int b) {
        update(new byte[]{
                (byte) (b >> 24),
                (byte) (b >> 16),
                (byte) (b >> 8),
                (byte) (b)
        });
    }

    @Override
    public long getValue() {
        return (crc & 0xFF);
    }

    @Override
    public void reset() {
        crc = 0;
    }

    /**
     * Test
     */
    public static void main(String[] args) {
        CRC8 crc = new CRC8();
        crc.reset();
        crc.update("test".getBytes());
        System.out.println("181=" + crc.getValue());
        crc.reset();
        crc.update("hello world".getBytes());
        System.out.println("59=" + crc.getValue());
    }
}
