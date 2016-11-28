package cn.edu.nju.cs.screencamera;

import android.os.Environment;
import android.util.Log;

import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.SymbolType;
import net.fec.openrq.decoder.SourceBlockDecoder;
import net.fec.openrq.parameters.FECParameters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

/**
 * Created by zhantong on 2016/11/27.
 */

public class ShiftCodeStream{
    private static final String TAG="ShiftCodeStream";
    private Map<DecodeHintType,?> hints;
    private ArrayDataDecoder dataDecoder=null;
    private int raptorQSymbolSize =-1;
    private Callback callback;

    public interface Callback{
        void onBarcodeNotFound();
        void onCRCCheckFailed();
        void onBeforeDataDecoded();
    }

    public ShiftCodeStream(Map<DecodeHintType,?> hints){
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
            //Utils.dumpFile(Environment.getExternalStorageDirectory().toString()+"/"+frame.getIndex()+".yuv",frame.getPixels());
            Log.i(TAG,frame.toString());

            MediateBarcode mediateBarcode;
            try {
                mediateBarcode = new MediateBarcode(frame,new ShiftCodeConfig());
            } catch (NotFoundException e) {
                Log.i(TAG,"barcode not found");
                if(callback!=null){
                    callback.onBarcodeNotFound();
                }
                continue;
            }
            ShiftCode shiftCode=new ShiftCode(mediateBarcode,hints);
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
                    if(callback!=null){
                        callback.onCRCCheckFailed();
                    }
                    continue;
                }
            }
            int overlapSituation=shiftCode.getOverlapSituation();
            int[][] rawContents=shiftCode.getRawContents();
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
                if(isLastEncodingPacket(encodingPacket)&&callback!=null){
                    Log.i(TAG,"last encoding packet: "+encodingPacket.encodingSymbolID());
                    callback.onBeforeDataDecoded();
                    frames.clear();
                    RawImage rawImage=new RawImage(null,0,0,0,0);
                    frames.add(rawImage);
                    rawImage=null;
                }
                dataDecoder.sourceBlock(encodingPacket.sourceBlockNumber()).putEncodingPacket(encodingPacket);
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
