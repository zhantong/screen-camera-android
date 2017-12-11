package cn.edu.nju.cs.screencamera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
    File[] configFiles;
    Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_list);

        ListView listView = findViewById(R.id.list_main);

        configFiles = new File(getFilesDir(), "configs").listFiles();
        adapter = new Adapter();
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

    @Override
    protected void onResume() {
        super.onResume();

        configFiles = new File(getFilesDir(), "configs").listFiles();
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.config_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_new_config:
                startActivity(new Intent(ConfigListActivity.this, ConfigEditActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    class Adapter extends BaseAdapter {

        @Override
        public int getCount() {
            return configFiles.length;
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
            final File file = configFiles[position];
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
