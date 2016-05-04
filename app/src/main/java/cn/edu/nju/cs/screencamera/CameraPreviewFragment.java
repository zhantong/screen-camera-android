package cn.edu.nju.cs.screencamera;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by zhantong on 16/5/2.
 */
public class CameraPreviewFragment extends Fragment {
    public CameraPreview mPreview;
    public LinkedBlockingQueue<byte[]> queue;
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_camera_preview, container, false);
        mPreview = new CameraPreview(this.getActivity());
        FrameLayout preview = (FrameLayout) rootView.findViewById(R.id.cameraPreview);
        preview.addView(mPreview);
        SettingsFragment.passCamera(mPreview.getCameraInstance());
        SettingsFragment settingsFragment=new SettingsFragment();
        SharedPreferences sharedPreferences= PreferenceManager.getDefaultSharedPreferences(this.getActivity());
        settingsFragment.setDefault(sharedPreferences);
        PreferenceManager.setDefaultValues(this.getActivity(), R.xml.preferences, false);
        SettingsFragment.init(PreferenceManager.getDefaultSharedPreferences(this.getActivity()));
        mPreview.start(queue);
        return rootView;
    }
}
