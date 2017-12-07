package cn.edu.nju.cs.screencamera;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by zhantong on 2017/11/14.
 */

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        Button btnFromFile = findViewById(R.id.btn_from_file);
        btnFromFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FileActivity.class);
                startActivity(intent);
            }
        });
        Button btnFromCamera = findViewById(R.id.btn_from_camera);
        btnFromCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });

        copyConfigsFromAssetsToStorage();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                showSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void showSettings() {
        Intent intent = new Intent(this, MainSettingsActivity.class);
        startActivity(intent);
    }

    void copyConfigsFromAssetsToStorage() {
        String CONFIG_FLODER_NAME = "configs";
        AssetManager assetManager = getAssets();
        String[] configFileNames = new String[0];
        try {
            configFileNames = assetManager.list(CONFIG_FLODER_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }
        File parentPath = new File(getFilesDir(), CONFIG_FLODER_NAME);
        if (!parentPath.exists()) {
            parentPath.mkdir();
        }
        for (String configFileName : configFileNames) {
            try {
                InputStream in = assetManager.open(CONFIG_FLODER_NAME + "/" + configFileName);
                OutputStream out = new FileOutputStream(new File(parentPath, configFileName));
                byte[] buffer = new byte[1024];
                int read = in.read(buffer);
                while (read != -1) {
                    out.write(buffer, 0, read);
                    read = in.read(buffer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
