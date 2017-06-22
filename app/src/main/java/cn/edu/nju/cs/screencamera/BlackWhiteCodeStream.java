package cn.edu.nju.cs.screencamera;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.EncodingPacket;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.ParsingFailureException;
import net.fec.openrq.SymbolType;
import net.fec.openrq.decoder.SourceBlockDecoder;
import net.fec.openrq.parameters.FECParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import cn.edu.nju.cs.screencamera.Logback.CustomMarker;
import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

/**
 * Created by zhantong on 2017/6/18.
 */

public class BlackWhiteCodeStream extends StreamDecode {
    private static final String TAG="BlackWhiteCodeStream";
    private static final boolean DUMP=true;
    ArrayDataDecoder dataDecoder=null;
    int raptorQSymbolSize =-1;
    int rsEcSize=-1;
    BarcodeConfig barcodeConfig;

    static Logger LOG= LoggerFactory.getLogger(MainActivity.class);

    public BlackWhiteCodeStream(){
        barcodeConfig=getBarcodeConfigInstance();
    }
    BarcodeConfig getBarcodeConfigInstance(){
        return new BlackWhiteCodeConfig();
    }
    @Override
    protected void beforeStream() {
        rsEcSize=Integer.parseInt(barcodeConfig.hints.get(BlackWhiteCode.KEY_SIZE_RS_ERROR_CORRECTION).toString());
        LOG.info(CustomMarker.barcodeConfig,new Gson().toJson(getBarcodeConfigInstance().toJson()));
    }
    MediateBarcode getMediateBarcode(RawImage rawImage){
        try {
            return new MediateBarcode(rawImage,getBarcodeConfigInstance(),null,RawImage.CHANNLE_Y);
        } catch (NotFoundException e) {
            return null;
        }
    }
    BlackWhiteCode getBarcodeInstance(MediateBarcode mediateBarcode){
        return new BlackWhiteCode(mediateBarcode);
    }
    void sampleContent(BlackWhiteCode blackWhiteCode){
        blackWhiteCode.mediateBarcode.getContent(blackWhiteCode.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN),RawImage.CHANNLE_Y);
    }
    ArrayDataDecoder getDataDecoder(BlackWhiteCode blackWhiteCode){
        int head;
        try{
            head = blackWhiteCode.getTransmitFileLengthInBytes();
        }catch (CRCCheckException e){
            return null;
        }
        int numSourceBlock=Integer.parseInt(barcodeConfig.hints.get(BlackWhiteCode.KEY_NUMBER_RAPTORQ_SOURCE_BLOCKS).toString());
        FECParameters parameters=FECParameters.newParameters(head, raptorQSymbolSize,numSourceBlock);
        return OpenRQ.newDecoder(parameters,0);
    }
    int[][] getContents(BlackWhiteCode blackWhiteCode){
        return new int[][]{blackWhiteCode.getContent()};
    }
    int[] rsDecode(BlackWhiteCode blackWhiteCode,int[] content){
        try {
            return blackWhiteCode.rSDecode(content, blackWhiteCode.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN));
        } catch (ReedSolomonException e) {
            return null;
        }
    }
    byte[] getRaptorQEncodedData(BlackWhiteCode blackWhiteCode,int[] rSDecodedData){
        return Utils.intArrayToByteArray(rSDecodedData,rSDecodedData.length,rsEcSize, blackWhiteCode.calcRaptorQPacketSize());
    }
    @Override
    protected void processFrame(RawImage frame) {
        Gson gson=new Gson();
        JsonObject jsonRoot=new JsonObject();
        if(frame.getPixels()==null){
            return;
        }
        Log.i(TAG,frame.toString());
        MediateBarcode mediateBarcode=getMediateBarcode(frame);
        if(DUMP) {
            jsonRoot.add("image", frame.toJson());
        }
        if(mediateBarcode!=null) {
            BlackWhiteCode blackWhiteCode = getBarcodeInstance(mediateBarcode);
            sampleContent(blackWhiteCode);
            if(DUMP) {
                jsonRoot.add("barcode",blackWhiteCode.toJson());
            }
            if(raptorQSymbolSize ==-1){
                raptorQSymbolSize = blackWhiteCode.calcRaptorQSymbolSize(blackWhiteCode.calcRaptorQPacketSize());
            }
            if(dataDecoder==null){
                dataDecoder=getDataDecoder(blackWhiteCode);
                if(dataDecoder!=null){
                    FECParameters parameters=dataDecoder.fecParameters();
                    if(DUMP){
                        LOG.info(CustomMarker.fecParameters,new Gson().toJson(Utils.fecParametersToJson(parameters)));
                    }
                }
            }
            JsonArray rsJsonArray=new JsonArray();
            JsonArray encodingPacketJsonArray=new JsonArray();
            int[][] contents=getContents(blackWhiteCode);
            for(int[] content:contents){
                JsonObject rsJsonRoot=new JsonObject();
                if(DUMP){
                    rsJsonRoot.add("rsEncodedContent",gson.toJsonTree(content));
                }
                int[] rsDecodedContent=rsDecode(blackWhiteCode,content);
                if(rsDecodedContent!=null) {
                    if(DUMP){
                        rsJsonRoot.add("rsDecodedContent",gson.toJsonTree(rsDecodedContent));
                    }
                    byte[] raptorQEncodedData = getRaptorQEncodedData(blackWhiteCode, rsDecodedContent);
                    if (dataDecoder != null) {
                        EncodingPacket encodingPacket=null;
                        try {
                            encodingPacket = dataDecoder.parsePacket(raptorQEncodedData, true).value();
                        }catch (ParsingFailureException e){
                            e.printStackTrace();
                        }
                        if(encodingPacket!=null) {
                            encodingPacketJsonArray.add(Utils.encodingPacketToJson(encodingPacket));
                            System.out.println(Utils.encodingPacketToJson(encodingPacket));
                            if (isLastEncodingPacket(encodingPacket)) {
                                Log.i(TAG, "last encoding packet: " + encodingPacket.encodingSymbolID());
                                setStopQueue();
                            }
                            dataDecoder.sourceBlock(encodingPacket.sourceBlockNumber()).putEncodingPacket(encodingPacket);
                        }
                    }
                }
                rsJsonArray.add(rsJsonRoot);
            }
            jsonRoot.add("rs",rsJsonArray);
            JsonObject raptorQJsonRoot=new JsonObject();
            raptorQJsonRoot.add("encodingPacket",encodingPacketJsonArray);
            raptorQJsonRoot.add("decoder",Utils.sourceBlockDecoderToJson(dataDecoder.sourceBlock(0)));
            jsonRoot.add("raptorQ",raptorQJsonRoot);
        }
        if(DUMP){
            LOG.info(CustomMarker.processed,new Gson().toJson(jsonRoot));
        }
    }

    @Override
    protected void afterStream() {
        if(dataDecoder!=null&&dataDecoder.isDataDecoded()){
            Log.i(TAG,"RaptorQ decode success");
            writeRaptorQDataFile(dataDecoder,outputFilePath);
        }
    }
    boolean isLastEncodingPacket(EncodingPacket encodingPacket){
        SourceBlockDecoder sourceBlock=dataDecoder.sourceBlock(encodingPacket.sourceBlockNumber());
        return (sourceBlock.missingSourceSymbols().size()-sourceBlock.availableRepairSymbols().size()==1)
                &&((encodingPacket.symbolType()== SymbolType.SOURCE&&!sourceBlock.containsSourceSymbol(encodingPacket.encodingSymbolID()))
                ||(encodingPacket.symbolType()== SymbolType.REPAIR&&!sourceBlock.containsRepairSymbol(encodingPacket.encodingSymbolID())));
    }
    void writeRaptorQDataFile(ArrayDataDecoder decoder,String filePath){
        byte[] out = decoder.dataArray();
        String sha1 = FileVerification.bytesToSHA1(out);
        LOG.info(CustomMarker.sha1,sha1);
        if(Utils.bytesToFile(out, filePath)){
            Log.i(TAG,"successfully write to "+filePath);
        }else{
            Log.i(TAG,"failed to write to "+filePath);
        }
    }
}
