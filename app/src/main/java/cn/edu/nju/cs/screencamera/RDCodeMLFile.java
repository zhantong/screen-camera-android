package cn.edu.nju.cs.screencamera;


import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

/**
 * Created by zhantong on 2017/6/4.
 */

public class RDCodeMLFile implements StreamDecode.CallBack {
    private static final String TAG = "ShiftCodeMLFile";
    RDCodeMLConfig config = new RDCodeMLConfig();
    int countAllRegions = 0;
    int numFileBytes = -1;
    int numAllRegions = -1;
    int countFrames = 0;
    int numDataRegions;
    int indexCenterBlock;
    int numRegionDataBytes;
    int realCurrentRegionOffset;
    int numRegions;
    int numBytesPerRegion;
    int numBytesPerRegionLine;
    int numRSBytes;
    Map<Integer, int[][][]> windows = new HashMap<>();

    public RDCodeMLFile() {
    }

    @Override
    public void beforeStream(StreamDecode streamDecod) {
        numRSBytes = 6;
        int numColors = 4;
        int numRegionBytes = (config.regionWidth * config.regionHeight) / (8 / (int) Math.sqrt(numColors));
        numRegionDataBytes = numRegionBytes - numRSBytes;
        int numInterFrameParity = 1;
        int numFramesPerWindow = 8;
        int indexLastFrame = numFramesPerWindow - numInterFrameParity;
        numBytesPerRegionLine = config.mainBlock.get(District.MAIN).getBitsPerUnit() * config.regionWidth / 8;
        numBytesPerRegion = numBytesPerRegionLine * config.regionHeight;
        numRegions = config.numRegionVertical * config.numRegionHorizon;
        int numParityRegions = 3;
        numDataRegions = numRegions - numParityRegions;
        int numBytesPerFrame = numRegions * numBytesPerRegion;
        indexCenterBlock = numRegions / 2;
    }

    @Override
    public void processFrame(StreamDecode streamDecode, RawImage frame) {

    }

    @Override
    public void processFrame(StreamDecode streamDecode, JsonElement frameData) {
        Gson gson = new Gson();
        int[] rsEncodedContent = gson.fromJson(((JsonObject) frameData).get("value"), int[].class);
        int index = ((JsonObject) frameData).get("index").getAsInt();
        countFrames++;
        rsEncodedContent = Utils.changeNumBitsPerInt(rsEncodedContent, config.mainBlock.get(District.MAIN).getBitsPerUnit(), 8);
        int[][] frame = new int[numRegions][];
        for (int indexRegionOffset = 0, posOffset = 0; indexRegionOffset < numRegions; indexRegionOffset += config.numRegionHorizon, posOffset += numBytesPerRegion * config.numRegionHorizon) {
            for (int indexRegionInLine = 0; indexRegionInLine < config.numRegionHorizon; indexRegionInLine++) {
                int indexRegion = indexRegionOffset + indexRegionInLine;
                int pos = posOffset + indexRegionInLine * numBytesPerRegionLine;
                int[] regionData = new int[numBytesPerRegion];
                int regionDataPos = 0;
                for (int line = 0; line < numBytesPerRegion; line += numBytesPerRegionLine, pos += numBytesPerRegionLine * config.numRegionHorizon) {
                    System.arraycopy(rsEncodedContent, pos, regionData, regionDataPos, numBytesPerRegionLine);
                    regionDataPos += numBytesPerRegionLine;
                }
                try {
                    if (indexRegion == indexCenterBlock) {
                        Utils.rSDecode(regionData, 12, 8);
                    } else {
                        Utils.rSDecode(regionData, numRSBytes, 8);
                    }
                    frame[indexRegion] = new int[numRegionDataBytes];
                    System.arraycopy(regionData, 0, frame[indexRegion], 0, numRegionDataBytes);
                } catch (ReedSolomonException e) {
                    System.out.println("RS decode failed " + indexRegion);
                }
            }
        }

        List<Integer>[] interRegionParity = new List[3];
        interRegionParity[0] = new ArrayList<>();
        for (int i = 0; i < numRegions - 2; i++) {
            if (i != indexCenterBlock && ((numRegions - i - 3) % 4 < 2)) {
                interRegionParity[0].add(i);
            }
        }
        interRegionParity[1] = new ArrayList<>();
        for (int i = 0; i < numRegions; i++) {
            if (i != indexCenterBlock && (i % 2 == numRegions % 2)) {
                interRegionParity[1].add(i);
            }
        }
        interRegionParity[2] = new ArrayList<>();
        for (int i = 0; i < numRegions; i++) {
            if (i != indexCenterBlock) {
                interRegionParity[2].add(i);
            }
        }

        if (frame[indexCenterBlock] == null) {
            System.out.println("null center region");
        } else {
            int currentWindow = frame[indexCenterBlock][0];
            int currentFrame = frame[indexCenterBlock][1];
            if (numFileBytes == -1) {
                numFileBytes = (frame[indexCenterBlock][2] << 24) | (frame[indexCenterBlock][3] << 16) | (frame[indexCenterBlock][4] << 8) | frame[indexCenterBlock][5];
                numAllRegions = (int) Math.ceil(numFileBytes / (double) numRegionDataBytes);
                System.out.println("numFileBytes: " + numFileBytes + " numAllRegions: " + numAllRegions + " numRegionDataBytes: " + numRegionDataBytes);
            }
            realCurrentRegionOffset = (currentWindow * 7 + currentFrame) * (numDataRegions - 1);
            System.out.println("realCurrentRegionOffset: " + realCurrentRegionOffset);
            System.out.println("window " + currentWindow + " frame " + currentFrame + " data:" + Arrays.toString(frame));
            if (!windows.containsKey(currentWindow)) {
                windows.put(currentWindow, new int[8][numRegions][]);
            }
            int[][][] window = windows.get(currentWindow);
            for (int i = 0; i < frame.length; i++) {
                if (frame[i] != null) {
                    if (window[currentFrame][i] == null) {
                        window[currentFrame][i] = frame[i];
                        if (i < numDataRegions && i != indexCenterBlock && currentFrame < 7 && realCurrentRegionOffset + (i < indexCenterBlock ? i : i - 1) < numAllRegions) {
                            countAllRegions++;
                        }
                        if (numAllRegions == -1 || countAllRegions == numAllRegions) {
                            System.out.println("done in " + countFrames + " frames");
                            return;
                        }
                    }
                }
            }
            if (currentFrame != 7) {
                interRegionEC(interRegionParity, window, currentFrame, numRegionDataBytes);
            } else {
                interFrameEC(window, currentFrame, numRegionDataBytes, interRegionParity);
            }
        }
        System.out.println("progress: " + (double) countAllRegions / numAllRegions);
    }

    @Override
    public void afterStream(StreamDecode streamDecode) {
        if (numAllRegions == countAllRegions) {
            byte[] out = new byte[numFileBytes];
            int outPos = 0;
            int numRegionsPerWindow = 7 * numDataRegions;
            int i = 0;
            while (true) {
                int window = i / numRegionsPerWindow;
                int frame = i % numRegionsPerWindow / numDataRegions;
                int region = i % numRegionsPerWindow % numDataRegions;
                if (region != indexCenterBlock) {
                    int[] regionData = windows.get(window)[frame][region];
                    System.out.println("window: " + window + " frame: " + frame + " region: " + region + " bytes: " + (regionData == null ? "null" : regionData.length));
                    for (int pos = 0; pos < regionData.length && outPos < out.length; pos++, outPos++) {
                        out[outPos] = (byte) regionData[pos];
                    }
                    if (outPos == out.length) {
                        break;
                    }
                }
                i++;
            }
            String sha1 = FileVerification.bytesToSHA1(out);
            Log.d(TAG, "file SHA-1 verification: " + sha1);
            if (Utils.bytesToFile(out, streamDecode.outputFilePath)) {
                Log.i(TAG, "successfully write to " + streamDecode.outputFilePath);
            } else {
                Log.i(TAG, "failed to write to " + streamDecode.outputFilePath);
            }
        } else {
            Log.i(TAG, "file not complete");
        }
    }

    void interRegionEC(List<Integer>[] interRegionParity, int[][][] window, int currentFrame, int numRegionDataBytes) {
        for (int i = 0; i < interRegionParity.length; i++) {
            for (int j = 0; j < interRegionParity.length; j++) {
                int countErrorRegion = 0;
                int indexErrorRegion = -1;
                for (int k : interRegionParity[j]) {
                    if (window[currentFrame][k] == null) {
                        countErrorRegion++;
                        indexErrorRegion = k;
                    }
                }
                if (countErrorRegion == 1) {
                    int[] region = new int[numRegionDataBytes];
                    for (int k : interRegionParity[j]) {
                        if (k != indexErrorRegion) {
                            for (int pos = 0; pos < region.length; pos++) {
                                region[pos] ^= window[currentFrame][k][pos];
                            }
                        }
                    }
                    if (window[currentFrame][indexErrorRegion] == null) {
                        window[currentFrame][indexErrorRegion] = region;
                        System.out.println("inter region recover success index " + indexErrorRegion + " data:" + Arrays.toString(region));
                        if (indexErrorRegion < numDataRegions && indexErrorRegion != indexCenterBlock && currentFrame < 7 && realCurrentRegionOffset + i < numAllRegions) {
                            countAllRegions++;
                        }
                        if (numAllRegions == -1 || countAllRegions == numAllRegions) {
                            System.out.println("done in " + countFrames + " frames");
                            return;
                        }
                    }
                }
            }
        }
    }

    void interFrameEC(int[][][] window, int currentFrame, int numRegionDataBytes, List<Integer>[] interRegionParity) {
        for (int i = 0; i < window[currentFrame].length; i++) {
            if (window[currentFrame][i] != null) {
                int countErrorRegion = 0;
                int indexErrorFrame = -1;
                for (int j = 0; j < currentFrame; j++) {
                    if (window[j][i] == null) {
                        countErrorRegion++;
                        indexErrorFrame = j;
                    }
                }
                if (countErrorRegion == 1) {
                    int[] region = new int[numRegionDataBytes];
                    for (int j = 0; j <= currentFrame; j++) {
                        if (j != indexErrorFrame) {
                            for (int pos = 0; pos < region.length; pos++) {
                                region[pos] ^= window[j][i][pos];
                            }
                        }
                    }
                    if (window[indexErrorFrame][i] == null) {
                        window[indexErrorFrame][i] = region;
                        System.out.println("inter frame recover success frame " + indexErrorFrame + " region " + i + " data:" + Arrays.toString(region));
                        if (i < numDataRegions && i != indexCenterBlock && indexErrorFrame < 7 && realCurrentRegionOffset + i < numAllRegions) {
                            countAllRegions++;
                        }
                        if (countAllRegions == numAllRegions) {
                            System.out.println("done in " + countFrames + " frames");
                            return;
                        }
                        interRegionEC(interRegionParity, window, indexErrorFrame, numRegionDataBytes);
                    }
                }
            }
        }
    }
}
