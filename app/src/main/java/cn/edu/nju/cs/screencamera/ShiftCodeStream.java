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

import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

/**
 * Created by zhantong on 2016/11/27.
 */

public class ShiftCodeStream extends StreamDecode{
    private static final String TAG="ShiftCodeStream";
    private Map<DecodeHintType,?> hints;
    private ArrayDataDecoder dataDecoder=null;
    private int raptorQSymbolSize =-1;
    private int rsEcSize=-1;
    static Logger LOG= LoggerFactory.getLogger(MainActivity.class);
    private EncodingPacket lastEncodingPacket;
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
                writeRaptorQDataFile(dataDecoder,"out.txt");
            }
        }
    }

    public void processFrame(RawImage frame) {
        //Utils.dumpFile(Environment.getExternalStorageDirectory().toString()+"/"+frame.getIndex()+".yuv",frame.getPixels());
        Log.i(TAG,frame.toString());

        MediateBarcode mediateBarcode;
        try {
            //mediateBarcode = new MediateBarcode(frame,new ShiftCodeConfig());
            mediateBarcode = new MediateBarcode(frame,new ShiftCodeColorConfig());
        } catch (NotFoundException e) {
            Log.i(TAG,"barcode not found");
            if(getIsCamera()){
                focusCamera();
            }
            return;
        }
        //ShiftCode shiftCode=new ShiftCode(mediateBarcode,hints);
        ShiftCodeColor shiftCode=new ShiftCodeColor(mediateBarcode,hints);
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
        if(false) {
            JsonObject root = shiftCode.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN).toJson();
            root.addProperty("overlapSituation",shiftCode.getOverlapSituation());
            root.add("result", new Gson().toJsonTree(rawContents));
            LOG.info(new Gson().toJson(root));
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
            Log.i(TAG,"raptorq encoded data length: "+raptorQEncodedData.length);
            Log.i(TAG,"raptorq encoded data: "+Arrays.toString(raptorQEncodedData));
            EncodingPacket encodingPacket=dataDecoder.parsePacket(raptorQEncodedData,true).value();
            Log.i(TAG,"encoding packet: source block number: "+encodingPacket.sourceBlockNumber()+" "+encodingPacket.encodingSymbolID()+" "+encodingPacket.symbolType()+" "+encodingPacket.numberOfSymbols());
            if(isLastEncodingPacket(encodingPacket)){
                Log.i(TAG,"last encoding packet: "+encodingPacket.encodingSymbolID());
                lastEncodingPacket=encodingPacket;
                setStopQueue();
                return;
            }
            dataDecoder.sourceBlock(encodingPacket.sourceBlockNumber()).putEncodingPacket(encodingPacket);
        }
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
