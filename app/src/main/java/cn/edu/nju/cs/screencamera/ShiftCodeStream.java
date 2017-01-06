package cn.edu.nju.cs.screencamera;

import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.SymbolType;
import net.fec.openrq.decoder.SourceBlockDecoder;
import net.fec.openrq.parameters.FECParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import cn.edu.nju.cs.screencamera.Logback.CustomMarker;
import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

/**
 * Created by zhantong on 2016/11/27.
 */

public class ShiftCodeStream extends StreamDecode{
    private static final String TAG="ShiftCodeStream";
    protected Map<DecodeHintType,?> hints;
    private ArrayDataDecoder dataDecoder=null;
    private int raptorQSymbolSize =-1;
    private int rsEcSize=-1;
    static Logger LOG= LoggerFactory.getLogger(MainActivity.class);
    private EncodingPacket lastEncodingPacket;

    private boolean DUMP=false;

    protected int[] rectangle=null;
    protected void beforeStream() {
        if(hints!=null){
            rsEcSize=Integer.parseInt(hints.get(DecodeHintType.RS_ERROR_CORRECTION_SIZE).toString());
        }
    }

    public void afterStream() {
        if(dataDecoder!=null){
            dataDecoder.sourceBlock(lastEncodingPacket.sourceBlockNumber()).putEncodingPacket(lastEncodingPacket);
            if(dataDecoder.isDataDecoded()){
                Log.i(TAG,"RaptorQ decode success");
                if(outputFilePath==null){
                    Log.i(TAG,"output file path not set.");
                    return;
                }
                writeRaptorQDataFile(dataDecoder,outputFilePath);
            }
        }
    }

    public void processFrame(RawImage frame) {
        //Utils.dumpFile(Environment.getExternalStorageDirectory().toString()+"/"+frame.getIndex()+".yuv",frame.getPixels());
        Log.i(TAG,frame.toString());

        MediateBarcode mediateBarcode;
        try {
            mediateBarcode = getMediateBarcode(frame);
        } catch (NotFoundException e) {
            Log.i(TAG,"barcode not found: "+e.toString());
            if(getIsCamera()){
                focusCamera();
            }
            return;
        }
        ShiftCode shiftCode=getShiftCode(mediateBarcode);
        if(raptorQSymbolSize ==-1){
            raptorQSymbolSize =shiftCode.calcRaptorQSymbolSize(shiftCode.calcRaptorQPacketSize());
        }
        if(dataDecoder==null){
            try {
                int head = shiftCode.getTransmitFileLengthInBytes();
                int numSourceBlock=0;
                if(hints!=null){
                    numSourceBlock=Integer.parseInt(hints.get(DecodeHintType.RAPTORQ_NUMBER_OF_SOURCE_BLOCKS).toString());
                }
                FECParameters parameters=FECParameters.newParameters(head, raptorQSymbolSize,numSourceBlock);
                System.out.println("FECParameters: "+parameters.toString());
                Log.i(TAG,"data length: "+parameters.dataLengthAsInt()+" symbol length: "+parameters.symbolSize());
                dataDecoder= OpenRQ.newDecoder(parameters,0);
            } catch (CRCCheckException e) {
                e.printStackTrace();
                if(getIsCamera()){
                    focusCamera();
                }
                return;
            }
        }
        int overlapSituation=shiftCode.getOverlapSituation();
        int[][] rawContents=shiftCode.getRawContents();
        if(DUMP) {
            JsonObject barcodeJson=new JsonObject();
            barcodeJson.addProperty("index",shiftCode.mediateBarcode.rawImage.getIndex());
            barcodeJson.addProperty("timestamp",frame.getTimestamp());
            int[][] temp=new int[][]{Utils.changeNumBitsPerInt(rawContents[0],2,12),Utils.changeNumBitsPerInt(rawContents[1],4,12)};

            barcodeJson.add("results", new Gson().toJsonTree(temp));
            LOG.info(CustomMarker.source,new Gson().toJson(barcodeJson));
        }
        for(int[] rawContent:rawContents){
            int[] rSDecodedData;
            try {
                rSDecodedData = shiftCode.rSDecode(rawContent,shiftCode.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN));
            } catch (ReedSolomonException e) {
                Log.i(TAG,"RS decode failed");
                continue;
            }
            Log.i(TAG,"RS decode success");
            byte[] raptorQEncodedData=Utils.intArrayToByteArray(rSDecodedData,rSDecodedData.length,rsEcSize, shiftCode.calcRaptorQPacketSize());
            //Log.i(TAG,"raptorq encoded data length: "+raptorQEncodedData.length);
            //Log.i(TAG,"raptorq encoded data: "+Arrays.toString(raptorQEncodedData));
            EncodingPacket encodingPacket=dataDecoder.parsePacket(raptorQEncodedData,true).value();
            if(DUMP){
                JsonObject barcodeJson=new JsonObject();
                barcodeJson.addProperty("index",shiftCode.mediateBarcode.rawImage.getIndex());
                barcodeJson.addProperty("esi",encodingPacket.encodingSymbolID());
                barcodeJson.addProperty("type",encodingPacket.symbolType().name());
                LOG.info(CustomMarker.processed,new Gson().toJson(barcodeJson));
            }
            Log.i(TAG,"encoding packet: source block number: "+encodingPacket.sourceBlockNumber()+" "+encodingPacket.encodingSymbolID()+" "+encodingPacket.symbolType()+" "+encodingPacket.numberOfSymbols());
            if(isLastEncodingPacket(encodingPacket)){
                Log.i(TAG,"last encoding packet: "+encodingPacket.encodingSymbolID());
                lastEncodingPacket=encodingPacket;
                setStopQueue();
                return;
            }
            dataDecoder.sourceBlock(encodingPacket.sourceBlockNumber()).putEncodingPacket(encodingPacket);
        }
        rectangle=zoomRectangle(mediateBarcode.getRectangle());
    }
    protected MediateBarcode getMediateBarcode(RawImage frame) throws NotFoundException {
        return new MediateBarcode(frame,new ShiftCodeConfig(),rectangle,RawImage.CHANNLE_Y);
    }
    protected ShiftCode getShiftCode(MediateBarcode mediateBarcode){
        return new ShiftCode(mediateBarcode,hints);
    }

    public ShiftCodeStream(Map<DecodeHintType,?> hints){
        this.hints=hints;
    }
    private boolean isLastEncodingPacket(EncodingPacket encodingPacket){
        SourceBlockDecoder sourceBlock=dataDecoder.sourceBlock(encodingPacket.sourceBlockNumber());
        return (sourceBlock.missingSourceSymbols().size()-sourceBlock.availableRepairSymbols().size()==1)
                &&((encodingPacket.symbolType()== SymbolType.SOURCE&&!sourceBlock.containsSourceSymbol(encodingPacket.encodingSymbolID()))
                ||(encodingPacket.symbolType()== SymbolType.REPAIR&&!sourceBlock.containsRepairSymbol(encodingPacket.encodingSymbolID())));
    }
    private void writeRaptorQDataFile(ArrayDataDecoder decoder,String filePath){
        byte[] out = decoder.dataArray();
        String sha1 = FileVerification.bytesToSHA1(out);
        Log.d(TAG, "file SHA-1 verification: " + sha1);
        bytesToFile(out, filePath);
    }
    private boolean bytesToFile(byte[] bytes,String filePath){
        if(filePath.isEmpty()){
            Log.i(TAG, "file path is empty");
            return false;
        }
        File file = new File(filePath);
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
