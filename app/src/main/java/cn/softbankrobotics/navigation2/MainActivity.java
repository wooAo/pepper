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
    //??????Frame?????????
    private LinkedHashMap<String, AttachedFrame> mSavedFrames = new LinkedHashMap<>();
    private String mCurrentMapName;
    //????????????
    private NavigationService mNavigationService;
    private boolean mDoLocalize = false;
    Dialog dia;
    ImageView imageView;

    private Fragment mCurrentFragment = new Fragment();//???????????????
    private CreateMapFragment mCreateMapFragment = new CreateMapFragment();//??????????????????
    private LoadMapFragment mLoadMapFragment = new LoadMapFragment();//??????????????????
    private NavigationFragment mNavigationFragment = new NavigationFragment();//??????????????????

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

    //???????????????
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

    //Fragment??????
    private FragmentTransaction switchFragment(Fragment targetFragment) {

        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();
        if (!targetFragment.isAdded()) {
            //???????????????switchFragment()???currentFragment???null????????????????????????
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

    //??????????????????
    @AfterPermissionGranted(PERMISSIONS)
    private void requestPermission() {
        if (EasyPermissions.hasPermissions(this, mPerms)) {
            //LogUtils.d(TAG, "??????????????????????????????");
        } else {
            EasyPermissions.requestPermissions(this, getString(R.string.request_permission), PERMISSIONS, mPerms);
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        if (requestCode == PERMISSIONS) {
            LogUtils.d(TAG, "onPermissionsGranted: ????????????");
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        LogUtils.d(TAG, "onPermissionsDenied: ????????????");
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

    //?????????????????????
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
                                    //?????????????????????????????????????????????????????????????????????????????????
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

    //??????????????????????????????
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
                        LogUtils.d(TAG, "???????????????????????????" + name + "???????????????" + password);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });
        builder.show();
    }

    //??????????????????
    public void openCreateMapFragment() {
        mDoLocalize = true;
        switchFragment(mCreateMapFragment).commit();
        runOnUiThread(() -> mNavigationView.setSelectedItemId(mNavigationView.getMenu().getItem(0).getItemId()));
    }

    //?????????????????????????????????????????????????????????????????????????????????????????????
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

    //????????????????????????????????????????????????????????????????????????????????????????????????
    public void releaseAbilities() {
        // Release the holder asynchronously.
        try {
            holder.async().release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //?????????????????????????????????
    protected void hideBottomUIMenu() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }
}
