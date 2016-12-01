package cn.edu.nju.cs.screencamera;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.bind.JsonTreeReader;
import com.google.gson.stream.JsonReader;

import net.fec.openrq.ArrayDataDecoder;
import net.fec.openrq.OpenRQ;
import net.fec.openrq.parameters.FECParameters;
import net.fec.openrq.parameters.SerializableParameters;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import cn.edu.nju.cs.screencamera.ReedSolomon.ReedSolomonException;

/**
 * Created by zhantong on 2016/11/29.
 */

public class ShiftCodeMLFile {
    private static final String TAG="ShiftCodeMLFile";
    Map<DecodeHintType,?> hints;
    public ShiftCodeMLFile(String filePath,Map<DecodeHintType,?> hints){
        this.hints=hints;
        Gson gson=new Gson();
        JsonObject root=null;
        try {
            JsonParser parser=new JsonParser();
            root=(JsonObject) parser.parse(new FileReader(filePath));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        JsonObject raptorQMeta=(JsonObject)root.get("raptorQMeta");
        long commonOTI=raptorQMeta.get("commonOTI").getAsLong();
        int schemeSpecificOTI=raptorQMeta.get("schemeSpecificOTI").getAsInt();
        int[][] data=gson.fromJson(root.get("values"),int[][].class);
        SerializableParameters serializableParameters=new SerializableParameters(commonOTI,schemeSpecificOTI);
        FECParameters parameters=FECParameters.parse(serializableParameters).value();
        stream(data,parameters);
    }
    public void stream(int[][] data, FECParameters parameters){
        ArrayDataDecoder dataDecoder= OpenRQ.newDecoder(parameters,0);
        ShiftCodeML shiftCodeML=new ShiftCodeML(new MediateBarcode(new ShiftCodeMLConfig()),hints);
        for(int[] rawContent:data){
            try {
                int[] rSDecodedData = shiftCodeML.rSDecode(rawContent,shiftCodeML.mediateBarcode.districts.get(Districts.MAIN).get(District.MAIN));
            } catch (ReedSolomonException e) {
                Log.i(TAG,"RS decode failed");
                continue;
            }
            Log.i(TAG,"RS decode success");
        }
    }
}
