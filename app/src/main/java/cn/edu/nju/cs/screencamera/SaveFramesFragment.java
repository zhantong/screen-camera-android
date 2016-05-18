package cn.edu.nju.cs.screencamera;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import cn.edu.nju.cs.screencamera.FileExplorer.FileChooser;

/**
 * Created by zhantong on 16/5/18.
 */
public class SaveFramesFragment extends Fragment implements VideoToFrames.Callback{
    private static final int REQUEST_CODE_GET_FILE_PATH=1;
    private View thisView;
    private OutputImageFormat outputImageFormat;
    private SaveFramesFragment self=this;

    final Handler handler=new Handler(){
      public void handleMessage(Message msg){
          String str=(String)msg.obj;
          updateInfo(str);
      }
    };
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        thisView = inflater.inflate(R.layout.fragment_save_frames, container, false);
        final Button buttonFilePathInput = (Button) thisView.findViewById(R.id.button_file_path_input);
        buttonFilePathInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), FileChooser.class);
                startActivityForResult(intent,REQUEST_CODE_GET_FILE_PATH);
            }
        });

        final Button buttonStart = (Button) thisView.findViewById(R.id.button_start);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editTextOutputFolder=(EditText)thisView.findViewById(R.id.folder_created);
                String outputDir= Environment.getExternalStorageDirectory()+"/"+editTextOutputFolder.getText().toString();
                EditText editTextInputFilePath=(EditText)thisView.findViewById(R.id.file_path_input);
                String inputFilePath= editTextInputFilePath.getText().toString();
                VideoToFrames videoToFrames = new VideoToFrames();
                videoToFrames.setCallback(self);
                try {
                    videoToFrames.setSaveFrames(outputDir, outputImageFormat);
                    updateInfo("运行中...");
                    videoToFrames.decode(inputFilePath);
                }catch (Throwable t){
                    t.printStackTrace();
                }
            }
        });
        initImageFormatSpinner();
        return thisView;
    }
    private void initImageFormatSpinner(){
        Spinner barcodeFormatSpinner=(Spinner)thisView.findViewById(R.id.image_format);
        ArrayAdapter<OutputImageFormat> adapter=new ArrayAdapter<>(getActivity(),android.R.layout.simple_spinner_item, OutputImageFormat.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        barcodeFormatSpinner.setAdapter(adapter);
        barcodeFormatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                outputImageFormat = OutputImageFormat.values()[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_CODE_GET_FILE_PATH){
            if(resultCode== Activity.RESULT_OK){
                EditText editText=(EditText)thisView.findViewById(R.id.file_path_input);
                String path = data.getStringExtra("GetFilePath");
                editText.setText(path);
            }
        }
    }
    private void updateInfo(String info){
        TextView textView = (TextView) thisView.findViewById(R.id.info);
        textView.setText(info);
    }
    public void onDecodeFrame(int index){
        Message msg=handler.obtainMessage();
        msg.obj="运行中...第"+index+"帧";
        handler.sendMessage(msg);
    }
    public void onFinishDecode(){
        Message msg=handler.obtainMessage();
        msg.obj="完成！";
        handler.sendMessage(msg);
    }
}
