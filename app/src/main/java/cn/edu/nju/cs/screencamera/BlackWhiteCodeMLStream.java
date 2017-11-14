package cn.edu.nju.cs.screencamera;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.fec.openrq.parameters.FECParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import cn.edu.nju.cs.screencamera.Logback.CustomMarker;

/**
 * Created by zhantong on 2017/5/11.
 */

public class BlackWhiteCodeMLStream implements StreamDecode.CallBack {
    private static final String TAG = "BlackWhiteCodeMLStream";
    private static final boolean DUMP = true;
    int transmitFileLengthInBytes = -1;
    int numRandomBarcode;
    BarcodeConfig barcodeConfig;

    static Logger LOG = LoggerFactory.getLogger(FileActivity.class);

    List<int[]> randomIntArrayList;

    public BlackWhiteCodeMLStream() {
        barcodeConfig = getBarcodeConfigInstance();
        numRandomBarcode = Integer.parseInt(barcodeConfig.hints.get(BlackWhiteCodeML.KEY_NUMBER_RANDOM_BARCODES).toString());
        randomIntArrayList = getBarcodeInstance(new MediateBarcode(getBarcodeConfigInstance())).randomBarcodeValue(getBarcodeConfigInstance(), numRandomBarcode);
    }

    BlackWhiteCodeML getBarcodeInstance(MediateBarcode mediateBarcode) {
        return new BlackWhiteCodeML(mediateBarcode);
    }

    BarcodeConfig getBarcodeConfigInstance() {
        return new BlackWhiteCodeMLConfig();
    }

    void sampleContent(BlackWhiteCodeML blackWhiteCodeML) {
        blackWhiteCodeML.mediateBarcode.getContent(blackWhiteCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN), RawImage.CHANNLE_Y);
    }

    MediateBarcode getMediateBarcode(RawImage rawImage) {
        try {
            return new MediateBarcode(rawImage, getBarcodeConfigInstance(), null, RawImage.CHANNLE_Y);
        } catch (NotFoundException e) {
            return null;
        }
    }

    @Override
    public void beforeStream(StreamDecode streamDecode) {
        LOG.info(CustomMarker.barcodeConfig, new Gson().toJson(getBarcodeConfigInstance().toJson()));
    }

    @Override
    public void processFrame(StreamDecode streamDecode, RawImage frame) {
        Gson gson = new Gson();
        JsonObject jsonRoot = new JsonObject();
        if (frame.getPixels() == null) {
            return;
        }
        Log.i(TAG, frame.toString());
        MediateBarcode mediateBarcode = getMediateBarcode(frame);
        if (DUMP) {
            jsonRoot.add("image", frame.toJson());
        }
        if (mediateBarcode != null) {
            BlackWhiteCodeML blackWhiteCodeML = getBarcodeInstance(mediateBarcode);
            sampleContent(blackWhiteCodeML);
            if (DUMP) {
                jsonRoot.add("barcode", blackWhiteCodeML.toJson());
            }
            if (blackWhiteCodeML.getIsRandom()) {
                int index = Integer.MAX_VALUE;
                try {
                    index = blackWhiteCodeML.getTransmitFileLengthInBytes();
                } catch (CRCCheckException e) {
                    e.printStackTrace();
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                if (index < numRandomBarcode) {
                    JsonObject randomJsonRoot = new JsonObject();
                    randomJsonRoot.addProperty("index", index);
                    randomJsonRoot.add("truthValue", gson.toJsonTree(randomIntArrayList.get(index)));
                    jsonRoot.add("random", randomJsonRoot);
                }
            } else {
                if (transmitFileLengthInBytes == -1) {
                    try {
                        transmitFileLengthInBytes = blackWhiteCodeML.getTransmitFileLengthInBytes();
                    } catch (CRCCheckException e) {
                        e.printStackTrace();
                    }
                    if (transmitFileLengthInBytes != -1) {
                        int raptorQSymbolSize = blackWhiteCodeML.calcRaptorQSymbolSize(blackWhiteCodeML.calcRaptorQPacketSize());
                        int numSourceBlock = Integer.parseInt(barcodeConfig.hints.get(BlackWhiteCodeML.KEY_NUMBER_RAPTORQ_SOURCE_BLOCKS).toString());
                        FECParameters parameters = FECParameters.newParameters(transmitFileLengthInBytes, raptorQSymbolSize, numSourceBlock);
                        if (DUMP) {
                            LOG.info(CustomMarker.fecParameters, new Gson().toJson(Utils.fecParametersToJson(parameters)));
                        }
                    }
                }
            }
        }
        if (DUMP) {
            LOG.info(CustomMarker.processed, new Gson().toJson(jsonRoot));
        }
    }

    @Override
    public void processFrame(StreamDecode streamDecode, JsonElement frameData) {

    }

    @Override
    public void afterStream(StreamDecode streamDecode) {

    }
}
