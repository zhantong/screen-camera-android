package cn.edu.nju.cs.screencamera;

import android.os.Environment;
import android.util.Log;
import android.util.Pair;

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
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import cn.edu.nju.cs.screencamera.Logback.CustomMarker;
import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

/**
 * Created by zhantong on 2017/5/24.
 */

public class BlackWhiteCodeStream extends StreamDecode{
    private static final String TAG="ShiftCodeMLStream";
    private static final boolean DUMP=false;
    private Map<DecodeHintType,?> hints;
    private ArrayDataDecoder dataDecoder=null;
    private int raptorQSymbolSize =-1;
    private ShiftCodeMLStream.Callback callback;

    static Logger LOG= LoggerFactory.getLogger(MainActivity.class);

    private List<int[]> randomIntArrayList=ShiftCodeML.randomBarcodeValue(new ShiftCodeMLConfig());
    private List<BitSet> truthBitSetList;

    public interface Callback{
        void onBarcodeNotFound();
        void onCRCCheckFailed();
        void onBeforeDataDecoded();
    }
    public BlackWhiteCodeStream(Map<DecodeHintType,?> hints){
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
                mediateBarcode = new MediateBarcode(frame,new BlackWhiteCodeConfig(),null,RawImage.CHANNLE_Y);
            } catch (NotFoundException e) {
                Log.i(TAG,"barcode not found");
                if(callback!=null){
                    callback.onBarcodeNotFound();
                }
                continue;
            }
            BlackWhiteCode blackWhiteCode=new BlackWhiteCode(mediateBarcode,hints);
            blackWhiteCode.mediateBarcode.getContent(blackWhiteCode.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN),RawImage.CHANNLE_Y);
            int overlapSituation=blackWhiteCode.getOverlapSituation();
            if(DUMP) {
                JsonObject mainJson=blackWhiteCode.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN).toJson();
                barcodeJson.add("barcode",mainJson);
                barcodeJson.addProperty("index",blackWhiteCode.mediateBarcode.rawImage.getIndex());
                //JsonObject varyBarJson=shiftCodeML.getVaryBarToJson();
                //barcodeJson.add("varyBar",varyBarJson);
                barcodeJson.addProperty("overlapSituation",overlapSituation);
                //barcodeJson.addProperty("isRandom",shiftCodeML.getIsRandom());
                //String jsonString=new Gson().toJson(root);
                //LOG.info(CustomMarker.source,jsonString);
            }

            if(raptorQSymbolSize ==-1){
                raptorQSymbolSize =blackWhiteCode.calcRaptorQSymbolSize(blackWhiteCode.calcRaptorQPacketSize());
            }
            if(dataDecoder==null){
                try {
                    int head = blackWhiteCode.getTransmitFileLengthInBytes();
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
            int[][] rawContents=blackWhiteCode.getMixedRawContent();
            for(int[] rawContent:rawContents){
                int[] rSDecodedData;
                try {
                    rSDecodedData = blackWhiteCode.rSDecode(rawContent,blackWhiteCode.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN));
                } catch (ReedSolomonException e) {
                    Log.i(TAG,"RS decode failed");
                    continue;
                }
                Log.i(TAG,"RS decode success");
                byte[] raptorQEncodedData=Utils.intArrayToByteArray(rSDecodedData,rSDecodedData.length,rsEcSize, blackWhiteCode.calcRaptorQPacketSize());
                Log.i(TAG,"raptorq encoded data length: "+raptorQEncodedData.length);
                Log.i(TAG,"raptorq encoded data: "+ Arrays.toString(raptorQEncodedData));
                try {
                    EncodingPacket encodingPacket = dataDecoder.parsePacket(raptorQEncodedData, true).value();
//                if(DUMP){
//                    JsonObject barcodeJson=new JsonObject();
//                    barcodeJson.addProperty("index",blackWhiteCode.mediateBarcode.rawImage.getIndex());
//                    barcodeJson.addProperty("esi",encodingPacket.encodingSymbolID());
//                    barcodeJson.addProperty("type",encodingPacket.symbolType().name());
//                    LOG.info(CustomMarker.processed,new Gson().toJson(barcodeJson));
//                }
                    Log.i(TAG, "encoding packet: source block number: " + encodingPacket.sourceBlockNumber() + " " + encodingPacket.encodingSymbolID() + " " + encodingPacket.symbolType() + " " + encodingPacket.numberOfSymbols());
                    if (isLastEncodingPacket(encodingPacket)) {
                        Log.i(TAG, "last encoding packet: " + encodingPacket.encodingSymbolID());
                        //setStopQueue();
                        stopVideoDecoding();
                        frames.clear();
                        RawImage rawImage=new RawImage();
                        frames.add(rawImage);
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
