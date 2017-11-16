package cn.edu.nju.cs.screencamera;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;


/**
 * Created by zhantong on 16/5/2.
 */
public class CameraActivity extends Activity {
    private boolean onWorking = false;

    public CameraPreview mPreview;

    private BarcodeFormat barcodeFormat;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        sharedPref = getSharedPreferences("main", Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        initBarcodeFormatSpinner();

        final Button buttonSettings = findViewById(R.id.btn_camera_settings);
        buttonSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getFragmentManager().getBackStackEntryCount() == 0) {
                    getFragmentManager().beginTransaction().replace(R.id.camera_preview, new CameraSettingsFragment()).addToBackStack(null).commit();
                    buttonSettings.setText("取消");
                } else {
                    getFragmentManager().popBackStack();
                    buttonSettings.setText("设置");
                }
            }
        });

        mPreview = new CameraPreview(this);
        FrameLayout preview = findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        CameraSettingsFragment.passCamera(mPreview.getCameraInstance());
        CameraSettingsFragment cameraSettingsFragment = new CameraSettingsFragment();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        cameraSettingsFragment.setDefault(sharedPreferences);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_camera, false);
        CameraSettingsFragment.init(PreferenceManager.getDefaultSharedPreferences(this));

        final Button buttonStart = findViewById(R.id.btn_start);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onWorking) {
                    getFragmentManager().popBackStack();
                } else {
                    start();
                    buttonStart.setText("停止");
                    onWorking = true;
                }
            }
        });
    }

    private void initBarcodeFormatSpinner() {
        Spinner barcodeFormatSpinner = findViewById(R.id.barcode_format);
        barcodeFormatSpinner.setTag("BARCODE_FORMAT");
        ArrayAdapter<BarcodeFormat> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, BarcodeFormat.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        barcodeFormatSpinner.setAdapter(adapter);
        barcodeFormatSpinner.setSelection(sharedPref.getInt((String) barcodeFormatSpinner.getTag(), 0));

        barcodeFormatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                barcodeFormat = BarcodeFormat.values()[position];

                editor.putInt((String) parent.getTag(), position);
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    void start() {
        EditText editTextFileName = findViewById(R.id.file_name_output);
        final String outputFilePath = Utils.combinePaths(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), editTextFileName.getText().toString());
        Thread worker = new Thread() {
            @Override
            public void run() {
                StreamDecode streamDecode = MultiFormatStream.getInstance(barcodeFormat, mPreview, outputFilePath);
                streamDecode.start();
                getFragmentManager().popBackStack();
            }
        };
        worker.start();
    }
}
