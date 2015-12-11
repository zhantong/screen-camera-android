package com.nju.cs.screencamera;


import android.app.AlertDialog;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity {
    private CameraPreview mPreview;
    final static LinkedBlockingQueue<byte[]> rev=new LinkedBlockingQueue<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        CameraSettings cameraSettings=new CameraSettings();
        cameraSettings=null;
        TextView debugView=(TextView)findViewById(R.id.debug_view);
        TextView infoView=(TextView)findViewById(R.id.info_view);
        debugView.setGravity(Gravity.BOTTOM);
        infoView.setGravity(Gravity.BOTTOM);
    }
    public void openCamera(View view){
        final TextView debugView=(TextView)findViewById(R.id.debug_view);
        final TextView infoView=(TextView)findViewById(R.id.info_view);
        final CameraPreview mPreview=new CameraPreview(this,rev);
        this.mPreview=mPreview;
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        EditText editTextFileName=(EditText)findViewById(R.id.fileName);
        final String newFileName=editTextFileName.getText().toString();
        final Handler nHandler = new Handler();
        Thread worker=new Thread(){
            @Override
            public void run() {
                File out=new File(Environment.getExternalStorageDirectory()+"/Download/"+newFileName);
                ImgToFile imgToFile=new ImgToFile(mPreview,debugView,infoView,nHandler);
                imgToFile.imgsToFile(rev, out);
            }
        };
        worker.start();
    }
    public void stop(View view){
        mPreview.stop();
    }
    public void selectVideoFile(View view){
        File mPath = new File(Environment.getExternalStorageDirectory() + "//DIR//");
        FileDialog fileDialog = new FileDialog(this, mPath);
        //fileDialog.setFileEndsWith(".txt");
        fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
            public void fileSelected(File file) {
                EditText editText = (EditText) findViewById(R.id.videoFilePath);
                editText.setText(file.toString());
            }
        });
        fileDialog.showDialog();
    }
    public boolean checkEndsWithInStringArray(String checkItsEnd,
                                              String[] fileEndings){
        for(String aEnd : fileEndings){
            if(checkItsEnd.endsWith(aEnd))
                return true;
        }
        return false;
    }
    public void openFile(View view){
        EditText editTextFileName=(EditText)findViewById(R.id.fileName);
        String newFileName=editTextFileName.getText().toString();
        String filePath=Environment.getExternalStorageDirectory()+"/Download/"+newFileName;
        File file=new File(filePath);
        if(file.isFile())
        {
            String fileName = file.toString();
            Intent intent;
            if(checkEndsWithInStringArray(fileName, getResources().
                    getStringArray(R.array.fileEndingImage))){
                intent = OpenFiles.getImageFileIntent(file);
                startActivity(intent);
            }else if(checkEndsWithInStringArray(fileName, getResources().
                    getStringArray(R.array.fileEndingWebText))){
                intent = OpenFiles.getHtmlFileIntent(file);
                startActivity(intent);
            }else if(checkEndsWithInStringArray(fileName, getResources().
                    getStringArray(R.array.fileEndingPackage))){
                intent = OpenFiles.getApkFileIntent(file);
                startActivity(intent);

            }else if(checkEndsWithInStringArray(fileName, getResources().
                    getStringArray(R.array.fileEndingAudio))){
                intent = OpenFiles.getAudioFileIntent(file);
                startActivity(intent);
            }else if(checkEndsWithInStringArray(fileName, getResources().
                    getStringArray(R.array.fileEndingVideo))){
                intent = OpenFiles.getVideoFileIntent(file);
                startActivity(intent);
            }else if(checkEndsWithInStringArray(fileName, getResources().
                    getStringArray(R.array.fileEndingText))){
                intent = OpenFiles.getTextFileIntent(file);
                startActivity(intent);
            }else if(checkEndsWithInStringArray(fileName, getResources().
                    getStringArray(R.array.fileEndingPdf))){
                intent = OpenFiles.getPdfFileIntent(file);
                startActivity(intent);
            }else if(checkEndsWithInStringArray(fileName, getResources().
                    getStringArray(R.array.fileEndingWord))){
                intent = OpenFiles.getWordFileIntent(file);
                startActivity(intent);
            }else if(checkEndsWithInStringArray(fileName, getResources().
                    getStringArray(R.array.fileEndingExcel))){
                intent = OpenFiles.getExcelFileIntent(file);
                startActivity(intent);
            }else if(checkEndsWithInStringArray(fileName, getResources().
                    getStringArray(R.array.fileEndingPPT))){
                intent = OpenFiles.getPPTFileIntent(file);
                startActivity(intent);
            }else
            {
                new AlertDialog.Builder(this).setTitle("错误").setItems(new String[] {"无法打开，请安装相应的软件！"},null).setNegativeButton("确定",null).show();
            }
        }else
        {
            new AlertDialog.Builder(this).setTitle("错误").setItems(new String[] {"对不起，这不是文件！"},null).setNegativeButton("确定",null).show();
        }
    }
    public void start(View view){
        //final TextView editText=(TextView)findViewById(R.id.text_view);
        //editText.setText("正在识别...");
        EditText editTextVideoFilePath=(EditText)findViewById(R.id.videoFilePath);
        String videoFilePath=editTextVideoFilePath.getText().toString();
        EditText editTextFileName=(EditText)findViewById(R.id.fileName);
        final String newFileName=editTextFileName.getText().toString();
        final Handler nHandler = new Handler();
        Thread worker=new Thread(){
            @Override
            public void run() {
                File out=new File(Environment.getExternalStorageDirectory()+"/Download/"+newFileName);
                //ImgToFile imgToFile=new ImgToFile(editText,nHandler);
                //imgToFile.imgsToFile(rev, out);
            }
        };
        worker.start();
        System.out.println(videoFilePath);
        VideoToFrames videoToFrames=new VideoToFrames();
        try {
            //videoToFrames.testExtractMpegFrames(rev,videoFilePath);
        }catch (Throwable e){
            e.printStackTrace();
        }
    }
}
