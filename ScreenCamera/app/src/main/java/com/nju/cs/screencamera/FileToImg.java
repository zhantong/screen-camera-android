package com.nju.cs.screencamera;

/*
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
*/

/**
 * Created by zhantong on 15/11/15.
 */
public class FileToImg {
    int frameWhiteLength=10;
    int frameBlackLength=1;
    int frameVaryLength=1;
    int contentLength=76;
    int blockLength=4;
    int grayCodeLength=10;
    /*
    public static void main(String[] args){
        FileToImg f=new FileToImg();
        f.toImage(f.readFile("/Users/zhantong/Desktop/test.txt"),"/Users/zhantong/Desktop/test/");
    }
    public String readFile(String path){
        File inFile=new File(path);
        FileInputStream fileInputStream=null;
        try{
            fileInputStream=new FileInputStream(inFile);
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }
        StringBuffer stringBuffer=new StringBuffer();
        try{
            int i;
            while((i=fileInputStream.read())!=-1) {
                String b = Integer.toBinaryString(i);
                int temp=Integer.parseInt(b);
                stringBuffer.append(String.format("%1$08d",temp));
            }
            return stringBuffer.toString();
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }
    public void toImage(String biData,String path){
        String imgType="png";
        int length=((frameWhiteLength+frameBlackLength+frameVaryLength)*2+contentLength)*blockLength;
        int startOffset=(frameWhiteLength+frameBlackLength+frameVaryLength)*blockLength;
        int stopOffset=startOffset+contentLength*blockLength;
        int biDataLength=biData.length();
        int imgAmount=(int)Math.ceil((double)biDataLength/(contentLength*contentLength));
        int index = 0;
        for(int i=1;i<=imgAmount;i++) {
            BufferedImage img = new BufferedImage(length, length, BufferedImage.TYPE_BYTE_BINARY);
            Graphics2D g = img.createGraphics();
            g.setBackground(Color.WHITE);
            g.clearRect(0, 0, length, length);
            g.setColor(Color.BLACK);

            boolean flag = true;
            for (int y = startOffset; y < stopOffset; y += blockLength) {
                for (int x = startOffset; x < stopOffset; x += blockLength) {
                    if (index < biDataLength) {
                        if (biData.charAt(index) == '0') {
                            g.fillRect(x, y, blockLength, blockLength);
                        }
                        index++;
                    } else if (flag) {
                        g.fillRect(x, y, blockLength, blockLength);
                        flag = false;
                    }
                }
            }
            addFrame(g);
            g.dispose();
            img.flush();
            String destPath=String.format("%s%06d.%s",path,i,imgType);
            File destFile = new File(destPath);
            try {
                ImageIO.write(img, imgType, destFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void addFrame(Graphics2D g){
        int startOffset=(frameWhiteLength+frameBlackLength)*blockLength;
        int stopOffset=startOffset+(contentLength+frameVaryLength)*blockLength;
        int vBlockLength=frameVaryLength*blockLength;
        for(int i=startOffset;i<stopOffset;i+=vBlockLength*2){
            g.fillRect(i,startOffset,vBlockLength,vBlockLength);
            g.fillRect(startOffset,i,vBlockLength,vBlockLength);
            g.fillRect(stopOffset,i,vBlockLength,vBlockLength);
            g.fillRect(i,stopOffset,vBlockLength,vBlockLength);
        }
        startOffset=frameWhiteLength*blockLength;
        stopOffset=startOffset+(2*(frameBlackLength+frameVaryLength)+contentLength)*blockLength;
        int bBlockLength=frameBlackLength*blockLength;
        g.fillRect(startOffset,startOffset,bBlockLength,stopOffset-startOffset);
        g.fillRect(startOffset,startOffset,stopOffset-startOffset,bBlockLength);
        g.fillRect(startOffset,stopOffset-bBlockLength,stopOffset-startOffset,bBlockLength);
        g.fillRect(stopOffset-bBlockLength,startOffset,bBlockLength,stopOffset-startOffset);
    }
    */
}
