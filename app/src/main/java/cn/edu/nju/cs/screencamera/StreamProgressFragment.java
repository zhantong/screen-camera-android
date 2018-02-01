package cn.edu.nju.cs.screencamera;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by zhantong on 2017/12/12.
 */

public class StreamProgressFragment extends Fragment {
    View rootView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_stream_progress, container, false);
        return rootView;
    }

    void updateProgress(final int progress, final int max) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ProgressBar progressBar = rootView.findViewById(R.id.progress_bar_progress);
                progressBar.setMax(max);
                progressBar.setProgress(progress);
            }
        });
    }

    void update(final String string) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textView = rootView.findViewById(R.id.text_view_test);
                textView.setText(string);
            }
        });
    }
}
