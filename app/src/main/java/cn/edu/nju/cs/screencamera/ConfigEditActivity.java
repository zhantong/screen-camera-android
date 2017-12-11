package cn.edu.nju.cs.screencamera;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.config_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                saveConfig();
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void saveConfig() {
        EditText textContent = findViewById(R.id.content);
        EditText textConfigName = findViewById(R.id.config_name);
        String configName = textConfigName.getText().toString();
        String filePath = Utils.combinePaths(getFilesDir().getAbsolutePath(), "configs", configName + ".json");
        try {
            Utils.saveStringToFile(textContent.getText().toString(), filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
