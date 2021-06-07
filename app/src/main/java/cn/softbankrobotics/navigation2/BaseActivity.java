package cn.softbankrobotics.navigation2;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置全屏
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,
                WindowManager.LayoutParams. FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE);

//        MyApplication.getInstance(); //.addActivity(this);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
//        Application.getInstance().removeActivity(this);
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
