package cn.softbankrobotics.navigation2.utils;

import android.util.Log;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.builder.GoToBuilder;
import com.aldebaran.qi.sdk.object.actuation.AttachedFrame;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.actuation.GoTo;
import com.aldebaran.qi.sdk.util.FutureUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import cn.softbankrobotics.navigation2.MyApplication;

public class GoToUtils {
    private static final String TAG = "GoToUtils";
    private static final int MAXTRIES = 30;
    private int tryCounter;
    private final List<onStartedMovingListener> startedListeners;
    private final List<onFinishedMovingListener> finishedListeners;
    private Future<Void> currentGoToAction;

    public GoToUtils() {
        startedListeners = new ArrayList<>();
        finishedListeners = new ArrayList<>();
    }

    public Future<Void> goTo(AttachedFrame attachedFrame) {
        return attachedFrame
                .async()
                .frame()
                .andThenCompose(frame -> goTo(frame));
    }

    private Future<Void> goTo(Frame frame) {
        tryCounter = MAXTRIES;
        raiseStartedMoving();

        return GoToBuilder.with(MyApplication.getInstance().getQiContext())
                .withFrame(frame)
                .buildAsync()
                .andThenConsume(goToAction -> tryGoTo(goToAction));
    }

    /**
     *  移动
     *      不要调用这个方法，请调用goto
     * @param goToAction    移动
     */
    private void tryGoTo(GoTo goToAction) {
        // This function runs the GoTo asynchronously, then checks the success.
        currentGoToAction = goToAction.async().run()
                .thenConsume(goToResult -> {

                    if (goToResult.isSuccess()) {
                        Log.d(TAG, "GoTo successful");
                        raiseFinishedMoving(GoToStatus.FINISHED);
                    } else if (goToResult.isCancelled()) {
                        Log.d(TAG, "GoTo cancelled");
                        raiseFinishedMoving(GoToStatus.CANCELLED);
                    } else if (goToResult.hasError() && tryCounter > 0) {
                        tryCounter--;
                        Log.d(TAG, "Move ended with error: ", goToResult.getError());
                        Log.d(TAG, "Retrying " + tryCounter + " times.");
                        FutureUtils.wait((long) 1500, TimeUnit.MILLISECONDS)
                                .thenConsume(aUselessVoid -> tryGoTo(goToAction));
                    } else {
                        raiseFinishedMoving(GoToStatus.FAILED);
                        Log.d(TAG, "Move finished, but the robot did not reach destination.");
                    }
                });
    }

    public void raiseFinishedMoving(GoToStatus success) {
        for (onFinishedMovingListener f : finishedListeners) {
            f.onFinishedMoving(success);
        }
    }

    private void raiseStartedMoving() {
        for (onStartedMovingListener f : startedListeners) {
            f.onStartedMoving();
        }
    }

    /**
     *  取消 移动
     * @return  取消移动 的结果
     */
    public Future<Void> checkAndCancelCurrentGoto() {
        tryCounter = 0;
        return pauseGoto();
    }

    public Future<Void> pauseGoto(){
        if (currentGoToAction == null) {
            return Future.of(null);
        }
        currentGoToAction.requestCancellation();
        Log.d(TAG, "pauseGoto");
        return currentGoToAction;
    }

    public void addOnStartedMovingListener(onStartedMovingListener f) {
        startedListeners.add(f);
    }

    public void addOnFinishedMovingListener(onFinishedMovingListener f) {
        finishedListeners.add(f);
    }

    public void removeOnStartedMovingListeners() {
        startedListeners.clear();
    }

    public void removeOnFinishedMovingListeners() {
        finishedListeners.clear();
    }

    /**
     *  机器人移动 启动监听接口
     */
    public interface onStartedMovingListener {
        void onStartedMoving();
    }

    /**
     *  机器人移动 完成监听接口
     */
    public interface onFinishedMovingListener {
        void onFinishedMoving(GoToStatus success);
    }

    /**
     *  移动状态
     */
    public enum GoToStatus {
        FAILED, // 移动出错
        CANCELLED,  // 移动取消
        FINISHED    //移动成功
    }
}
