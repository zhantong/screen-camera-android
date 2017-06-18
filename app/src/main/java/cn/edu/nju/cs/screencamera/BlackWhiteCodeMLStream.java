package cn.edu.nju.cs.screencamera;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.parameters.FECParameters;
import net.fec.openrq.parameters.SerializableParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import cn.edu.nju.cs.screencamera.Logback.CustomMarker;

/**
 * Created by zhantong on 2017/5/11.
 */

public class BlackWhiteCodeMLStream extends StreamDecode {
    private static final String TAG="BlackWhiteCodeMLStream";
    private static final boolean DUMP=true;
    private Map<DecodeHintType,?> hints;
    private ArrayDataDecoder dataDecoder=null;
    private int raptorQSymbolSize =-1;
    private int numRandomBarcode=100;

    static Logger LOG= LoggerFactory.getLogger(MainActivity.class);

    private List<int[]> randomIntArrayList;

    public BlackWhiteCodeMLStream(Map<DecodeHintType,?> hints){
        this.hints=hints;
        if(hints!=null){
            if(hints.containsKey(DecodeHintType.NUMBER_OF_RANDOM_BARCODES)){
                numRandomBarcode=Integer.parseInt(hints.get(DecodeHintType.NUMBER_OF_RANDOM_BARCODES).toString());
            }
        }
        randomIntArrayList=BlackWhiteCodeML.randomBarcodeValue(new BlackWhiteCodeMLConfig(),numRandomBarcode);
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
            mediateBarcode = new MediateBarcode(frame,new BlackWhiteCodeMLConfig(),null,RawImage.CHANNLE_Y);
        } catch (NotFoundException e) {
            Log.i(TAG,"barcode not found");
            return;
        }
        BlackWhiteCodeML blackWhiteCodeML=new BlackWhiteCodeML(mediateBarcode,hints);
        blackWhiteCodeML.mediateBarcode.getContent(blackWhiteCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN),RawImage.CHANNLE_Y);
        int overlapSituation=blackWhiteCodeML.getOverlapSituation();
        if(DUMP) {
            JsonObject mainJson=blackWhiteCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN).toJson();
            barcodeJson.add("barcode",mainJson);
            barcodeJson.addProperty("index",blackWhiteCodeML.mediateBarcode.rawImage.getIndex());
            JsonObject varyBarJson=blackWhiteCodeML.getVaryBarToJson();
            barcodeJson.add("varyBar",varyBarJson);
            barcodeJson.addProperty("overlapSituation",overlapSituation);
            barcodeJson.addProperty("isRandom",blackWhiteCodeML.getIsRandom());
            //String jsonString=new Gson().toJson(root);
            //LOG.info(CustomMarker.source,jsonString);
        }
        if(blackWhiteCodeML.getIsRandom()){
            try {
                int index = blackWhiteCodeML.getTransmitFileLengthInBytes();
                System.out.println("random index: " + index);
                barcodeJson.addProperty("randomIndex",index);
                if (index >= numRandomBarcode) {
                    return;
                }
                int[] value = randomIntArrayList.get(index);
                if (DUMP) {
                    barcodeJson.add("value", new Gson().toJsonTree(value));
                    //root.addProperty("index",shiftCodeML.mediateBarcode.rawImage.getIndex());
                    //LOG.info(CustomMarker.processed,new Gson().toJson(barcodeJson));
                }
            } catch (NumberFormatException e) {

            }catch (CRCCheckException e){
                e.printStackTrace();
            }
        }else{
            if(raptorQSymbolSize ==-1){
                raptorQSymbolSize =blackWhiteCodeML.calcRaptorQSymbolSize(blackWhiteCodeML.calcRaptorQPacketSize());
            }
            if(dataDecoder==null){
                try {
                    int head = blackWhiteCodeML.getTransmitFileLengthInBytes();
                    int numSourceBlock=0;
                    if(hints!=null){
                        numSourceBlock=Integer.parseInt(hints.get(DecodeHintType.RAPTORQ_NUMBER_OF_SOURCE_BLOCKS).toString());
                    }
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
        }
        if(DUMP){
            LOG.info(CustomMarker.processed,new Gson().toJson(barcodeJson));
        }
    }
}
