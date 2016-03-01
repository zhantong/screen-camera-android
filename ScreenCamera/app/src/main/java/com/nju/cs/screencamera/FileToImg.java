package com.nju.cs.screencamera;


import com.nju.cs.screencamera.ReedSolomon.GenericGF;
import com.nju.cs.screencamera.ReedSolomon.ReedSolomonEncoder;

import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.encoder.DataEncoder;
import net.fec.openrq.encoder.SourceBlockEncoder;
import net.fec.openrq.parameters.FECParameters;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * 此类目前仅作为与JAVA版本生成二维码参数作同步用
 * 主要供ImgToFile类继承,获得二维码布局等信息
 */
public class FileToImg {
    int frameWhiteLength=10;
    int frameBlackLength=1;
    int frameVaryLength=1;
    int frameVaryTwoLength=1;
    int contentLength=80;
    int blockLength=4;
    int ecNum=80;
    int ecLength=10;
    int fileByteNum;
}
