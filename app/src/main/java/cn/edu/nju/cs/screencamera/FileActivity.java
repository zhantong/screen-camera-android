package cn.edu.nju.cs.screencamera;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * UI主要操作
 * 也是控制二维码识别的主要入口
 */
public class FileActivity extends Activity {
    BarcodeSettingsFragment barcodeSettingsFragment;

    /**
     * 界面初始化,设置界面,调用CameraSettings()设置相机参数
     *
     * @param savedInstanceState 默认参数
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);

        barcodeSettingsFragment = (BarcodeSettingsFragment) getFragmentManager().findFragmentById(R.id.fragment_barcode_settings);

        Button btnStart = findViewById(R.id.btn_start);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processFile();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.file, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_video_to_frames:
                videoToFrames();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    void videoToFrames() {
        Intent intent = new Intent(this, VideoToFramesActivity.class);
        startActivity(intent);
    }

    public void processFile() {
        EditText editTextInputFilePath = findViewById(R.id.file_path_input);
        final String inputFilePath = editTextInputFilePath.getText().toString();

        Thread worker = new Thread() {
            @Override
            public void run() {
                StreamDecode streamDecode = MultiFormatStream.getInstance(barcodeSettingsFragment.getConfig(), inputFilePath);
                streamDecode.setActivity(FileActivity.this);
                streamDecode.start();
            }
        };
        worker.start();
    }
}
