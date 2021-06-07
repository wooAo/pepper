package cn.softbankrobotics.navigation2;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.HolderBuilder;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
//import com.example.sisyphus.robot.service.BaseActivity;
import com.aldebaran.qi.sdk.object.actuation.AttachedFrame;
import com.aldebaran.qi.sdk.object.actuation.ExplorationMap;
import com.aldebaran.qi.sdk.object.holder.AutonomousAbilitiesType;
import com.aldebaran.qi.sdk.object.holder.Holder;
import com.aldebaran.qi.sdk.object.touch.Touch;
import com.aldebaran.qi.sdk.object.touch.TouchSensor;
import com.aldebaran.qi.sdk.object.touch.TouchState;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.cache.CacheMode;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.softbankrobotics.transam.connection.api.RobotConnectionConfigration;
import com.softbankrobotics.transam.navigation.naoqi.NavigationListener;
import com.softbankrobotics.transam.navigation.naoqi.NavigationService;
import com.softbankrobotics.transam.navigation.naoqi.NavigationServiceBuild;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import cn.softbankrobotics.navigation2.callback.EnterRobotInfoCallBack;
import cn.softbankrobotics.navigation2.callback.InitServiceCallBack;
import cn.softbankrobotics.navigation2.fragment.CreateMapFragment;
import cn.softbankrobotics.navigation2.fragment.LoadMapFragment;
import cn.softbankrobotics.navigation2.fragment.NavigationFragment;
import cn.softbankrobotics.navigation2.utils.FolderUtils;
import cn.softbankrobotics.navigation2.utils.GoToUtils;
import cn.softbankrobotics.navigation2.utils.LogUtils;
import cn.softbankrobotics.navigation2.utils.ToolUtils;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends BaseActivity implements RobotLifecycleCallbacks, EasyPermissions.PermissionCallbacks {

    private final String TAG = "MainActivity";
    private QiContext mQiContext;
    private ExplorationMap mExplorationMap;
    private Holder holder;
    public GoToUtils goToUtils;
    //存储Frame的集合
    private LinkedHashMap<String, AttachedFrame> mSavedFrames = new LinkedHashMap<>();
    private String mCurrentMapName;
    //导航服务
    private NavigationService mNavigationService;
    private boolean mDoLocalize = false;
    Dialog dia;
    ImageView imageView;

    private Fragment mCurrentFragment = new Fragment();//当前的界面
    private CreateMapFragment mCreateMapFragment = new CreateMapFragment();//创建地图界面
    private LoadMapFragment mLoadMapFragment = new LoadMapFragment();//加载地图界面
    private NavigationFragment mNavigationFragment = new NavigationFragment();//移动导航界面

    private String[] mPerms = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,};
    private static final int PERMISSIONS = 100;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.item_create_map:
                    switchFragment(mCreateMapFragment).commit();
                    return true;
                case R.id.item_load_map:
                    switchFragment(mLoadMapFragment).commit();
                    return true;
                case R.id.item_navigation:
                    switchFragment(mNavigationFragment).commit();
                    return true;
            }
            return false;
        }
    };

    private BottomNavigationView mNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        QiSDK.register(this, this);
        requestPermission();
        if(getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initView();
        goToUtils = new GoToUtils();
        //hideBottomUIMenu();

        /*holder = HolderBuilder.with(mQiContext)
                .withAutonomousAbilities(
                        //AutonomousAbilitiesType.BACKGROUND_MOVEMENT,
                        AutonomousAbilitiesType.BASIC_AWARENESS,
                        AutonomousAbilitiesType.AUTONOMOUS_BLINKING
                )
                .build();
        holder.async().release();*/
    }

    //初始化界面
    private void initView() {
        switchFragment(mCreateMapFragment).commit();
        mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        LogUtils.d(TAG, "Robot Focus Gained");
        Touch touch=qiContext.getTouch();
        List<String> sensorNames = touch.getSensorNames();
        this.mQiContext = qiContext;
        MyApplication.getInstance().setQiContext(qiContext);
    }

    @Override
    public void onRobotFocusLost() {
        LogUtils.d(TAG, "Robot Focus Lost");
        goToUtils.checkAndCancelCurrentGoto();
    }

    @Override
    public void onRobotFocusRefused(String reason) {
        LogUtils.d(TAG, "onRobotFocusRefused: " + reason);
    }

    @Override
    protected void onDestroy() {
        QiSDK.unregister(this, this);
        super.onDestroy();
    }

    public String getCurrentMapName() {
        return mCurrentMapName;
    }

    public void setCurrentMapName(String mapName) {
        this.mCurrentMapName = mapName;
    }

    public LinkedHashMap<String, AttachedFrame> getSavedFrames() {
        return mSavedFrames;
    }

    public ExplorationMap getExplorationMap() {
        return mExplorationMap;
    }

    public void setExplorationMap(ExplorationMap explorationMap) {
        this.mExplorationMap = explorationMap;
    }

    public NavigationService getNavigationService() {
        return this.mNavigationService;
    }

    public boolean getDoLocalize() {
        return mDoLocalize;
    }

    public void setDoLocalize(boolean doLocalize) {
        this.mDoLocalize = doLocalize;
    }

    //Fragment优化
    private FragmentTransaction switchFragment(Fragment targetFragment) {

        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();
        if (!targetFragment.isAdded()) {
            //第一次使用switchFragment()时currentFragment为null，所以要判断一下
            if (mCurrentFragment != null) {
                transaction.hide(mCurrentFragment);
            }
            transaction.add(R.id.fragment, targetFragment, targetFragment.getClass().getName());
        } else {
            transaction.hide(mCurrentFragment).show(targetFragment);
        }
        mCurrentFragment = targetFragment;
        return transaction;
    }

    /**
     * Send the robot to the desired position.
     *
     * @param location the name of the location
     */
    public void goToLocation(Map.Entry<String, AttachedFrame> location) {
        LogUtils.d(TAG,"");
        goToUtils.checkAndCancelCurrentGoto().thenConsume(aVoid -> {
            holdAbilities();
            goToUtils.goTo(location.getValue());
        });
    }

    //请求所需权限
    @AfterPermissionGranted(PERMISSIONS)
    private void requestPermission() {
        if (EasyPermissions.hasPermissions(this, mPerms)) {
            //LogUtils.d(TAG, "已经获取读写内存权限");
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.request_permission), PERMISSIONS, mPerms);
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        if (requestCode == PERMISSIONS) {
            LogUtils.d(TAG, "onPermissionsGranted: 获取权限");
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        LogUtils.d(TAG, "onPermissionsDenied: 拒绝权限");
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this)
                    .setRationale(R.string.permission_rationale)
                    .setTitle(R.string.necessary_permission)
                    .build()
                    .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    //初始化导航服务
    public void initNavigationService(InitServiceCallBack callBack) {
        if (mNavigationService == null) {
            enterRobotInfo(new EnterRobotInfoCallBack() {
                @Override
                public void setRobotConnectionConfiguration(RobotConnectionConfigration robotConnectionConfigration) {
                    Executor executor = Executors.newSingleThreadExecutor();
                    executor.execute(() -> {
                        mNavigationService = new NavigationServiceBuild().setConnection(robotConnectionConfigration).build();
                        mNavigationService.init(MainActivity.this);
                        mNavigationService.addListener(new NavigationListener() {
                            @Override
                            public void obtainBitmap(byte[] bytes) {
                                if (bytes.length != 0) {
                                    LogUtils.d(TAG, getString(R.string.obtain_image_successful));
                                    try {
                                        File cachePicFile = new File(FolderUtils.getMapFolder(MainActivity.this, mCurrentMapName), Constant.PIC_FILE_NAME);
                                        FileUtils.writeByteArrayToFile(cachePicFile, bytes);
                                    } catch (IOException e) {
                                        LogUtils.e(TAG, getString(R.string.save_image_failed) + ":" + e.getMessage());
                                        runOnUiThread(() -> ToolUtils.showToast(MainActivity.this, getString(R.string.save_image_failed)));
                                    }
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                    runOnUiThread(() -> {
                                        if (mCreateMapFragment.isVisible()) {
                                            mCreateMapFragment.mapImageView.setImageBitmap(bitmap);
                                        } else if (mLoadMapFragment.isVisible()) {
                                            mLoadMapFragment.mapImageView.setImageBitmap(bitmap);
                                        }
                                    });
                                    //创建地图界面，显示地图图片后，需要先定位才能保存标记点
                                    if (mCreateMapFragment.isVisible()) {
                                        mCreateMapFragment.startLocalize();
                                    }
                                } else {
                                    LogUtils.d(TAG, getString(R.string.obtain_image_failed));
                                    runOnUiThread(() -> ToolUtils.showToast(MainActivity.this, getString(R.string.obtain_image_failed)));
                                }
                            }

                            @Override
                            public void onError(String s) {
                                LogUtils.d(TAG, getString(R.string.obtain_image_failed) + ";" + s);
                                runOnUiThread(() -> ToolUtils.showToast(MainActivity.this, getString(R.string.obtain_image_failed)));
                            }
                        });
                        callBack.onInitServiceSuccess();
                    });
                }
            });
        } else {
            Executor executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> callBack.onInitServiceSuccess());
        }
    }

    //输入机器人名称和密码
    private void enterRobotInfo(EnterRobotInfoCallBack enterRobotInfoCallBack) {
        LayoutInflater from = LayoutInflater.from(MainActivity.this);
        View inflate = from.inflate(R.layout.dialog_robot_info, null);
        EditText robotName = inflate.findViewById(R.id.robot_name);
        EditText robotPassword = inflate.findViewById(R.id.robot_password);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getResources().getString(R.string.set_robot_info)).setIcon(android.R.drawable.ic_dialog_info).setView(inflate)
                .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
        builder.setPositiveButton(getResources().getString(R.string.confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String name = robotName.getText().toString();
                String password = robotPassword.getText().toString();
                if (name.isEmpty() || password.isEmpty()) {
                    ToolUtils.showToast(MainActivity.this, getString(R.string.reenter_robot_info));
                    return;
                }
                Executor executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    try {
                        RobotConnectionConfigration robotConnectionConfigration = new RobotConnectionConfigration();
                        robotConnectionConfigration.setAccount(name);
                        robotConnectionConfigration.setPassword(password);
                        enterRobotInfoCallBack.setRobotConnectionConfiguration(robotConnectionConfigration);
                        LogUtils.d(TAG, "设置机器人名称为：" + name + "；密码为：" + password);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });
        builder.show();
    }

    //打开建图界面
    public void openCreateMapFragment() {
        mDoLocalize = true;
        switchFragment(mCreateMapFragment).commit();
        runOnUiThread(() -> mNavigationView.setSelectedItemId(mNavigationView.getMenu().getItem(0).getItemId()));
    }

    //关闭自主生活模式（建议建图、定位、导航的时候关闭自主生活模式）
    public void holdAbilities() {
        // Build the holder for the abilities.
        holder = HolderBuilder.with(mQiContext)
                .withAutonomousAbilities(
                        //AutonomousAbilitiesType.BACKGROUND_MOVEMENT,
                        AutonomousAbilitiesType.BASIC_AWARENESS,
                        AutonomousAbilitiesType.AUTONOMOUS_BLINKING
                )
                .build();

        // Hold the abilities asynchronously.
        holder.async().hold();
    }

    //打开自主生活模式（可以根据业务场景在适当的时候打开自主生活模式）
    public void releaseAbilities() {
        // Release the holder asynchronously.
        try {
            holder.async().release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //隐藏虚拟按键，并且全屏
    protected void hideBottomUIMenu() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }
}
