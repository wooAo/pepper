package cn.softbankrobotics.navigation2.fragment;

import android.app.Dialog;
import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.os.Build;
import android.renderscript.ScriptGroup;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;


import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.builder.GoToBuilder;
import com.aldebaran.qi.sdk.builder.LocalizeBuilder;
import com.aldebaran.qi.sdk.builder.LookAtBuilder;
import com.aldebaran.qi.sdk.builder.TransformBuilder;
import com.aldebaran.qi.sdk.object.actuation.Actuation;
import com.aldebaran.qi.sdk.object.actuation.AttachedFrame;
import com.aldebaran.qi.sdk.object.actuation.ExplorationMap;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.actuation.FreeFrame;
import com.aldebaran.qi.sdk.object.actuation.GoTo;
import com.aldebaran.qi.sdk.object.actuation.Localize;
import com.aldebaran.qi.sdk.object.actuation.LookAt;
import com.aldebaran.qi.sdk.object.geometry.Quaternion;
import com.aldebaran.qi.sdk.object.geometry.Transform;
import com.aldebaran.qi.sdk.object.geometry.Vector3;
import com.aldebaran.qi.sdk.util.FutureUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import cn.softbankrobotics.navigation2.Constant;
import cn.softbankrobotics.navigation2.MyAdapter;
import cn.softbankrobotics.navigation2.MyApplication;
import cn.softbankrobotics.navigation2.R;
import cn.softbankrobotics.navigation2.utils.GoToUtils;
import cn.softbankrobotics.navigation2.utils.LogUtils;
import cn.softbankrobotics.navigation2.utils.ToolUtils;

public class NavigationFragment extends BaseFragment implements View.OnClickListener{

    private final String TAG = "NavigationFragment";
    private View view; // 导航页面视图
    private ExplorationMap mExplorationMap;   /* 地图 */
    private LinkedHashMap<String, AttachedFrame> mSavedFrames;   /* 导航点 */
    private Localize mLocalize; // 定位对象
    private Future<Void> mLocalizeFuture;   //定位线程结果对象
    private List<String> targets; // 导航点列表

    private static final int MAX_LOCALIZE_CANCEL_RETRIES = 5;//重置定位的重试次数
    private static AtomicInteger localizeCancelRetryCounter = new AtomicInteger(MAX_LOCALIZE_CANCEL_RETRIES);

    // 控件
    private Button loadMapListButton;
    private Button pauseButton;
    private Button continueButton;
    private Button stopButton;
    private Button all;

    // 线程，用于在ui线程中执行机器人操作
    private Executor executor = Executors.newSingleThreadExecutor();

    // 说话线程结果对象
    private Future<Void> sayFuture;

    private boolean isNavigated = false; // 导航标识符
    private AtomicInteger goToTargetsCounter = new AtomicInteger(0); // 遍历地图计数器
    private boolean goToTargetsSuccess = false; // 遍历地图标识符
    private boolean goToTargetsLoop = true; // 是否循环遍历地图

    // 线程同步工具
    private CountDownLatch latch;

    // 当前移动目标点，动态更新
    private String currentTarget;

    // 提示信息
    Dialog dia;
    ImageView imageView;

    /**
     *  Fragment生命周期函数
     *      加载导航页面视图，控件，监听器
     * @return 导航视图
     */
    @Override
    public View initView() {
        Context context = mActivity;
        // 注册提示信息
        dia = new Dialog(context, R.style.edit_AlertDialog_style);
        dia.setContentView(R.layout.activity_start_dialog);
        imageView = dia.findViewById(R.id.start_img);
        imageView.setBackgroundResource(R.drawable.walking);
        view = View.inflate(mActivity, R.layout.fragment_navigation, null);
        loadMapListButton = view.findViewById(R.id.navigation_button);
        loadMapListButton.setOnClickListener(v -> {
            if(!isNavigated){
                LogUtils.d(TAG, "开始导航");
                clickDialog();
                executor.execute(() -> doNavigation());
            }
        });

        // 注册控件
        pauseButton = view.findViewById(R.id.pause_button);
        continueButton = view.findViewById(R.id.continue_button);
        stopButton = view.findViewById(R.id.stop_button);
        all = view.findViewById(R.id.all);
        // 注册点击事件
        pauseButton.setOnClickListener(this);
        continueButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        all.setOnClickListener(this);
        return view;
    }

    /**
     *  Fragment生命周期函数
     *      始初化导航数据，此方法会在每次进入导航页面时运行
     *          导航点 在此方法中加载
     */
    @Override
    public void onStart() {
        super.onStart();
        initData();
    }

    /**
     *  导航功能控件的点击监听函数
     *      导航功能的开启，暂停，继续，终止
     * @param v 按钮控件
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.all:
                LogUtils.d(TAG,"遍历 " + v.getId());
                if(isNavigated) goToTargets();
                else ToolUtils.sayAndAsync("定位之后才能开始导航，请先定位");
                break;
            case R.id.pause_button:
                // 暂停
                LogUtils.d(TAG,"暂停 " + v.getId());
                mActivity.goToUtils.pauseGoto();
                break;
            case R.id.continue_button:
                // 继续
                LogUtils.d(TAG,"继续 " + v.getId());
                goToTarget(currentTarget);
                break;
            case R.id.stop_button:
                // 结束导航
                LogUtils.d(TAG,"结束导航 " + v.getId());
                mActivity.runOnUiThread(() -> clickDialog());
                ToolUtils.sayAndAsync("那我今天的介绍就到这里了，感谢您的耐心倾听，感谢您的参观。");
                if(mActivity.goToUtils != null) mActivity.goToUtils.checkAndCancelCurrentGoto();
                if(sayFuture != null) sayFuture.requestCancellation();
                if(mLocalizeFuture != null) mLocalizeFuture.requestCancellation();
                isNavigated = false;
                mActivity.releaseAbilities();
                break;
            default:
                break;
        }
    }

    /** Fragment生命周期函数
     *      当导航页面隐藏时调用
     *          ==》 当从导航页面回到加载地图页面时会调用
     * @param hidden 当前页面的隐藏状态
     */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            //Fragment显示时调用
            initData();
        }
    }

    /**
     * 初始化地图数据，导航点按钮及其监听
     *      利用recycleView实现动态添加导航点，根据所加载的地图确定导航点
     */
    private void initData(){
        // 获取 地图
        mExplorationMap = mActivity.getExplorationMap();
        // 获取 导航点集合
        mSavedFrames = mActivity.getSavedFrames();

        // 初始化 导航点
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        targets = new ArrayList<>();//mSavedFrames.keySet()
        // 辅助导航点 不显示
        for(String s : mSavedFrames.keySet()){
            if(!s.startsWith("辅助点")) targets.add(s);
        }

        LogUtils.d(TAG,"初始化导航点，导航点个数为：" + targets.size());
        // 设置 RecycleView 的布局
        GridLayoutManager gridLayoutManager = new GridLayoutManager(mActivity, 3, GridLayoutManager.VERTICAL, false);
        gridLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(gridLayoutManager);
        MyAdapter myAdapter = new MyAdapter(targets, mActivity);
        // 导航点按钮点击监听
        myAdapter.setOnItemClickListener(new MyAdapter.OnItemClickListener() {
            @Override
            public void onButtonClicked(View view, int position, String string) {
                if(isNavigated){
                    LogUtils.d(TAG,"动态添加的控件 ==》 " + position + ":" + string);
                    goToTarget(string);
                }else {
                    ToolUtils.sayAndAsync("定位之后才能开始导航，请先定位");
                }
            }
        });
        recyclerView.setAdapter(myAdapter);
    }

    /**
     *  开始定位
     */
    private void doNavigation(){
        if (mExplorationMap == null) {
            ToolUtils.sayAsync("我没有地图，无法开始展厅工厂");
            return;
        }
        if (mSavedFrames.size() == 0) {
            ToolUtils.sayAsync("我没有标记点，无法开始展厅工厂");
            return;
        }
        // 若已经导航过，再次导航时，需要置空
        if (mLocalize != null && mLocalizeFuture != null) {
            LogUtils.d(TAG, "Cancel localize");
            if (!mLocalizeFuture.isDone()) {
                boolean isCanceled = mLocalizeFuture.cancel(true);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (isCanceled) {
                    localizeCancelRetryCounter = new AtomicInteger(MAX_LOCALIZE_CANCEL_RETRIES);
                    mLocalize = null;
                } else {
                    if (localizeCancelRetryCounter.getAndDecrement() > 0) {
                        LogUtils.d(TAG, "Cancel localize failed retry");
                        doNavigation();
                    } else {
                        LogUtils.d(TAG, "Cancel localize failed, please restart");
                    }
                }
            }else {
                mLocalize = null;
            }
        }
        // 创建 定位对象
        mLocalize = LocalizeBuilder.with(MyApplication.getInstance().getQiContext())
                .withMap(mExplorationMap)
                .build();

        mLocalize.addOnStartedListener(() -> {
            ToolUtils.sayAsync("请让我准备下，我将要开始定位了");
            LogUtils.d(TAG, "我将要开始定位了");
        });
        //reLocalize
        mLocalize.removeAllOnStatusChangedListeners();
        mLocalize.addOnStatusChangedListener(status -> {
            switch (status) {
                case NOT_STARTED:
                    LogUtils.d(TAG, "未开始定位");
                    break;
                case SCANNING:
                    LogUtils.d(TAG, "正在定位");
                    break;
                case LOCALIZED:
                    LogUtils.d(TAG, "定位成功");
                    ToolUtils.sayAsync("我准备好了，我将为您介绍工厂", () -> {
                        isNavigated = true;
                        // 关闭 机器人 自主能力
                        mActivity.holdAbilities();
                    });
                    break;
                default:
                    break;
            }
        });
        // 定位有两种方式
        //方式1：定位的位置和方向没有限制，但是会受现场光线和周围人员影响
        //mLocalizeFuture = mLocalize.async().run();
        //方式2：需要按照建图起始点的位置和朝向放置，不会受现场光线和周围人员影响（推荐）
        //建图时朝向貌似也有影响，所以也要注意，建图时候最好比较暗
        Transform transform = TransformBuilder.create().fromTranslation(new Vector3(0, 0, 0));
        mLocalizeFuture = mLocalize.async().runWithLocalizationHint(transform);

        // 定位结果处理
        mLocalizeFuture.thenConsume(result -> {
            if (result.hasError()) {
                mLocalize = null;
                LogUtils.e(TAG, "Localize failed: " + result.getErrorMessage());
                ToolUtils.sayAsync("定位失败，我将会重试", this::doNavigation);
            } else if (result.isCancelled()) {
                LogUtils.d(TAG, "Localize canceled");
                mLocalize = null;
            } else {
                //一般不会走到这里，Localize除非主动cancel或者发生异常，不然不会停止
                LogUtils.e(TAG, "Localize 发生异常，停止定位");
            }
        });
    }

    /**
     *  移动到目标点
     * @param location    目标点名称
     */
    private void goToTarget(String location){

        // 开启机器人移动 启动监听
        mActivity.goToUtils.addOnStartedMovingListener(() -> mActivity.runOnUiThread(() -> {
            // 开启移动提示框
            dia.show();
            dia.setCanceledOnTouchOutside(true);
            Window window = dia.getWindow();
            WindowManager.LayoutParams lp = Objects.requireNonNull(window).getAttributes();
            lp.x = 0;
            lp.y = 40;
            dia.onWindowAttributesChanged(lp);
            // 点击关闭对话
            imageView.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dia.dismiss();
                        }
                    });
            mActivity.goToUtils.removeOnStartedMovingListeners();
            LogUtils.d(TAG, "goToTarget: removeOnStartedMovingListeners");
        }));
        LogUtils.d(TAG, "goToTarget: addOnStartedMovingListener");

        // 开启机器人移动 完成监听
        mActivity.goToUtils.addOnFinishedMovingListener((goToStatus) -> {
            mActivity.goToUtils.removeOnFinishedMovingListeners();
            LogUtils.d(TAG, "goToTarget: removeOnFinishedMovingListeners");
            switch (goToStatus){
                case FINISHED:
                    // 转身
                    //LogUtils.d(TAG, "goToTarget: 开始转身");

                    /*Actuation actuation = MyApplication.getInstance().getQiContext().getActuation();
                    Frame robotFrame = actuation.robotFrame();
                    FreeFrame targetFrame = MyApplication.getInstance().getQiContext().getMapping().makeFreeFrame();
                    Transform turnAround = TransformBuilder.create().from2DTransform(0d, 0d, Math.PI);
                    targetFrame.update(robotFrame, turnAround, 0L);
                    GoTo goTo = GoToBuilder.with(MyApplication.getInstance().getQiContext())
                            .withFrame(targetFrame.frame())
                            .build();

                    goTo.async().run();*/

                    LogUtils.d(TAG, "成功达到 : " + location);
                    // 获取当前导航点 信息
                    String introduction = getIntroduction(location);
                    ToolUtils.sayAndAsync(introduction);

                    break;
                case CANCELLED:
                    LogUtils.d(TAG, "goToTarget: 导航取消");
                    break;
                case FAILED:
                    LogUtils.d(TAG, "goToTarget: 导航失败");
                    break;
            }
            // 关闭移动提示框
            mActivity.runOnUiThread(() -> {
                if(dia.isShowing()) dia.dismiss();
            });
        });

        // 移动
        LogUtils.d(TAG, "goToTarget: addOnFinishedMovingListeners");
        goToLocation(location);
    }

    /**
     *  遍历地图
     */
    private void goToTargets(){
        if(mExplorationMap == null){
            ToolUtils.sayAndAsync("我没有地图，请添加地图");
            return;
        }
        if(mSavedFrames.size() == 0){
            ToolUtils.sayAndAsync("我没有导航点，请添加至少一个导航点");
            return;
        }
        if(!isNavigated){
            ToolUtils.sayAndAsync("我还没有定位，请先让我定位");
            return;
        }
        goToTargetsCounter.set(0);
        goToTargetsSuccess = false;

        // 开启机器人移动 启动监听
        mActivity.goToUtils.addOnStartedMovingListener(() -> mActivity.runOnUiThread(() -> {
            dia.show();
            dia.setCanceledOnTouchOutside(true); // Sets whether this dialog is
            Window window = dia.getWindow();
            WindowManager.LayoutParams lp = Objects.requireNonNull(window).getAttributes();
            lp.x = 0;
            lp.y = 40;
            dia.onWindowAttributesChanged(lp);
            mActivity.goToUtils.removeOnStartedMovingListeners();
            LogUtils.d(TAG, "goToTargets: removeOnStartedMovingListeners");
        }));
        LogUtils.d(TAG, "goToTargets: addOnStartedMovingListener");

        // 开启机器人移动 完成监听
        mActivity.goToUtils.addOnFinishedMovingListener((goToStatus) -> { // lambda
            LogUtils.d(TAG, "goToTargets : " + goToStatus);
            if(goToStatus == GoToUtils.GoToStatus.FINISHED
                    && goToTargetsCounter.get() < targets.size() - 1){
                // 进入此条件，则说明当前点已成功达到，开始前往下个导航点
                LogUtils.d(TAG, "成功达到 : " + targets.get(goToTargetsCounter.get()));
                String introduction = getIntroduction(targets.get(goToTargetsCounter.get()));
                ToolUtils.sayAndAsync(introduction)
                        .andThenConsume(ignore -> {
                            goToTargetsCounter.getAndIncrement();
                            LogUtils.d(TAG, "开始前往下一个导航点: " + targets.get(goToTargetsCounter.get()));
                            goToLocation(targets.get(goToTargetsCounter.get()));
                        });
            }
            else {
                // 一旦进入此条件，则说明前往目标点失败 或 targets已经全部达到
                mActivity.goToUtils.removeOnFinishedMovingListeners();
                LogUtils.d(TAG, "goToTargets: removeOnFinishedMovingListeners");
                latch = new CountDownLatch(1);
                switch (goToStatus){
                    case FINISHED:  // 最后一个目标点成功达到
                        LogUtils.d(TAG, "成功达到 ：" + targets.get(goToTargetsCounter.get()) + " ，最后一个导航点");
                        String introduction = getIntroduction(targets.get(goToTargetsCounter.get()));
                        ToolUtils.sayAndAsync(introduction, () -> latch.countDown());
                        goToTargetsSuccess = true;
                        break;
                    case CANCELLED: // 导航被取消
                        goToTargetsSuccess = false;
                        latch.countDown();
                        break;
                    case FAILED:    // 导航出错
                        LogUtils.d(TAG, "goToTargets: failed to complete");
                        goToTargetsSuccess = false;
                        latch.countDown();
                        break;
                }
                try {
                    latch.await();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                FutureUtils.wait(2, TimeUnit.SECONDS).thenConsume(aUselessFuture -> {
                    // 关闭移动提示框
                    mActivity.runOnUiThread(() -> {
                        if(dia.isShowing()) dia.dismiss();
                    });
                    LogUtils.d(TAG, "goToTargetsSuccess: " + goToTargetsSuccess + ", goToTargetsLoop: " + goToTargetsLoop);
                    // 若成功达到所以点，且 开启循环 功能，则递归调用自己
                    if(goToTargetsSuccess && goToTargetsLoop) goToTargets();
                });
            }
        });

        // 移动
        LogUtils.d(TAG, "goToTargets: addOnFinishedMovingListeners");
        goToLocation(targets.get(goToTargetsCounter.get()));
    }

    /**
     *  移动到目标点
     *      不要调用此方法，请调用 goToTarget/goToTargets
     * @param location    目标点名称
     */
    public void goToLocation(String location) {
        // 记录要去的目标点
        currentTarget = location;
        LogUtils.d(TAG,"开始前往 : " + location);
        ToolUtils.sayAndAsync("开始前往" + location);
        mActivity.goToUtils.checkAndCancelCurrentGoto().thenConsume(aVoid -> {
            mActivity.holdAbilities();
            mActivity.goToUtils.goTo(mSavedFrames.get(location));
        });
    }

    /**
     *  获取目标点 文本
     *      向服务器请求，获取对应文本
     *      异步
     * @param string    目标点
     * @return  查询本文
     */
    private String getIntroduction(String string){
        StringBuilder text = new StringBuilder();
        text.append("这里是").append(string).append("，");
        // 标记点 文本，先写死
        if("实验室出口".equals(string)) text.append(Constant.shiyanshirukou);
        if("仪表台".equals(string)) text.append(Constant.yibiaotai);
        if("通信测试台".equals(string)) text.append(Constant.tongxunceshitai);
        if("中央测试台".equals(string)) text.append(Constant.zhongyangceshitai);
        if("边缘网关测试台".equals(string)) text.append(Constant.bianyuanwangguanceshitai);
        if("外壳打标机".equals(string)) text.append(Constant.waikedabiaoji);
        if("波峰焊".equals(string)) text.append(Constant.bofenghan);
        if("波峰焊工作区".equals(string)) text.append(Constant.bofenghangongzuoqu);
        if("机械手臂试验区".equals(string)) text.append(Constant.jixieshoubi);
        if("自动光学检测".equals(string)) text.append(Constant.zidongguangxuejiance);
        if("贴片机".equals(string)) text.append(Constant.tiepianji);
        if("印刷机".equals(string)) text.append(Constant.yinshuaji);
        if("PCB激光打标机".equals(string)) text.append(Constant.PCBjiguangdabiaoji);
        return text.toString();
    }

    /**
     *  按钮时提示信息
     */
    public void clickDialog(){
        dia.show();
        dia.setCanceledOnTouchOutside(true); // Sets whether this dialog is
        Window window = dia.getWindow();
        WindowManager.LayoutParams lp = Objects.requireNonNull(window).getAttributes();
        lp.x = 0;
        lp.y = 40;
        dia.onWindowAttributesChanged(lp);

        // 点击关闭对话
        imageView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dia.dismiss();
                    }
                });

        // 8秒后关闭
        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            public void run() {
                dia.dismiss();
                t.cancel();
            }
        }, 10000);
    }
}
