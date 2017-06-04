package cn.edu.nju.cs.screencamera;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import cn.edu.nju.cs.screencamera.Logback.CustomMarker;

/**
 * Created by zhantong on 2017/6/4.
 */

public class RDCodeMLStream  extends StreamDecode{
    private static final String TAG="RDCodeMLStream";
    private static final boolean DUMP=true;
    private Map<DecodeHintType,?> hints;
    private ShiftCodeMLStream.Callback callback;

    static Logger LOG= LoggerFactory.getLogger(MainActivity.class);

    private List<int[]> randomIntArrayList=RDCodeML.randomBarcodeValue(new RDCodeMLConfig());
    private int[] rectangle=null;
    public interface Callback{
        void onBarcodeNotFound();
        void onCRCCheckFailed();
        void onBeforeDataDecoded();
    }
    public RDCodeMLStream(Map<DecodeHintType,?> hints){
        this.hints=hints;

    }
    public void setCallback(ShiftCodeMLStream.Callback callback){
        this.callback=callback;
    }
    public void stream(LinkedBlockingQueue<RawImage> frames) throws InterruptedException{
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
                mediateBarcode = new MediateBarcode(frame,new RDCodeMLConfig(),rectangle,RawImage.CHANNLE_Y);
            } catch (NotFoundException e) {
                Log.i(TAG,"barcode not found");
                if(callback!=null){
                    callback.onBarcodeNotFound();
                }
                continue;
            }
            RDCodeML rDCodeML=new RDCodeML(mediateBarcode,hints);
            rDCodeML.mediateBarcode.getContent(rDCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN),RawImage.CHANNLE_U);
            rDCodeML.mediateBarcode.getContent(rDCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN),RawImage.CHANNLE_V);
            int overlapSituation=rDCodeML.getOverlapSituation();
            if(DUMP) {
                JsonObject mainJson=rDCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN).toJson();
                barcodeJson.add("barcode",mainJson);
                barcodeJson.addProperty("index",rDCodeML.mediateBarcode.rawImage.getIndex());
                JsonObject varyBarJson=rDCodeML.getVaryBarToJson();
                barcodeJson.add("varyBar",varyBarJson);
                barcodeJson.addProperty("overlapSituation",overlapSituation);
                barcodeJson.addProperty("isRandom",rDCodeML.getIsRandom());
                //String jsonString=new Gson().toJson(root);
                //LOG.info(CustomMarker.source,jsonString);
            }
            if(rDCodeML.getIsRandom()){
                try {
                    try {
                        int index = rDCodeML.getTransmitFileLengthInBytes();
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
            if(DUMP){
                LOG.info(CustomMarker.processed,new Gson().toJson(barcodeJson));
            }
            //rectangle=zoomRectangle(mediateBarcode.getRectangle());
        }
    }
}
