package cn.edu.nju.cs.screencamera;

import android.os.Environment;
import android.util.Log;

import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.parameters.FECParameters;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

/**
 * Created by zhantong on 2016/11/27.
 */

public class ShiftCodeStream {
    private static final String TAG="ShiftCodeStream";
    private Map<DecodeHintType,?> hints;
    private ArrayDataDecoder dataDecoder=null;
    private int numDataBytesPerBarcode=-1;
    public ShiftCodeStream(Map<DecodeHintType,?> hints){
        this.hints=hints;
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
                continue;
            }
            ShiftCode shiftCode=new ShiftCode(mediateBarcode,hints);
            if(numDataBytesPerBarcode==-1){
                numDataBytesPerBarcode=shiftCode.calcNumRaptorQBytes();
            }
            if(dataDecoder==null){
                try {
                    int head = shiftCode.getTransmitFileLengthInBytes();
                    int numSourceBlock=0;
                    if(hints!=null){
                        numSourceBlock=Integer.parseInt(hints.get(DecodeHintType.RAPTORQ_NUMBER_OF_SOURCE_BLOCKS).toString());
                    }
                    FECParameters parameters=FECParameters.newParameters(head,numDataBytesPerBarcode,numSourceBlock);
                    dataDecoder= OpenRQ.newDecoder(parameters,0);
                } catch (CRCCheckException e) {
                    e.printStackTrace();
                    continue;
                }
            }
            int overlapSituation=shiftCode.getOverlapSituation();
            if(overlapSituation==ShiftCode.OVERLAP_CLEAR_WHITE||overlapSituation==ShiftCode.OVERLAP_CLEAR_BLACK){
                int[] rawContent=shiftCode.getClearRawContent();
                int[] rSDecodedData;
                try {
                    rSDecodedData = shiftCode.rSDecode(rawContent,shiftCode.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN));
                } catch (ReedSolomonException e) {
                    Log.i(TAG,"RS decode failed");
                    continue;
                }
                Log.i(TAG,"RS decode success");
                byte[] RaptorQEncodedData=Utils.intArrayToByteArray(rSDecodedData,rSDecodedData.length,rsEcSize,numDataBytesPerBarcode);
                EncodingPacket encodingPacket=dataDecoder.parsePacket(RaptorQEncodedData,true).value();
                Log.i(TAG, "encoding symbol ID:" + encodingPacket.encodingSymbolID() + "\t" + encodingPacket.symbolType());
            }else{
                int[][] mixedContent=shiftCode.getMixedRawContent();
                for(int i=0;i<2;i++){
                    int[] rawContent=mixedContent[i];
                    int[] rSDecodedData;
                    try {
                        rSDecodedData = shiftCode.rSDecode(rawContent,shiftCode.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN));
                    } catch (ReedSolomonException e) {
                        Log.i(TAG,"RS decode failed");
                        continue;
                    }
                    Log.i(TAG,"RS decode success");
                    byte[] RaptorQEncodedData=Utils.intArrayToByteArray(rSDecodedData,rSDecodedData.length,rsEcSize,numDataBytesPerBarcode);
                    EncodingPacket encodingPacket=dataDecoder.parsePacket(RaptorQEncodedData,true).value();
                    Log.i(TAG, "encoding symbol ID:" + encodingPacket.encodingSymbolID() + "\t" + encodingPacket.symbolType());
                }
            }
        }
    }
}
