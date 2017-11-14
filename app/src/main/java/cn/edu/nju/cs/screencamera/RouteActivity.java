package cn.edu.nju.cs.screencamera;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Created by zhantong on 2017/11/14.
 */

public class RouteActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        Button btnFromFile = findViewById(R.id.btn_from_file);
        btnFromFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RouteActivity.this, FileActivity.class);
                startActivity(intent);
            }
        });
        Button btnFromCamera = findViewById(R.id.btn_from_camera);
        btnFromCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RouteActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });
    }
}
