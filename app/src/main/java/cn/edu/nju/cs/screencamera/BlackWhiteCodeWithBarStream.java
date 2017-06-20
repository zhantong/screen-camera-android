package cn.edu.nju.cs.screencamera;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.parameters.FECParameters;
import net.fec.openrq.parameters.SerializableParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;

import cn.edu.nju.cs.screencamera.Logback.CustomMarker;
import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

/**
 * Created by zhantong on 2017/5/24.
 */

public class BlackWhiteCodeWithBarStream extends BlackWhiteCodeStream{
    private static final String TAG="ShiftCodeMLStream";
    private static final boolean DUMP=false;

    static Logger LOG= LoggerFactory.getLogger(MainActivity.class);


    public BlackWhiteCodeWithBarStream() {
    }
    BarcodeConfig getBarcodeConfigInstance(){
        return new BlackWhiteCodeWithBarConfig();
    }
    @Override
    protected void processFrame(RawImage frame) {
        JsonObject barcodeJson=new JsonObject();
        if(frame.getPixels()==null){
            return;
        }
        Log.i(TAG,frame.toString());

        MediateBarcode mediateBarcode;
        try {
            mediateBarcode = new MediateBarcode(frame,new BlackWhiteCodeWithBarConfig(),null,RawImage.CHANNLE_Y);
        } catch (NotFoundException e) {
            Log.i(TAG,"barcode not found");
            return;
        }
        BlackWhiteCodeWithBar blackWhiteCodeWithBar =new BlackWhiteCodeWithBar(mediateBarcode);
        blackWhiteCodeWithBar.mediateBarcode.getContent(blackWhiteCodeWithBar.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN),RawImage.CHANNLE_Y);
        int overlapSituation= blackWhiteCodeWithBar.getOverlapSituation();
        if(DUMP) {
            JsonObject mainJson= blackWhiteCodeWithBar.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN).toJson();
            barcodeJson.add("barcode",mainJson);
            barcodeJson.addProperty("index", blackWhiteCodeWithBar.mediateBarcode.rawImage.getIndex());
            //JsonObject varyBarJson=shiftCodeML.getVaryBarToJson();
            //barcodeJson.add("varyBar",varyBarJson);
            barcodeJson.addProperty("overlapSituation",overlapSituation);
            //barcodeJson.addProperty("isRandom",shiftCodeML.getIsRandom());
            //String jsonString=new Gson().toJson(root);
            //LOG.info(CustomMarker.source,jsonString);
        }

        if(raptorQSymbolSize ==-1){
            raptorQSymbolSize = blackWhiteCodeWithBar.calcRaptorQSymbolSize(blackWhiteCodeWithBar.calcRaptorQPacketSize());
        }
        if(dataDecoder==null){
            try {
                int head = blackWhiteCodeWithBar.getTransmitFileLengthInBytes();
                int numSourceBlock=Integer.parseInt(barcodeConfig.hints.get(BlackWhiteCode.KEY_NUMBER_RAPTORQ_SOURCE_BLOCKS).toString());
                FECParameters parameters=FECParameters.newParameters(head, raptorQSymbolSize,numSourceBlock);
                if(DUMP){
                    JsonObject paramsJson=new JsonObject();
                    SerializableParameters serializableParameters= parameters.asSerializable();
                    paramsJson.addProperty("commonOTI",serializableParameters.commonOTI());
                    paramsJson.addProperty("schemeSpecificOTI",serializableParameters.schemeSpecificOTI());
                    LOG.info(CustomMarker.raptorQMeta,new Gson().toJson(paramsJson));
                }
                System.out.println("FECParameters: "+parameters.toString());
                Log.i(TAG,"data length: "+parameters.dataLengthAsInt()+" symbol length: "+parameters.symbolSize());
                dataDecoder= OpenRQ.newDecoder(parameters,0);
            } catch (CRCCheckException e) {
                e.printStackTrace();
                return;
            }
        }
        int[][] rawContents= blackWhiteCodeWithBar.getMixedRawContent();
        for(int[] rawContent:rawContents){
            int[] rSDecodedData;
            try {
                rSDecodedData = blackWhiteCodeWithBar.rSDecode(rawContent, blackWhiteCodeWithBar.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN));
            } catch (ReedSolomonException e) {
                Log.i(TAG,"RS decode failed");
                continue;
            }
            Log.i(TAG,"RS decode success");
            byte[] raptorQEncodedData=Utils.intArrayToByteArray(rSDecodedData,rSDecodedData.length,rsEcSize, blackWhiteCodeWithBar.calcRaptorQPacketSize());
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
                continue;
            }
        }
        if(DUMP){
            LOG.info(CustomMarker.processed,new Gson().toJson(barcodeJson));
        }
    }
}
