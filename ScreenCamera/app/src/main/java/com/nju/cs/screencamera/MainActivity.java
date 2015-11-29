package com.nju.cs.screencamera;


import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    BlockingDeque<Bitmap> rev=new LinkedBlockingDeque<Bitmap>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void selectVideoFile(View view){
        File mPath = new File(Environment.getExternalStorageDirectory() + "//DIR//");
        FileDialog fileDialog = new FileDialog(this, mPath);
        fileDialog.setFileEndsWith(".txt");
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
        if(file!=null&&file.isFile())
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
        final TextView editText=(TextView)findViewById(R.id.textView);
        editText.setText("正在识别...");
        EditText editTextVideoFilePath=(EditText)findViewById(R.id.videoFilePath);
        String videoFilePath=editTextVideoFilePath.getText().toString();
        EditText editTextFileName=(EditText)findViewById(R.id.fileName);
        final String newFileName=editTextFileName.getText().toString();
        final Handler mHandler = new Handler(){

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 0:
                        editText.setText("识别完成!");
                        break;
                    default:
                        break;
                }
            }

        };
        final long TIMEOUT=1000l;
        Thread worker=new Thread(){
            @Override
            public void run() {
                /*
                while(true){
                    try {
                        Bitmap bitmap = rev.poll(TIMEOUT, TimeUnit.MILLISECONDS);
                        if(bitmap==null){
                            continue;
                        }
                        bitmap.recycle();
                        bitmap=null;
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                */
                File out=new File(Environment.getExternalStorageDirectory()+"/Download/"+newFileName);
                ImgToFile imgToFile=new ImgToFile();
                imgToFile.imgsToFile(rev, out);
                mHandler.sendEmptyMessage(0);
            }
        };
        worker.start();
        System.out.println(videoFilePath);
        VideoToFrames videoToFrames=new VideoToFrames();
        try {
            videoToFrames.testExtractMpegFrames(rev,videoFilePath);
        }catch (Throwable e){
            e.printStackTrace();
        }

        /*
        String filePath=Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/test3.mp4";
        File videoFile=new File(filePath);

        TextView editText=(TextView)findViewById(R.id.textV);
        //editText.setText(duration);
        ImgToFile imgToFile=new ImgToFile();
        File out=new File(Environment.getExternalStorageDirectory()+"/out3.txt");
        imgToFile.imgsToFile(rev,out);
        */
    }
    public void dealWith(ArrayList<Bitmap> bitMapList){
        TextView editText=(TextView)findViewById(R.id.textView);

        for(Bitmap b:bitMapList){
            int pix=b.getPixel(500,500);
            editText.setText(Integer.toString(pix));
        }

    }

}
