package cn.edu.nju.cs.screencamera;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;


/**
 * Created by zhantong on 16/5/2.
 */
public class CameraPreviewFragment extends Fragment {
    private OnStartListener mCallback;
    private boolean onWorking = false;

    public interface OnStartListener {
        void onStartRecognize();
    }

    public CameraPreview mPreview;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_camera_preview, container, false);
        final Button buttonSettings = (Button) rootView.findViewById(R.id.button_settings);
        buttonSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getChildFragmentManager().getBackStackEntryCount() == 0) {
                    getChildFragmentManager().beginTransaction().replace(R.id.cameraPreview, new CameraSettingsFragment()).addToBackStack(null).commit();
                    buttonSettings.setText("取消");
                } else {
                    getChildFragmentManager().popBackStack();
                    buttonSettings.setText("设置");
                }
            }
        });
        final Button buttonStart = (Button) rootView.findViewById(R.id.button_start);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onWorking) {
                    getActivity().getFragmentManager().popBackStack();
                } else {
                    buttonStart.setText("停止");
                    mCallback.onStartRecognize();
                    onWorking = true;
                }
            }
        });
        mPreview = new CameraPreview(this.getActivity());
        FrameLayout preview = (FrameLayout) rootView.findViewById(R.id.cameraPreview);
        preview.addView(mPreview);
        CameraSettingsFragment.passCamera(mPreview.getCameraInstance());
        CameraSettingsFragment cameraSettingsFragment = new CameraSettingsFragment();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
        cameraSettingsFragment.setDefault(sharedPreferences);
        PreferenceManager.setDefaultValues(this.getActivity(), R.xml.camera_preferences, false);
        CameraSettingsFragment.init(PreferenceManager.getDefaultSharedPreferences(this.getActivity()));
        return rootView;
    }

    public void addCallback(OnStartListener listener) {
        mCallback = listener;
    }
}
