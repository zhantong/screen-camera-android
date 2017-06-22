package cn.edu.nju.cs.screencamera;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.ParsingFailureException;
import net.fec.openrq.parameters.FECParameters;
import net.fec.openrq.parameters.SerializableParameters;


import cn.edu.nju.cs.screencamera.Logback.CustomMarker;

/**
 * Created by zhantong on 2017/6/18.
 */

public class BlackWhiteCodeMLFile extends BlackWhiteCodeStream{
    private static final String TAG="BlackWhiteCodeMLFile";
    private static final boolean DUMP=true;
    private BlackWhiteCodeML blackWhiteCodeML;


    public BlackWhiteCodeMLFile(){
        blackWhiteCodeML=getBarcodeInstance(new MediateBarcode(getBarcodeConfigInstance()));
    }
    BlackWhiteCodeML getBarcodeInstance(MediateBarcode mediateBarcode){
        return new BlackWhiteCodeML(mediateBarcode);
    }
    BarcodeConfig getBarcodeConfigInstance(){
        return new BlackWhiteCodeMLConfig();
    }
    @Override
    protected void beforeStream() {
        super.beforeStream();
        JsonObject raptorQMeta=(JsonObject) inputJsonRoot.get("fecParameters");
        long commonOTI=raptorQMeta.get("commonOTI").getAsLong();
        int schemeSpecificOTI=raptorQMeta.get("schemeSpecificOTI").getAsInt();
        SerializableParameters serializableParameters=new SerializableParameters(commonOTI,schemeSpecificOTI);
        FECParameters parameters=FECParameters.parse(serializableParameters).value();
        LOG.info(CustomMarker.fecParameters,new Gson().toJson(Utils.fecParametersToJson(parameters)));
        dataDecoder= OpenRQ.newDecoder(parameters,0);
    }

    @Override
    protected void processFrame(int[] frameData) {
        Gson gson=new Gson();
        JsonObject jsonRoot=new JsonObject();
        JsonObject rsJsonRoot=new JsonObject();
        if(DUMP){
            rsJsonRoot.add("rsEncodedContent",gson.toJsonTree(frameData));
        }
        int[] rsDecodedContent=rsDecode(blackWhiteCodeML,frameData);
        JsonObject raptorQJsonRoot=new JsonObject();
        if(rsDecodedContent!=null) {
            if(DUMP){
                rsJsonRoot.add("rsDecodedContent",gson.toJsonTree(rsDecodedContent));
            }
            byte[] raptorQEncodedData = getRaptorQEncodedData(blackWhiteCodeML, rsDecodedContent);
            EncodingPacket encodingPacket=null;
            try {
                encodingPacket = dataDecoder.parsePacket(raptorQEncodedData, true).value();
            }catch (ParsingFailureException e){
                e.printStackTrace();
            }

            if(encodingPacket!=null) {
                raptorQJsonRoot.add("encodingPacket",Utils.encodingPacketToJson(encodingPacket));
                if (isLastEncodingPacket(encodingPacket)) {
                    Log.i(TAG, "last encoding packet: " + encodingPacket.encodingSymbolID());
                    setStopQueue();
                }
                dataDecoder.sourceBlock(encodingPacket.sourceBlockNumber()).putEncodingPacket(encodingPacket);
            }
        }
        raptorQJsonRoot.add("decoder",Utils.sourceBlockDecoderToJson(dataDecoder.sourceBlock(0)));
        jsonRoot.add("rs",rsJsonRoot);
        jsonRoot.add("raptorQ",raptorQJsonRoot);
        if(DUMP){
            LOG.info(CustomMarker.processed,new Gson().toJson(jsonRoot));
        }
    }
}
