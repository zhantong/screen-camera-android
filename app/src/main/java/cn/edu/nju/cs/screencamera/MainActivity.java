package cn.edu.nju.cs.screencamera;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import cn.edu.nju.cs.screencamera.Logback.ConfigureLogback;
import cn.edu.nju.cs.screencamera.Logback.CustomMarker;

/**
 * UI主要操作
 * 也是控制二维码识别的主要入口
 */
public class MainActivity extends Activity{
    private static Context mContext;

    private BarcodeFormat barcodeFormat;

    public static final int MESSAGE_UI_DEBUG_VIEW=1;
    public static final int MESSAGE_UI_INFO_VIEW=2;

    public static final int REQUEST_CODE_FILE_PATH_INPUT=1;
    public static final int REQUEST_CODE_FILE_PATH_TRUTH=2;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    Logger LOG= LoggerFactory.getLogger(MainActivity.class);

    final Handler mHandler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            String text;
            switch (msg.what){
                case MESSAGE_UI_DEBUG_VIEW:
                    text=(String)msg.obj;
                    TextView debugView=(TextView)findViewById(R.id.debug_view);
                    debugView.setText(text);
                    return true;
                case MESSAGE_UI_INFO_VIEW:
                    text=(String)msg.obj;
                    TextView infoView=(TextView)findViewById(R.id.info_view);
                    infoView.setText(text);
                    return true;
                default:
                    return false;
            }
        }
    });
    /**
     * 界面初始化,设置界面,调用CameraSettings()设置相机参数
     *
     * @param savedInstanceState 默认参数
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext=this;

        TextView debugView = (TextView) findViewById(R.id.debug_view);
        TextView infoView = (TextView) findViewById(R.id.info_view);
        debugView.setGravity(Gravity.BOTTOM);
        infoView.setGravity(Gravity.BOTTOM);


        Button buttonFilePathInput=(Button)findViewById(R.id.button_file_path_input);
        buttonFilePathInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFilePath(REQUEST_CODE_FILE_PATH_INPUT);
            }
        });
        Button buttonFilePathTruth=(Button)findViewById(R.id.button_file_path_truth);
        buttonFilePathTruth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFilePath(REQUEST_CODE_FILE_PATH_TRUTH);
            }
        });

        Button buttonSaveFrames=(Button)findViewById(R.id.save_frames);
        buttonSaveFrames.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFrames(v);
            }
        });

        sharedPref=getSharedPreferences("main", Context.MODE_PRIVATE);
        editor=sharedPref.edit();

        ToggleButton toggleButtonFileNameCreated=(ToggleButton)findViewById(R.id.toggle_file_name_created);
        toggleButtonFileNameCreated.setTag("AUTO_GENERATE_FILE_NAME_CREATED");
        toggleButtonFileNameCreated.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                EditText editTextFileNameCreated=(EditText)findViewById(R.id.file_name_created);
                if(isChecked){
                    String randomFileName= UUID.randomUUID().toString();
                    editTextFileNameCreated.setText(randomFileName);
                    editTextFileNameCreated.setEnabled(false);
                }else{
                    editTextFileNameCreated.setEnabled(true);
                }
                editor.putBoolean((String)buttonView.getTag(),isChecked);
                editor.apply();
            }
        });


        initBarcodeFormatSpinner();
        final EditText editTextFileNameCreated=(EditText)findViewById(R.id.file_name_created);
        editTextFileNameCreated.setTag("FILE_NAME_CREATED");
        editTextFileNameCreated.setText(sharedPref.getString((String)editTextFileNameCreated.getTag(),""));
        editTextFileNameCreated.addTextChangedListener(new EditTextTextWatcher(editTextFileNameCreated));

        final EditText editTextFilePathInput=(EditText)findViewById(R.id.file_path_input);
        editTextFilePathInput.setTag("FILE_PATH_INPUT");
        editTextFilePathInput.setText(sharedPref.getString((String)editTextFilePathInput.getTag(),""));
        editTextFilePathInput.addTextChangedListener(new EditTextTextWatcher(editTextFilePathInput));

        final EditText editTextFilePathTruth=(EditText)findViewById(R.id.file_path_truth);
        editTextFilePathTruth.setTag("FILE_PATH_TRUTH");
        editTextFilePathTruth.setText(sharedPref.getString((String)editTextFilePathTruth.getTag(),""));
        editTextFilePathTruth.addTextChangedListener(new EditTextTextWatcher(editTextFilePathTruth));

        toggleButtonFileNameCreated.setChecked(sharedPref.getBoolean((String)toggleButtonFileNameCreated.getTag(),false));

        ToggleButton toggleButtonFileNameLoggingAuto=(ToggleButton)findViewById(R.id.toggle_file_name_logging_auto);
        toggleButtonFileNameLoggingAuto.setTag("AUTO_GENERATE_FILE_NAME_LOGGING");
        toggleButtonFileNameLoggingAuto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                EditText editTextFileNameLogging=(EditText)findViewById(R.id.file_name_logging);
                if(isChecked){
                    String randomFileName=(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date()))+".txt";
                    editTextFileNameLogging.setText(randomFileName);
                    editTextFileNameLogging.setEnabled(false);
                }else{
                    editTextFileNameLogging.setEnabled(true);
                }
                Switch switchEnableLogging=(Switch)findViewById(R.id.switch_enable_logging);
                switchEnableLogging.setChecked(false);
                editor.putBoolean((String)buttonView.getTag(),isChecked);
                editor.apply();
            }
        });
        Switch switchEnableLogging=(Switch)findViewById(R.id.switch_enable_logging);
        switchEnableLogging.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    EditText editTextFileNameLogging=(EditText)findViewById(R.id.file_name_logging);
                    ConfigureLogback.configureLogbackDirectly(Utils.combinePaths(Environment.getExternalStorageDirectory().getAbsolutePath(),editTextFileNameLogging.getText().toString()));
                    Toast.makeText(getApplicationContext(),"logging",Toast.LENGTH_SHORT).show();
                }
            }
        });

        toggleButtonFileNameLoggingAuto.setChecked(sharedPref.getBoolean((String)toggleButtonFileNameLoggingAuto.getTag(),false));
        switchEnableLogging.setChecked(false);

        Button buttonTest=(Button)findViewById(R.id.button_test);
        buttonTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                test(v);
            }
        });

        Button buttonTest2=(Button)findViewById(R.id.button_test2);
        buttonTest2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                test2(v);
            }
        });
    }
    public static Context getContext(){
        return mContext;
    }

    private class EditTextTextWatcher implements TextWatcher{
        private EditText mEditText;

        public EditTextTextWatcher(EditText editText){
            mEditText=editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            editor.putString((String)mEditText.getTag(),mEditText.getText().toString());
            editor.apply();
        }
    }
    private void initBarcodeFormatSpinner(){
        Spinner barcodeFormatSpinner=(Spinner)findViewById(R.id.barcode_format);
        barcodeFormatSpinner.setTag("BARCODE_FORMAT");
        ArrayAdapter<BarcodeFormat> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,BarcodeFormat.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        barcodeFormatSpinner.setAdapter(adapter);
        barcodeFormatSpinner.setSelection(sharedPref.getInt((String) barcodeFormatSpinner.getTag(), 0));

        barcodeFormatSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                barcodeFormat=BarcodeFormat.values()[position];

                editor.putInt((String)parent.getTag(),position);
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
    public void saveFrames(View view){
        SaveFramesFragment fragment=new SaveFramesFragment();
        getFragmentManager().beginTransaction().replace(R.id.left_part, fragment).addToBackStack(null).commit();
        getFragmentManager().executePendingTransactions();
    }
    private void getFilePath(int requestCode){
        Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if(intent.resolveActivity(getPackageManager())!=null){
            startActivityForResult(Intent.createChooser(intent, "Select a File"), requestCode);
        }else{
            new AlertDialog.Builder(this).setTitle("未找到文件管理器")
                    .setMessage("请安装文件管理器以选择文件")
                    .setPositiveButton("确定",null)
                    .show();
        }
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        int id=0;
        switch (requestCode){
            case REQUEST_CODE_FILE_PATH_INPUT:
                id=R.id.file_path_input;
                break;
            case REQUEST_CODE_FILE_PATH_TRUTH:
                id=R.id.file_path_truth;
                break;
        }
        if (resultCode == RESULT_OK) {
            EditText editText = (EditText) findViewById(id);
            String curFileName=data.getData().getPath();
            editText.setText(curFileName);
        }
    }

    /**
     * 处理视频文件,从视频帧识别二维码
     *
     * @param view 默认参数
     */
    public void processVideo(View view) {
        EditText editTextVideoFilePath = (EditText) findViewById(R.id.file_path_input);
        final String videoFilePath = editTextVideoFilePath.getText().toString();
        EditText editTextFileName = (EditText) findViewById(R.id.file_name_created);
        final String newFileName = editTextFileName.getText().toString();
        EditText editTextTruthFilePath = (EditText) findViewById(R.id.file_path_truth);
        final String truthFilePath = editTextTruthFilePath.getText().toString();

        LOG.info(CustomMarker.source,videoFilePath);

        Thread worker = new Thread() {
            @Override
            public void run() {
                MultiFormatStream.decode(barcodeFormat,videoFilePath,null,null);
            }
        };
        worker.start();
    }

    /**
     * 处理单个图片,识别二维码
     *
     * @param view 默认参数
     */
    public void processImg(View view) {
        EditText editTextVideoFilePath = (EditText) findViewById(R.id.file_path_input);
        final String imageFilePath = editTextVideoFilePath.getText().toString();
        EditText editTextTruthFilePath = (EditText) findViewById(R.id.file_path_truth);
        final String truthFilePath = editTextTruthFilePath.getText().toString();
        Thread worker = new Thread() {
            @Override
            public void run() {
                MultiFormatStream.decode(barcodeFormat,null,imageFilePath,null);
            }
        };
        worker.start();
    }

    public void test(View view){
        EditText editTextInputFilePath = (EditText) findViewById(R.id.file_path_input);
        final String inputFilePath = editTextInputFilePath.getText().toString();
        ParseImage parseImage=new ParseImage(inputFilePath);
    }

    public void test2(View view){
        EditText editTextInputFilePath = (EditText) findViewById(R.id.file_path_input);
        final String inputFilePath = editTextInputFilePath.getText().toString();
        ParseImage parseImage=new ParseImage(inputFilePath,true);
    }

    /**
     * 在APP内打开文件
     *
     * @param view 默认参数
     */
    public void openFile(View view) {
        EditText editTextFileName = (EditText) findViewById(R.id.file_name_created);
        String originFileName = editTextFileName.getText().toString();
        File file=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),originFileName);
        file=correctFileExtension(file);
        String correctedFileName=file.getName();
        if(!correctedFileName.equals(originFileName)){
            editTextFileName.setText(correctedFileName);
        }

        String mimeType=URLConnection.guessContentTypeFromName(correctedFileName);
        if(mimeType!=null) {
            Intent newIntent=new Intent(Intent.ACTION_VIEW);
            newIntent.setDataAndType(Uri.fromFile(file), mimeType);
            newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(newIntent);
        }else{
            new AlertDialog.Builder(this).setTitle("未识别的文件类型")
                    .setMessage("未识别的文件后缀名或文件内容")
                    .setPositiveButton("确定",null)
                    .show();
        }
    }
    private File correctFileExtension(File file){
        String originFileName = file.getName();
        int lastSeparatorIndex=originFileName.lastIndexOf('.');
        String fileNameWithoutExtension=originFileName;
        String originExtension="";
        if(lastSeparatorIndex!=-1){
            fileNameWithoutExtension=originFileName.substring(0,lastSeparatorIndex);
            originExtension=originFileName.substring(lastSeparatorIndex+1);
        }
        String correctedExtension=getFileExtension(file);
        if(!correctedExtension.equals(originExtension)){
            String correctedFileName=fileNameWithoutExtension+"."+correctedExtension;
            File newFile=new File(file.getParent(),correctedFileName);
            file.renameTo(newFile);
            file=newFile;
        }
        return file;
    }
    private String getFileExtension(File file){
        if(!file.isFile()){
            throw new RuntimeException("file not exists");
        }
        ContentInfoUtil util=new ContentInfoUtil();
        ContentInfo info;
        try {
            info = util.findMatch(file);
        }catch (IOException e){
            throw new RuntimeException("file not exists");
        }
        if(info==null){
            return "txt";
        }
        return info.getFileExtensions()[0];
    }
    public void processCamera(View view){
        final CameraPreviewFragment fragment=new CameraPreviewFragment();
        fragment.addCallback(new CameraPreviewFragment.OnStartListener() {
            @Override
            public void onStartRecognize() {
                EditText editTextFileName = (EditText) findViewById(R.id.file_name_created);
                final String newFileName = editTextFileName.getText().toString();
                EditText editTextTruthFilePath = (EditText) findViewById(R.id.file_path_truth);
                final String truthFilePath = editTextTruthFilePath.getText().toString();
                Thread worker = new Thread() {
                    @Override
                    public void run() {
                        MultiFormatStream.decode(barcodeFormat,null,null,fragment.mPreview);
                        getFragmentManager().popBackStack();
                    }
                };
                worker.start();
            }
        });

        getFragmentManager().beginTransaction().replace(R.id.left_part, fragment).addToBackStack(null).commit();
        getFragmentManager().executePendingTransactions();
    }
}
