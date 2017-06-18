package cn.edu.nju.cs.screencamera;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.parameters.FECParameters;
import net.fec.openrq.parameters.SerializableParameters;

import java.util.Arrays;
import java.util.Map;

import cn.edu.nju.cs.screencamera.Logback.CustomMarker;
import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

/**
 * Created by zhantong on 2017/6/18.
 */

public class BlackWhiteCodeMLFile extends BlackWhiteCodeStream{
    private static final String TAG="BlackWhiteCodeMLFile";
    private static final boolean DUMP=false;
    Map<DecodeHintType,?> hints;
    private BlackWhiteCodeML blackWhiteCodeML;


    public BlackWhiteCodeMLFile(Map<DecodeHintType,?> hints){
        super(hints);
        blackWhiteCodeML=new BlackWhiteCodeML(new MediateBarcode(new BlackWhiteCodeMLConfig()),hints);
    }

    @Override
    protected void beforeStream() {
        super.beforeStream();
        JsonObject raptorQMeta=(JsonObject)jsonRoot.get("raptorQMeta");
        long commonOTI=raptorQMeta.get("commonOTI").getAsLong();
        int schemeSpecificOTI=raptorQMeta.get("schemeSpecificOTI").getAsInt();
        SerializableParameters serializableParameters=new SerializableParameters(commonOTI,schemeSpecificOTI);
        FECParameters parameters=FECParameters.parse(serializableParameters).value();
        dataDecoder= OpenRQ.newDecoder(parameters,0);
    }

    @Override
    protected void processFrame(int[] frameData) {
        JsonObject barcodeJson=new JsonObject();
        int[] rSDecodedData;
        try {
            rSDecodedData = blackWhiteCodeML.rSDecode(frameData,blackWhiteCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN));
        } catch (ReedSolomonException e) {
            Log.i(TAG,"RS decode failed");
            return;
        }
        Log.i(TAG,"RS decode success");
        byte[] raptorQEncodedData=Utils.intArrayToByteArray(rSDecodedData,rSDecodedData.length,rsEcSize, blackWhiteCodeML.calcRaptorQPacketSize());
        Log.i(TAG,"raptorq encoded data length: "+raptorQEncodedData.length);
        Log.i(TAG,"raptorq encoded data: "+ Arrays.toString(raptorQEncodedData));
        try {
            EncodingPacket encodingPacket = dataDecoder.parsePacket(raptorQEncodedData, true).value();
//                if(DUMP){
//                    JsonObject barcodeJson=new JsonObject();
//                    barcodeJson.addProperty("index",blackWhiteCodeWithBar.mediateBarcode.rawImage.getIndex());
//                    barcodeJson.addProperty("esi",encodingPacket.encodingSymbolID());
//                    barcodeJson.addProperty("type",encodingPacket.symbolType().name());
//                    LOG.info(CustomMarker.processed,new Gson().toJson(barcodeJson));
//                }
            Log.i(TAG, "encoding packet: source block number: " + encodingPacket.sourceBlockNumber() + " " + encodingPacket.encodingSymbolID() + " " + encodingPacket.symbolType() + " " + encodingPacket.numberOfSymbols());
            if (isLastEncodingPacket(encodingPacket)) {
                Log.i(TAG, "last encoding packet: " + encodingPacket.encodingSymbolID());
                setStopQueue();
            }
            dataDecoder.sourceBlock(encodingPacket.sourceBlockNumber()).putEncodingPacket(encodingPacket);
        }catch (Exception e){
            e.printStackTrace();
            return;
        }

        if(DUMP){
            LOG.info(CustomMarker.processed,new Gson().toJson(barcodeJson));
        }
    }
}
