package com.nju.cs.screencamera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    ArrayList<Bitmap> rev=new ArrayList<>();
    public void click(View view){
        File videoFile=new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/testvideo.mp4");
        MediaMetadataRetriever retriever=new MediaMetadataRetriever();
        retriever.setDataSource(videoFile.getAbsolutePath());
        String duration=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        for(int i=0;i<Integer.parseInt(duration);i+=200){
            Bitmap bitmap=retriever.getFrameAtTime(i*1000,MediaMetadataRetriever.OPTION_CLOSEST);
//            File f=new File(Environment.getExternalStorageDirectory()+"/Test/"+i+".jpeg");
//            try {
//                FileOutputStream out = new FileOutputStream(f);
//
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
//                out.flush();
//                out.close();
//            }catch (Exception e){
//                e.printStackTrace();
//            }
            rev.add(bitmap);
        }
        TextView editText=(TextView)findViewById(R.id.textV);
        editText.setText(duration);
        ImgToFile imgToFile=new ImgToFile();
        File out=new File(Environment.getExternalStorageDirectory()+"/out.txt");
        imgToFile.imgsToFile(rev,out);
    }
    public void dealWith(ArrayList<Bitmap> bitMapList){
        TextView editText=(TextView)findViewById(R.id.textV);

        for(Bitmap b:bitMapList){
            int pix=b.getPixel(500,500);
            editText.setText(Integer.toString(pix));
        }
    }
}
