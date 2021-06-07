package cn.softbankrobotics.navigation2;
import android.app.Application;
import com.aldebaran.qi.sdk.QiContext;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.cache.CacheEntity;
import com.lzy.okgo.cache.CacheMode;
import com.lzy.okgo.cookie.CookieJarImpl;
import com.lzy.okgo.cookie.store.SPCookieStore;
import com.lzy.okgo.https.HttpsUtils;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.CsvFormatStrategy;
import com.orhanobut.logger.DiskLogAdapter;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;
import java.util.concurrent.TimeUnit;
import cn.softbankrobotics.navigation2.utils.LogUtils;
import okhttp3.OkHttpClient;

public class MyApplication extends Application {
    private static MyApplication myApplication = null;
    private QiContext mQiContext;
    // test test=new ZXingUtils();
    // bitmap=ZXingUtils.createQRImage(getIntent().getStringExtra("content"), 400,400);

    @Override
    public void onCreate() {
        super.onCreate();
        myApplication = this;
        FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(false)  // (Optional) Whether to show thread info or not. Default true
                .methodCount(0)         // (Optional) How many method line to show. Default 2
                .methodOffset(7)        // (Optional) Hides internal method calls up to offset. Default 5
                //.logStrategy(customLog) // (Optional) Changes the log strategy to print out. Default LogCat
                .tag("SBRC")   // (Optional) Global tgetSupportActionBarag for every log. Default PRETTY_LfOGGER
                .build();
        Logger.addLogAdapter(new AndroidLogAdapter(formatStrategy) {
            @Override
            public boolean isLoggable(int priority, String tag) {
                return BuildConfig.DEBUG;
            }
        });
        FormatStrategy diskStrategy = CsvFormatStrategy.newBuilder()
                .tag("SBRC")
                .build();
        Logger.addLogAdapter(new DiskLogAdapter(diskStrategy));
        LogUtils.d("MyApplication", "应用启动");

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

    public static MyApplication getInstance() {
        return myApplication;
    }

    public void setQiContext(QiContext qiContext) {
        this.mQiContext = qiContext;
    }

    public QiContext getQiContext() {
        return mQiContext;
    }
}