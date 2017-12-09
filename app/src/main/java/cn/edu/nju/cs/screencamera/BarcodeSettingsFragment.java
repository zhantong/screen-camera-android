package cn.edu.nju.cs.screencamera;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static android.app.Activity.RESULT_OK;

/**
 * Created by zhantong on 2017/11/16.
 */

public class BarcodeSettingsFragment extends Fragment {
    View rootView;
    String barcodeConfigFileName;

    public static final int REQUEST_CODE_FILE_PATH_INPUT = 1;
    public static final int REQUEST_CODE_BARCODE_CONFIG = 2;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_barcode_settings, container, false);

        Button buttonFilePathInput = rootView.findViewById(R.id.button_file_path_input);
        buttonFilePathInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFilePath(REQUEST_CODE_FILE_PATH_INPUT);
            }
        });

        Button buttonBarcodeConfig = rootView.findViewById(R.id.button_barcode_config);
        buttonBarcodeConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ConfigListActivity.class);
                startActivityForResult(intent, REQUEST_CODE_BARCODE_CONFIG);
            }
        });

        sharedPref = getActivity().getSharedPreferences("main", Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        final EditText editTextFilePathInput = rootView.findViewById(R.id.file_path_input);
        editTextFilePathInput.setTag("FILE_PATH_INPUT");
        editTextFilePathInput.setText(sharedPref.getString((String) editTextFilePathInput.getTag(), ""));
        editTextFilePathInput.addTextChangedListener(new EditTextTextWatcher(editTextFilePathInput));

        return rootView;
    }

    void hideLayoutFilePathInput() {
        View view = rootView.findViewById(R.id.layout_file_path_input);
        view.setVisibility(View.GONE);
    }

    JsonObject getConfig() {
        JsonParser parser = new JsonParser();
        JsonObject root = null;
        try {
            root = parser.parse(new FileReader(new File(Utils.combinePaths(App.getContext().getFilesDir().getAbsolutePath(), "configs", barcodeConfigFileName + ".json")))).getAsJsonObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return root;
    }

    private void getFilePath(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(Intent.createChooser(intent, "Select a File"), requestCode);
        } else {
            new AlertDialog.Builder(getActivity()).setTitle("未找到文件管理器")
                    .setMessage("请安装文件管理器以选择文件")
                    .setPositiveButton("确定", null)
                    .show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_FILE_PATH_INPUT:
                if (resultCode == RESULT_OK) {
                    EditText editText = rootView.findViewById(R.id.file_path_input);
                    String curFileName = data.getData().getPath();
                    editText.setText(curFileName);
                }
                break;
            case REQUEST_CODE_BARCODE_CONFIG:
                if (resultCode == RESULT_OK) {
                    barcodeConfigFileName = data.getStringExtra("result");
                    EditText editText = rootView.findViewById(R.id.barcode_config);
                    editText.setText(barcodeConfigFileName);
                }
                break;
        }
    }

    private class EditTextTextWatcher implements TextWatcher {
        private EditText mEditText;

        public EditTextTextWatcher(EditText editText) {
            mEditText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            editor.putString((String) mEditText.getTag(), mEditText.getText().toString());
            editor.apply();
        }
    }
}
