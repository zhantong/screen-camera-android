package cn.edu.nju.cs.screencamera;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by zhantong on 2016/9/26.
 */

public class PropertiesReader {
    private Context context;
    private Properties properties;

    String propertiesFileName="app.properties";

    public PropertiesReader(){
        context=MainActivity.getContext();
        properties=new Properties();
        try {
            InputStream inputStream = context.getAssets().open(propertiesFileName);
            properties.load(inputStream);
        }catch (IOException e){
            throw new RuntimeException();
        }
    }
    public String getProperty(String key){
        return properties.getProperty(key);
    }
}
