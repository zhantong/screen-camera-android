package com.nju.cs.screencamera;


import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
    BlockingDeque<Bitmap> rev=new LinkedBlockingDeque<Bitmap>();

    public void click(View view){
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
                File out=new File(Environment.getExternalStorageDirectory()+"/out3.txt");
                ImgToFile imgToFile=new ImgToFile();
                imgToFile.imgsToFile(rev,out);
            }
        };
        worker.start();

        VideoToFrames videoToFrames=new VideoToFrames();
        try {
            videoToFrames.testExtractMpegFrames(rev);
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
        TextView editText=(TextView)findViewById(R.id.textV);

        for(Bitmap b:bitMapList){
            int pix=b.getPixel(500,500);
            editText.setText(Integer.toString(pix));
        }

    }

}
