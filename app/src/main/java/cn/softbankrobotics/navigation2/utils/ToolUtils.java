package cn.softbankrobotics.navigation2.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.widget.ImageView;
import android.widget.Toast;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.geometry.Quaternion;
import com.aldebaran.qi.sdk.object.geometry.Transform;
import com.aldebaran.qi.sdk.object.geometry.Vector3;
import com.softbankrobotics.transam.navigation.naoqi.NavigationService;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import cn.softbankrobotics.navigation2.Constant;
import cn.softbankrobotics.navigation2.MyApplication;
import cn.softbankrobotics.navigation2.R;
import cn.softbankrobotics.navigation2.callback.SayCallBack;

public class ToolUtils {
    private static final String TAG = "ToolUtils";
    //LookAt的点距离当前点的距离，由于没有控制旋转的API，所以设置LookAt的点距离很远就相当于向前平视了
    private static final int DISTANCE = 100;

    //吐司显示提示信息
    public static void showToast(Context context, String info) {
        Toast.makeText(context, info, Toast.LENGTH_LONG).show();
    }

    //提示信息
    public static Future<Void> sayAsync(String text) {

        Future<Void> sayFuture = SayBuilder.with(MyApplication.getInstance().getQiContext())
                .withText(text)
                .build().async().run();
        sayFuture.thenConsume(future -> {
            if (future.hasError()) {
                LogUtils.e(TAG, "say " + text + " error:" + future.getError());
            }
        });
        return sayFuture;
    }

    // 异步创建异步执行
    public static Future<Void> sayAndAsync(String text) {

        return SayBuilder.with(MyApplication.getInstance().getQiContext())
                .withText(text)
                .buildAsync()
                .andThenCompose(say -> say.async().run())
                .thenConsume(future -> {
                    if (future.hasError()) {
                        LogUtils.e(TAG, "say " + text + " error:" + future.getError());
                    }
                });
    }

    //提示信息（可以传回调）
    public static Future<Void> sayAsync(String text, SayCallBack sayCallBack) {
        Future<Void> sayFuture = SayBuilder.with(MyApplication.getInstance().getQiContext())
                .withText(text)
                .build().async().run();
        sayFuture.thenConsume(future -> {
            if (future.hasError()) {
                LogUtils.e(TAG, "say " + text + " error:" + future.getError());
            }
            sayCallBack.onSayDone();
        });
        return sayFuture;
    }

    //提示信息（可以传回调）
    public static Future<Void> sayAndAsync(String text, SayCallBack sayCallBack) {
        return SayBuilder.with(MyApplication.getInstance().getQiContext())
                .withText(text)
                .buildAsync()
                .andThenCompose(say -> say.async().run())
                .thenConsume(future -> {
                    if (future.hasError()) {
                        LogUtils.e(TAG, "say " + text + " error:" + future.getError());
                    }
                    sayCallBack.onSayDone();
                });
    }


    //加载显示地图
    public static void loadMapPic(Activity activity, NavigationService navigationService, String mapName, ImageView imageView) {
        File cachePicFile = new File(FolderUtils.getMapFolder(activity, mapName), Constant.PIC_FILE_NAME);
        if (cachePicFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(cachePicFile.getAbsolutePath());
            if (bitmap != null) {
                activity.runOnUiThread(() -> imageView.setImageBitmap(bitmap));
            } else {
                LogUtils.d(TAG, activity.getString(R.string.load_map_image_failed));
                activity.runOnUiThread(() -> showToast(activity, activity.getString(R.string.load_map_image_failed)));
            }
        } else {
            try {
                File mapFile = new File(FolderUtils.getMapFolder(activity, mapName), Constant.MAP_FILE_NAME);
                InputStream inputStream = new FileInputStream(mapFile);
                navigationService.getMapPicture(inputStream);
            } catch (Exception e) {
                LogUtils.d(TAG, activity.getString(R.string.read_map_data_failed) + ":" + e.toString());
                activity.runOnUiThread(() -> showToast(activity, activity.getString(R.string.read_map_data_failed)));
            }
        }
    }

    //计算目标点与给定原点之间的最后一次已知变换
    public static Transform getTransform(@NonNull Frame baseFrame, @NonNull Frame destinationFrame) {
        return destinationFrame.async().computeTransform(baseFrame).getValue().getTransform();
    }

    //根据Quaternion获取LookAt的点的坐标
    public static Vector3 getLookAtVector3(Quaternion rotation) {
        double x = rotation.getX();
        double y = rotation.getY();
        double z = rotation.getZ();
        double w = rotation.getW();
        //获取在Z轴上的旋转弧度
        double angle_z = Math.atan2(2 * (x * y + w * z), 1 - 2 * (y * y + z * z));
        double vx = DISTANCE * Math.cos(angle_z);//LookAt的点的x轴坐标
        double vy = DISTANCE * Math.sin(angle_z);//LookAt的点的y轴坐标
        return new Vector3(vx, vy, 0);
    }
}
