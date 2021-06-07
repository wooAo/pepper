package cn.softbankrobotics.navigation2.fragment;

import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class temp {
    // gotoTargets
    /*// 移动至目标帧
        GoTo goTo = GoToBuilder.with(MyApplication.getInstance().getQiContext())
                .withFrame(frame)
                .build();
        // 开始移动，异步调用！！
        Future<Void> goToResult = goTo.async().run();

        try {
            // 若移动成功
            if(goToResult.isSuccess()){
                // 设置当前位置 已被访问
                isVisited[count] = 1;
                // 头部转向
                Future<Void> lookAtFuture = startLookAt(frame);
                // 转向后 操作 （不考虑头部转向是否成功）
                FutureUtils.wait((long) 2.5, TimeUnit.SECONDS)
                        .thenConsume(lookAtResult -> {
                            if (lookAtResult.hasError()) {
                                LogUtils.d(TAG, "LookAt has error:" + lookAtResult.getErrorMessage());
                            }
                            // 终止 头部转向 线程
                            lookAtFuture.requestCancellation();
                            // 获取 目标点文本信息
                            String introduction = getText(targetPoint.getKey());
                            sayFuture = ToolUtils.sayAsync(introduction, () -> {
                                // 若下个目标存在
                                if(targetPoints.hasNext())
                                    goToTargets(targetPoints.next(), countVisited.getAndIncrement());
                                else{
                                    // 若不存在，重置迭代器，继续从头遍历
                                    targetPoints = mSavedFrames.entrySet().iterator();
                                    countVisited.set(0);
                                    goToTargets(targetPoints.next(), 0);
                                }
                            });
                            // 运行都此处时，说明当前位置已经成功抵达
                            isVisited[count] = 0;  //测试
                        });
            }else {
                // 若移动失败，则重新移动
                LogUtils.d(TAG, "Goto target point " + targetPoint.getKey() + " with error: " + goToResult.getErrorMessage());
                FutureUtils.wait((long) 0.5, TimeUnit.SECONDS)
                        .thenConsume(aUselessVoid -> goToTargets(targetPoint, count));
            }
        }catch (Exception e){
            LogUtils.e(TAG, "Goto task error: " + e.getMessage());
            // 移动出现异常，终止 移动
            goToResult.requestCancellation();

            // 重新移动 判定
            if(gotoRetryCounter.get() > 0){
                // 移动出错 且 重试次数大于0时
                LogUtils.d(TAG, "Goto target point " + targetPoint.getKey() + " in Exception, with error: " + goToResult.getErrorMessage());
                gotoRetryCounter.getAndDecrement();
                LogUtils.d(TAG, "Retrying goto target point " + targetPoint.getKey() + ",remain " + gotoRetryCounter + " times.");
                FutureUtils.wait((long) 0.5, TimeUnit.SECONDS)
                        .thenConsume(aUselessVoid -> goToTargets(targetPoint, count));
            }else {
                // 否则，导航失败
                ToolUtils.sayAsync("我无法到达指定位置，是不是有东西挡住我了呢");
                LogUtils.d(TAG, "Goto target finished, but the robot did not reach destination.");
            }
        }*/

    // gotoTarget
    // 处理移动结果
        /*go.thenConsume(future -> {
            if(future.isSuccess()){
                // 头部转向
                Future<Void> lookAtFuture = startLookAt(frame);
                // 转向后 操作 （不考虑头部转向是否成功）
                FutureUtils.wait((long) 2.5, TimeUnit.SECONDS)
                        .thenConsume(lookAtResult -> {
                            if (lookAtResult.hasError()) {
                                LogUtils.d(TAG, "LookAt has error:" + lookAtResult.getErrorMessage());
                            }
                            // 终止 头部转向 线程
                            lookAtFuture.requestCancellation();
                            // 获取 目标点文本信息
                            String introduction = getText(string);
                            sayFuture = ToolUtils.sayAsync(introduction, () -> {
                                mActivity.releaseAbilities();
                            });
                        });
            }else if(future.hasError()){
                LogUtils.e(TAG, "Goto task error: " + future.getErrorMessage());
                // 移动出现异常，终止 移动
                goToResult.requestCancellation();
                // 重新移动 判定
                if(gotoRetryCounter.get() > 0){
                    // 移动出错 且 重试次数大于0时
                    LogUtils.e(TAG, "Goto target point " + string + " in Exception, with error: " + goToResult.getErrorMessage());
                    gotoRetryCounter.getAndDecrement();
                    LogUtils.e(TAG, "Retrying goto target point " + string + ",remain " + gotoRetryCounter + " times.");
                    FutureUtils.wait((long) 0.5, TimeUnit.SECONDS)
                            .thenConsume(aUselessVoid -> goToTarget(string));
                }else {
                    // 否则，导航失败
                    ToolUtils.sayAsync("我无法到达指定位置，是不是有东西挡住我了呢");
                    LogUtils.d(TAG, "Goto target finished, but the robot did not reach destination.");
                }
            }
        });*/
    //goToResult = go;
    //return go;
}
