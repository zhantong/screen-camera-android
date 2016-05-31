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
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;


import java.io.File;
import java.io.FileInputStream;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 * UI主要操作
 * 也是控制二维码识别的主要入口
 */
public class MainActivity extends Activity implements CameraPreviewFragment.OnStartListener,View.OnFocusChangeListener{
    private CameraPreview mPreview;//相机
    private BarcodeFormat barcodeFormat;
    CameraPreviewFragment fragment;

    public static final int MESSAGE_UI_DEBUG_VIEW=1;
    public static final int MESSAGE_UI_INFO_VIEW=2;

    public static final int REQUEST_CODE_FILE_PATH_INPUT=1;
    public static final int REQUEST_CODE_FILE_PATH_TRUTH=2;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

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

        initBarcodeFormatSpinner();
        final EditText editTextFileNameCreated=(EditText)findViewById(R.id.file_name_created);
        editTextFileNameCreated.setTag("FILE_NAME_CREATED");
        editTextFileNameCreated.setText(sharedPref.getString((String)editTextFileNameCreated.getTag(),""));
        editTextFileNameCreated.setOnFocusChangeListener(this);

        final EditText editTextFilePathInput=(EditText)findViewById(R.id.file_path_input);
        editTextFilePathInput.setTag("FILE_PATH_INPUT");
        editTextFilePathInput.setText(sharedPref.getString((String)editTextFilePathInput.getTag(),""));
        editTextFilePathInput.setOnFocusChangeListener(this);

        final EditText editTextFilePathTruth=(EditText)findViewById(R.id.file_path_truth);
        editTextFilePathTruth.setTag("FILE_PATH_TRUTH");
        editTextFilePathTruth.setText(sharedPref.getString((String)editTextFilePathTruth.getTag(),""));
        editTextFilePathTruth.setOnFocusChangeListener(this);
    }
    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        EditText editText=(EditText)v;
        if(!hasFocus){
            editor.putString((String)editText.getTag(),editText.getText().toString());
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

    /**
     * 释放相机
     *
     * @param view 默认参数
     */
    public void stop(View view) {
        mPreview.stop();
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
        Thread worker = new Thread() {
            @Override
            public void run() {
                VideoToFile videoToFile=new VideoToFile(mHandler,barcodeFormat,truthFilePath);
                videoToFile.toFile(newFileName, videoFilePath);
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
                SingleImgToFile singleImgToFile=new SingleImgToFile(mHandler,barcodeFormat,truthFilePath);
                singleImgToFile.singleImg(imageFilePath);
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
        EditText editTextFileName = (EditText) findViewById(R.id.file_name_created);
        String newFileName = editTextFileName.getText().toString();
        String filePath = Environment.getExternalStorageDirectory() + "/Download/" + newFileName;
        Intent newIntent=new Intent(Intent.ACTION_VIEW);
        String mimeType="";
        File file = new File(filePath);
        try {
            mimeType = URLConnection.guessContentTypeFromName(URLEncoder.encode(file.getAbsolutePath(), "UTF-8"));
            if(mimeType==null){
                mimeType=URLConnection.guessContentTypeFromStream(new FileInputStream(file));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        if(mimeType!=null) {
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
    public void processCamera(View view){
        fragment=new CameraPreviewFragment();
        getFragmentManager().beginTransaction().replace(R.id.left_part, fragment).addToBackStack(null).commit();
        getFragmentManager().executePendingTransactions();
    }
    public void onStartReco(){
        EditText editTextFileName = (EditText) findViewById(R.id.file_name_created);
        final String newFileName = editTextFileName.getText().toString();
        EditText editTextTruthFilePath = (EditText) findViewById(R.id.file_path_truth);
        final String truthFilePath = editTextTruthFilePath.getText().toString();
        Thread worker = new Thread() {
            @Override
            public void run() {
                CameraToFile cameraToFile=new CameraToFile(mHandler,barcodeFormat,truthFilePath);
                cameraToFile.toFile(newFileName, fragment.mPreview);
            }
        };
        worker.start();
    }
}
