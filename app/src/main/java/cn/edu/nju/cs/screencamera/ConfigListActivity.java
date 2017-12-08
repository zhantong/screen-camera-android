package cn.edu.nju.cs.screencamera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;

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
            TextView textFileName = view.findViewById(R.id.text_file_name);
            textFileName.setText(files[position].getName());
            return view;
        }
    }
}
