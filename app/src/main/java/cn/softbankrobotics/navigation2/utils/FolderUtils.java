package cn.softbankrobotics.navigation2.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;

public class FolderUtils {
    private static final String TAG = "FolderUtils";

    //获取数据文件夹路径
    public static File getDataFolder(Context context) {
        File folder = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), context.getPackageName());
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                Log.d(TAG, "创建数据文件夹成功");
            } else {
                Log.d(TAG, "创建数据文件夹失败");
            }
        }
        return folder;
    }

    //获取地图文件保存路径
    public static File getMapFolder(Context context, String folderName) {
        File folder = new File(getDataFolder(context), folderName);
        if (!folder.exists()) {
            if (folder.mkdirs()) {
                Log.d(TAG, "创建地图文件夹成功");
            } else {
                Log.d(TAG, "创建地图文件夹失败");
            }
        }
        return folder;
    }
}
