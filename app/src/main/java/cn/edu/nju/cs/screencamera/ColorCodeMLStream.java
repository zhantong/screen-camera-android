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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import cn.edu.nju.cs.screencamera.Logback.CustomMarker;

/**
 * Created by zhantong on 2017/5/15.
 */

public class ColorCodeMLStream extends StreamDecode {
    private static final String TAG="BlackWhiteCodeMLStream";
    private static final boolean DUMP=true;
    private Map<DecodeHintType,?> hints;
    private ArrayDataDecoder dataDecoder=null;
    private int raptorQSymbolSize =-1;
    private ShiftCodeMLStream.Callback callback;

    static Logger LOG= LoggerFactory.getLogger(MainActivity.class);

    private List<int[]> randomIntArrayList=ColorCodeML.randomBarcodeValue(new ColorCodeMLConfig());
    private int[] rectangle=null;
    public interface Callback{
        void onBarcodeNotFound();
        void onCRCCheckFailed();
        void onBeforeDataDecoded();
    }
    public ColorCodeMLStream(Map<DecodeHintType,?> hints){
        this.hints=hints;

    }
    public void setCallback(ShiftCodeMLStream.Callback callback){
        this.callback=callback;
    }
    public void stream(LinkedBlockingQueue<RawImage> frames) throws InterruptedException{
        int rsEcSize=-1;
        if(hints!=null){
            rsEcSize=Integer.parseInt(hints.get(DecodeHintType.RS_ERROR_CORRECTION_SIZE).toString());
        }
        for(RawImage frame;(frame=frames.poll(4, TimeUnit.SECONDS))!=null;){
            JsonObject barcodeJson=new JsonObject();
            if(frame.getPixels()==null){
                break;
            }
            System.out.println("queue size: "+frames.size());
            //Utils.dumpFile(Environment.getExternalStorageDirectory().toString()+"/"+frame.getIndex()+".yuv",frame.getPixels());
            Log.i(TAG,frame.toString());

            MediateBarcode mediateBarcode;
            try {
                mediateBarcode = new MediateBarcode(frame,new ColorCodeMLConfig(),rectangle,RawImage.CHANNLE_Y);
            } catch (NotFoundException e) {
                Log.i(TAG,"barcode not found");
                if(callback!=null){
                    callback.onBarcodeNotFound();
                }
                continue;
            }
            ColorCodeML colorCodeML=new ColorCodeML(mediateBarcode,hints);
            colorCodeML.mediateBarcode.getContent(colorCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN),RawImage.CHANNLE_U);
            colorCodeML.mediateBarcode.getContent(colorCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN),RawImage.CHANNLE_V);
            int overlapSituation=colorCodeML.getOverlapSituation();
            if(DUMP) {
                JsonObject mainJson=colorCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN).toJson();
                barcodeJson.add("barcode",mainJson);
                barcodeJson.addProperty("index",colorCodeML.mediateBarcode.rawImage.getIndex());
                JsonObject varyBarJson=colorCodeML.getVaryBarToJson();
                barcodeJson.add("varyBar",varyBarJson);
                barcodeJson.addProperty("overlapSituation",overlapSituation);
                barcodeJson.addProperty("isRandom",colorCodeML.getIsRandom());
                //String jsonString=new Gson().toJson(root);
                //LOG.info(CustomMarker.source,jsonString);
            }
            if(colorCodeML.getIsRandom()){
                if(true) {
                    try {
                        try {
                            int index = colorCodeML.getTransmitFileLengthInBytes();
                            System.out.println("random index: " + index);
                            barcodeJson.addProperty("randomIndex",index);
                            if (index >= BlackWhiteCodeML.numRandomBarcode) {
                                continue;
                            }
                            int[] value = randomIntArrayList.get(index);
                            if (DUMP) {
                                barcodeJson.add("value", new Gson().toJsonTree(value));
                                //root.addProperty("index",shiftCodeML.mediateBarcode.rawImage.getIndex());
                                //LOG.info(CustomMarker.processed,new Gson().toJson(barcodeJson));
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }

                    } catch (CRCCheckException e) {
                        e.printStackTrace();
                    }
                }
            }else{
                if(raptorQSymbolSize ==-1){
                    raptorQSymbolSize =colorCodeML.calcRaptorQSymbolSize(colorCodeML.calcRaptorQPacketSize());
                }
                if(dataDecoder==null){
                    try {
                        int head = colorCodeML.getTransmitFileLengthInBytes();
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
                        if(callback!=null){
                            callback.onCRCCheckFailed();
                        }
                        continue;
                    }
                }
            }
            if(DUMP){
                LOG.info(CustomMarker.processed,new Gson().toJson(barcodeJson));
            }
            //rectangle=zoomRectangle(mediateBarcode.getRectangle());
        }
    }
    private static int[] zoomRectangle(int[] originRectangle){
        int left=originRectangle[0];
        int up=originRectangle[1];
        int right=originRectangle[2];
        int down=originRectangle[3];
        left+=(left/5);
        up+=(up/5);
        right-=(right/5);
        down-=(down/5);
        return new int[]{left,up,right,down};
    }
}
