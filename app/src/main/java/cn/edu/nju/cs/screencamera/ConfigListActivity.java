package cn.edu.nju.cs.screencamera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Created by zhantong on 2017/12/8.
 */

public class ConfigListActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_list);

        ListView listView = findViewById(R.id.list_main);

        final File[] configFiles = new File(getFilesDir(), "configs").listFiles();
        Adapter adapter = new Adapter(configFiles);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                String fileName = configFiles[position].getName();
                fileName = fileName.substring(0, fileName.lastIndexOf("."));
                intent.putExtra("result", fileName);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    class Adapter extends BaseAdapter {
        File[] files;

        public Adapter(File[] files) {
            this.files = files;
        }

        @Override
        public int getCount() {
            return files.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = View.inflate(ConfigListActivity.this, R.layout.config_list_cell, null);
            final File file = files[position];
            String fileName = file.getName();
            TextView textBarcodeConfigName = view.findViewById(R.id.text_barcode_config_name);
            textBarcodeConfigName.setText(fileName.substring(0, fileName.lastIndexOf(".")));
            JsonParser parser = new JsonParser();
            JsonObject jsonRoot = null;
            try {
                jsonRoot = parser.parse(new FileReader(file)).getAsJsonObject();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            TextView textBarcodeType = view.findViewById(R.id.text_barcode_type);
            textBarcodeType.setText(jsonRoot.get("barcodeFormat").getAsString());
            TextView textBarcodeSize = view.findViewById(R.id.text_barcode_size);
            textBarcodeSize.setText(jsonRoot.get("mainWidth").getAsInt() + "x" + jsonRoot.get("mainHeight").getAsInt());

            ImageButton btnEdit = view.findViewById(R.id.btn_edit);
            btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(ConfigListActivity.this, ConfigEditActivity.class);
                    intent.putExtra("path", file.getAbsolutePath());
                    startActivity(intent);
                }
            });
            return view;
        }
    }
}
