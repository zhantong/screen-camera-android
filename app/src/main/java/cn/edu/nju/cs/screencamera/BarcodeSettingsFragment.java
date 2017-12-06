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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import static android.app.Activity.RESULT_OK;

/**
 * Created by zhantong on 2017/11/16.
 */

public class BarcodeSettingsFragment extends Fragment {
    View rootView;
    private BarcodeFormat barcodeFormat;

    public static final int REQUEST_CODE_FILE_PATH_INPUT = 1;

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

        sharedPref = getActivity().getSharedPreferences("main", Context.MODE_PRIVATE);
        editor = sharedPref.edit();


        initBarcodeFormatSpinner();

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

    BarcodeFormat getBarcodeFormat() {
        return barcodeFormat;
    }

    private void initBarcodeFormatSpinner() {
        Spinner barcodeFormatSpinner = rootView.findViewById(R.id.barcode_format);
        barcodeFormatSpinner.setTag("BARCODE_FORMAT");
        ArrayAdapter<BarcodeFormat> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, BarcodeFormat.values());
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
        int id = 0;
        switch (requestCode) {
            case REQUEST_CODE_FILE_PATH_INPUT:
                id = R.id.file_path_input;
                break;
        }
        if (resultCode == RESULT_OK) {
            EditText editText = rootView.findViewById(id);
            String curFileName = data.getData().getPath();
            editText.setText(curFileName);
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
