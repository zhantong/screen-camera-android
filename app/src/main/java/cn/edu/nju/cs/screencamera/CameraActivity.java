package cn.edu.nju.cs.screencamera;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;


/**
 * Created by zhantong on 16/5/2.
 */
public class CameraActivity extends Activity {
    private boolean onWorking = false;

    public CameraPreview mPreview;

    BarcodeSettingsFragment barcodeSettingsFragment;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        barcodeSettingsFragment = (BarcodeSettingsFragment) getFragmentManager().findFragmentById(R.id.fragment_barcode_settings);
        barcodeSettingsFragment.hideLayoutFilePathInput();

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

    void start() {
        Thread worker = new Thread() {
            @Override
            public void run() {
                StreamDecode streamDecode = MultiFormatStream.getInstance(barcodeSettingsFragment.getBarcodeConfig(), mPreview);
                streamDecode.start();
                getFragmentManager().popBackStack();
            }
        };
        worker.start();
    }
}
