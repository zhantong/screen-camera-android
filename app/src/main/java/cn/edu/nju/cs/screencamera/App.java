package cn.edu.nju.cs.screencamera;

import android.app.Application;
import android.content.Context;

/**
 * Created by zhantong on 2017/11/16.
 */

public class App extends Application {
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    static Context getContext() {
        return context;
    }
}
