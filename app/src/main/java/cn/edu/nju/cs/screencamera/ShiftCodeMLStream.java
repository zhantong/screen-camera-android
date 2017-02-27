package cn.edu.nju.cs.screencamera;

import android.os.Environment;
import android.util.Log;
import android.util.Pair;
import android.util.SparseIntArray;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.SymbolType;
import net.fec.openrq.decoder.SourceBlockDecoder;
import net.fec.openrq.parameters.FECParameters;
import net.fec.openrq.parameters.SerializableParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import cn.edu.nju.cs.screencamera.Logback.CustomMarker;
import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

/**
 * Created by zhantong on 2016/11/28.
 */

public class ShiftCodeMLStream extends StreamDecode{
    private static final String TAG="ShiftCodeMLStream";
    private static final boolean DUMP=true;
    private Map<DecodeHintType,?> hints;
    private ArrayDataDecoder dataDecoder=null;
    private int raptorQSymbolSize =-1;
    private Callback callback;

    static Logger LOG= LoggerFactory.getLogger(MainActivity.class);

    private List<int[]> randomIntArrayList=ShiftCodeML.randomBarcodeValue(new ShiftCodeMLConfig());

    public interface Callback{
        void onBarcodeNotFound();
        void onCRCCheckFailed();
        void onBeforeDataDecoded();
    }

    public ShiftCodeMLStream(Map<DecodeHintType,?> hints){
        this.hints=hints;
    }
    public void setCallback(Callback callback){
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
                mediateBarcode = new MediateBarcode(frame,new ShiftCodeMLConfig(),null,RawImage.CHANNLE_Y);
            } catch (NotFoundException e) {
                Log.i(TAG,"barcode not found");
                if(callback!=null){
                    callback.onBarcodeNotFound();
                }
                continue;
            }
            ShiftCodeML shiftCodeML=new ShiftCodeML(mediateBarcode,hints);
            shiftCodeML.mediateBarcode.getContent(shiftCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN),RawImage.CHANNLE_Y);
            if(DUMP) {
                JsonObject mainJson=shiftCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN).toJson();
                barcodeJson.add("barcode",mainJson);
                barcodeJson.addProperty("index",shiftCodeML.mediateBarcode.rawImage.getIndex());
                JsonObject varyBarJson=shiftCodeML.getVaryBarToJson();
                barcodeJson.add("varyBar",varyBarJson);
                //String jsonString=new Gson().toJson(root);
                //LOG.info(CustomMarker.source,jsonString);
            }
            if(shiftCodeML.getIsRandom()){
                try {
                    int index = shiftCodeML.getTransmitFileLengthInBytes();
                    System.out.println("random index: "+index);
                    int[] value=randomIntArrayList.get(index);
                    if(DUMP){
                        barcodeJson.add("value",new Gson().toJsonTree(value));
                        //root.addProperty("index",shiftCodeML.mediateBarcode.rawImage.getIndex());
                        //LOG.info(CustomMarker.processed,new Gson().toJson(barcodeJson));
                    }
                }catch (CRCCheckException e){
                    e.printStackTrace();
                }
            }else{
                if(raptorQSymbolSize ==-1){
                    raptorQSymbolSize =shiftCodeML.calcRaptorQSymbolSize(shiftCodeML.calcRaptorQPacketSize());
                }
                if(dataDecoder==null){
                    try {
                        int head = shiftCodeML.getTransmitFileLengthInBytes();
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
        }
        if(dataDecoder!=null&&dataDecoder.isDataDecoded()){
            Log.i(TAG,"RaptorQ decode success");
            writeRaptorQDataFile(dataDecoder,"out.txt");
        }
    }
    private boolean isLastEncodingPacket(EncodingPacket encodingPacket){
        SourceBlockDecoder sourceBlock=dataDecoder.sourceBlock(encodingPacket.sourceBlockNumber());
        return (sourceBlock.missingSourceSymbols().size()-sourceBlock.availableRepairSymbols().size()==1)
                &&((encodingPacket.symbolType()== SymbolType.SOURCE&&!sourceBlock.containsSourceSymbol(encodingPacket.encodingSymbolID()))
                ||(encodingPacket.symbolType()== SymbolType.REPAIR&&!sourceBlock.containsRepairSymbol(encodingPacket.encodingSymbolID())));
    }
    private void writeRaptorQDataFile(ArrayDataDecoder decoder,String fileName){
        byte[] out = decoder.dataArray();
        String sha1 = FileVerification.bytesToSHA1(out);
        Log.d(TAG, "file SHA-1 verification: " + sha1);
        bytesToFile(out, fileName);
    }
    private boolean bytesToFile(byte[] bytes,String fileName){
        if(fileName.isEmpty()){
            Log.i(TAG, "file name is empty");
            return false;
        }
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),fileName);
        OutputStream os;
        try {
            os = new FileOutputStream(file);
            os.write(bytes);
            os.close();
        } catch (FileNotFoundException e) {
            Log.i(TAG, "file path error, cannot create file:" + e.toString());
            return false;
        }catch (IOException e){
            Log.i(TAG, "IOException:" + e.toString());
            return false;
        }
        Log.i(TAG,"file created successfully: "+file.getAbsolutePath());
        return true;
    }
}
