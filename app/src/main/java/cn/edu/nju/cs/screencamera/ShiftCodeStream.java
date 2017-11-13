package cn.edu.nju.cs.screencamera;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.parameters.FECParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.edu.nju.cs.screencamera.Logback.CustomMarker;
import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

/**
 * Created by zhantong on 2016/11/27.
 */

public class ShiftCodeStream extends BlackWhiteCodeStream {
    private static final String TAG = "ShiftCodeStream";
    static Logger LOG = LoggerFactory.getLogger(MainActivity.class);

    private boolean DUMP = false;

    BarcodeConfig getBarcodeConfigInstance() {
        return new BlackWhiteCodeConfig();
    }

    ShiftCode getShiftCode(MediateBarcode mediateBarcode) {
        return new ShiftCode(mediateBarcode);
    }

    @Override
    public void processFrame(StreamDecode streamDecode, RawImage frame) {
        //Utils.dumpFile(Environment.getExternalStorageDirectory().toString()+"/"+frame.getIndex()+".yuv",frame.getPixels());
        Log.i(TAG, frame.toString());

        MediateBarcode mediateBarcode;
        try {
            mediateBarcode = new MediateBarcode(frame, getBarcodeConfigInstance(), null, RawImage.CHANNLE_Y);
        } catch (NotFoundException e) {
            Log.i(TAG, "barcode not found: " + e.toString());
            if (streamDecode.getIsCamera()) {
                streamDecode.focusCamera();
            }
            return;
        }
        ShiftCode shiftCode = getShiftCode(mediateBarcode);
        if (raptorQSymbolSize == -1) {
            raptorQSymbolSize = shiftCode.calcRaptorQSymbolSize(shiftCode.calcRaptorQPacketSize());
        }
        if (dataDecoder == null) {
            try {
                int head = shiftCode.getTransmitFileLengthInBytes();
                int numSourceBlock = Integer.parseInt(barcodeConfig.hints.get(ShiftCode.KEY_NUMBER_RAPTORQ_SOURCE_BLOCKS).toString());
                FECParameters parameters = FECParameters.newParameters(head, raptorQSymbolSize, numSourceBlock);
                System.out.println("FECParameters: " + parameters.toString());
                Log.i(TAG, "data length: " + parameters.dataLengthAsInt() + " symbol length: " + parameters.symbolSize());
                dataDecoder = OpenRQ.newDecoder(parameters, 0);
            } catch (CRCCheckException e) {
                e.printStackTrace();
                if (streamDecode.getIsCamera()) {
                    streamDecode.focusCamera();
                }
                return;
            }
        }
        int overlapSituation = shiftCode.getOverlapSituation();
        int[][] rawContents = shiftCode.getMixedRawContent();
        if (DUMP) {
            JsonObject barcodeJson = new JsonObject();
            JsonObject mainJson = shiftCode.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN).toJson();
            barcodeJson.add("barcode", mainJson);
            barcodeJson.addProperty("index", shiftCode.mediateBarcode.rawImage.getIndex());
            barcodeJson.addProperty("timestamp", frame.getTimestamp());
            //int[][] temp=new int[][]{Utils.changeNumBitsPerInt(rawContents[0],2,12),Utils.changeNumBitsPerInt(rawContents[1],4,12)};

            //barcodeJson.add("results", new Gson().toJsonTree(temp));
            LOG.info(CustomMarker.source, new Gson().toJson(barcodeJson));
        }
        for (int[] rawContent : rawContents) {
            int[] rSDecodedData;
            try {
                rSDecodedData = shiftCode.rSDecode(rawContent, shiftCode.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN));
            } catch (ReedSolomonException e) {
                Log.i(TAG, "RS decode failed");
                continue;
            }
            Log.i(TAG, "RS decode success");
            byte[] raptorQEncodedData = Utils.intArrayToByteArray(rSDecodedData, rSDecodedData.length, rsEcSize, shiftCode.calcRaptorQPacketSize());
            //Log.i(TAG,"raptorq encoded data length: "+raptorQEncodedData.length);
            //Log.i(TAG,"raptorq encoded data: "+Arrays.toString(raptorQEncodedData));
            EncodingPacket encodingPacket = dataDecoder.parsePacket(raptorQEncodedData, true).value();
            if (DUMP) {
                JsonObject barcodeJson = new JsonObject();
                barcodeJson.addProperty("index", shiftCode.mediateBarcode.rawImage.getIndex());
                barcodeJson.addProperty("esi", encodingPacket.encodingSymbolID());
                barcodeJson.addProperty("type", encodingPacket.symbolType().name());
                LOG.info(CustomMarker.processed, new Gson().toJson(barcodeJson));
            }
            Log.i(TAG, "encoding packet: source block number: " + encodingPacket.sourceBlockNumber() + " " + encodingPacket.encodingSymbolID() + " " + encodingPacket.symbolType() + " " + encodingPacket.numberOfSymbols());
            if (isLastEncodingPacket(encodingPacket)) {
                Log.i(TAG, "last encoding packet: " + encodingPacket.encodingSymbolID());
                streamDecode.setStopQueue();
            }
            dataDecoder.sourceBlock(encodingPacket.sourceBlockNumber()).putEncodingPacket(encodingPacket);
        }
    }
}
