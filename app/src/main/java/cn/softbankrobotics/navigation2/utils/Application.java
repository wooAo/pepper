package cn.softbankrobotics.navigation2.utils;

import android.app.Activity;
import android.content.Context;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.cache.CacheEntity;
import com.lzy.okgo.cache.CacheMode;
import com.lzy.okgo.cookie.CookieJarImpl;
import com.lzy.okgo.cookie.store.SPCookieStore;
import com.lzy.okgo.https.HttpsUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * 基础Application类，必须在androidManifest.xml中指定
 */
public class Application extends android.app.Application {

    private static Application instance;
    public static int screenWidth, screenHeight;
    private static List<Activity> activityList = new LinkedList<Activity>();


    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
//        screenWidth = DeviceInfoUtils.getScreenWidth(this);//获取屏幕宽度
//        screenHeight = DeviceInfoUtils.getScreenHeight(this);//获取屏幕高度
        //科大讯飞语音 进行SDK的初始化
//        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=5cd245c9");
        //腾讯bugly 日志监听
//        CrashReport.initCrashReport(getApplicationContext(), "50b7f8c5d6", false);
        //OKHttputils初始化
        setUpOkhttpConfig();
    }

    /***
     * 设置网络请求的相关参数
     */
    private void setUpOkhttpConfig() {
//        OkGo.getInstance().init(this);
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        //全局的读取超时时间
        builder.readTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        //全局的写入超时时间
        builder.writeTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        //全局的连接超时时间
        builder.connectTimeout(15000, TimeUnit.MILLISECONDS);
        //使用sp保持cookie，如果cookie不过期，则一直有效
        builder.cookieJar(new CookieJarImpl(new SPCookieStore(this)));
        //信任所有证书
        HttpsUtils.SSLParams sslParams1 = HttpsUtils.getSslSocketFactory();
        builder.sslSocketFactory(sslParams1.sSLSocketFactory, sslParams1.trustManager);
        OkGo.getInstance().init(this)                       //必须调用初始化
                .setOkHttpClient(builder.build())               //建议设置OkHttpClient，不设置将使用默认的
                .setCacheMode(CacheMode.NO_CACHE)               //全局统一缓存模式，默认不使用缓存，可以不传
                .setCacheTime(CacheEntity.CACHE_NEVER_EXPIRE)   //全局统一缓存时间，默认永不过期，可以不传
                .setRetryCount(3);                              //全局统一超时重连次数，默认为三次，那么最差的情况会请求4次(一次原始请求，三次重连请求)，不需要可以设置为0
//                .addCommonHeaders(headers)                      //全局公共头
//                .addCommonParams(params);                       //全局公共参数
    }

    public static Application getInstance() {
        return instance;
    }





    public void addActivity(Activity activity) {
        activityList.add(activity);
    }

    public void removeActivity(Activity activity) {
        activityList.remove(activity);
    }

    public void exitAllActivity() {
        for (Activity activity : activityList) {
            activity.finish();
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
//        MultiDex.install(this);
    }

    public void closeSerialPort() {

    }
}