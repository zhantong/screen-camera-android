package cn.edu.nju.cs.screencamera;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;

import java.io.File;

/**
 * Created by zhantong on 2017/12/10.
 */

public class ConfigEditActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_edit);

        String filePath = getIntent().getStringExtra("path");
        String configName = new File(filePath).getName();
        configName = configName.substring(0, configName.lastIndexOf("."));
        EditText textConfigName = findViewById(R.id.config_name);
        textConfigName.setText(configName);
        String content = "";
        try {
            content = Utils.getStringFromFile(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        EditText textContent = findViewById(R.id.content);
        textContent.setText(content);
    }
}
