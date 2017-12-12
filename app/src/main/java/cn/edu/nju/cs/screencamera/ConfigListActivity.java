package cn.edu.nju.cs.screencamera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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

        configFiles = new File(getFilesDir(), "configs").listFiles();

        RecyclerView recyclerView = findViewById(R.id.recycler_main);

        recyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new Adapter();
        recyclerView.setAdapter(adapter);
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

    class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.config_list_cell, parent, false);
            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final File file = configFiles[position];
            String fileName = file.getName();
            holder.textBarcodeConfigName.setText(fileName.substring(0, fileName.lastIndexOf(".")));
            JsonParser parser = new JsonParser();
            JsonObject jsonRoot = null;
            try {
                jsonRoot = parser.parse(new FileReader(file)).getAsJsonObject();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            holder.textBarcodeType.setText(jsonRoot.get("barcodeFormat").getAsString());
            holder.textBarcodeSize.setText(jsonRoot.get("mainWidth").getAsInt() + "x" + jsonRoot.get("mainHeight").getAsInt());
            holder.btnEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(ConfigListActivity.this, ConfigEditActivity.class);
                    intent.putExtra("path", file.getAbsolutePath());
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return configFiles.length;
        }

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView textBarcodeConfigName;
            TextView textBarcodeType;
            TextView textBarcodeSize;
            ImageButton btnEdit;

            public ViewHolder(View itemView) {
                super(itemView);
                textBarcodeConfigName = itemView.findViewById(R.id.text_barcode_config_name);
                textBarcodeType = itemView.findViewById(R.id.text_barcode_type);
                textBarcodeSize = itemView.findViewById(R.id.text_barcode_size);
                btnEdit = itemView.findViewById(R.id.btn_edit);

                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                String fileName = configFiles[getPosition()].getName();
                fileName = fileName.substring(0, fileName.lastIndexOf("."));
                intent.putExtra("result", fileName);
                setResult(RESULT_OK, intent);
                finish();
            }
        }
    }
}
