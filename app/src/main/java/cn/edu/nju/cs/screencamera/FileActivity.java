package cn.edu.nju.cs.screencamera;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;

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
        EditText editTextFileName = findViewById(R.id.file_name_output);
        final String outputFilePath = Utils.combinePaths(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), editTextFileName.getText().toString());

        Thread worker = new Thread() {
            @Override
            public void run() {
                StreamDecode streamDecode = MultiFormatStream.getInstance(barcodeSettingsFragment.getBarcodeFormat(), inputFilePath, outputFilePath);
                streamDecode.start();
            }
        };
        worker.start();
    }

    /**
     * 在APP内打开文件
     *
     * @param view 默认参数
     */
    public void openFile(View view) {
        EditText editTextFileName = findViewById(R.id.file_name_output);
        String originFileName = editTextFileName.getText().toString();
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), originFileName);
        file = correctFileExtension(file);
        String correctedFileName = file.getName();
        if (!correctedFileName.equals(originFileName)) {
            editTextFileName.setText(correctedFileName);
        }

        String mimeType = URLConnection.guessContentTypeFromName(correctedFileName);
        if (mimeType != null) {
            Intent newIntent = new Intent(Intent.ACTION_VIEW);
            newIntent.setDataAndType(Uri.fromFile(file), mimeType);
            newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(newIntent);
        } else {
            new AlertDialog.Builder(this).setTitle("未识别的文件类型")
                    .setMessage("未识别的文件后缀名或文件内容")
                    .setPositiveButton("确定", null)
                    .show();
        }
    }

    private File correctFileExtension(File file) {
        String originFileName = file.getName();
        int lastSeparatorIndex = originFileName.lastIndexOf('.');
        String fileNameWithoutExtension = originFileName;
        String originExtension = "";
        if (lastSeparatorIndex != -1) {
            fileNameWithoutExtension = originFileName.substring(0, lastSeparatorIndex);
            originExtension = originFileName.substring(lastSeparatorIndex + 1);
        }
        String correctedExtension = getFileExtension(file);
        if (!correctedExtension.equals(originExtension)) {
            String correctedFileName = fileNameWithoutExtension + "." + correctedExtension;
            File newFile = new File(file.getParent(), correctedFileName);
            file.renameTo(newFile);
            file = newFile;
        }
        return file;
    }

    private String getFileExtension(File file) {
        if (!file.isFile()) {
            throw new RuntimeException("file not exists");
        }
        ContentInfoUtil util = new ContentInfoUtil();
        ContentInfo info;
        try {
            info = util.findMatch(file);
        } catch (IOException e) {
            throw new RuntimeException("file not exists");
        }
        if (info == null) {
            return "txt";
        }
        return info.getFileExtensions()[0];
    }
}
