package cn.softbankrobotics.navigation2.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.builder.LocalizeAndMapBuilder;
import com.aldebaran.qi.sdk.builder.LocalizeBuilder;
import com.aldebaran.qi.sdk.builder.TransformBuilder;
import com.aldebaran.qi.sdk.object.actuation.Actuation;
import com.aldebaran.qi.sdk.object.actuation.AttachedFrame;
import com.aldebaran.qi.sdk.object.actuation.ExplorationMap;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.actuation.Localize;
import com.aldebaran.qi.sdk.object.actuation.LocalizeAndMap;
import com.aldebaran.qi.sdk.object.actuation.Mapping;
import com.aldebaran.qi.sdk.object.geometry.Transform;
import com.aldebaran.qi.sdk.object.geometry.Vector3;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import cn.softbankrobotics.navigation2.Constant;
import cn.softbankrobotics.navigation2.MyApplication;
import cn.softbankrobotics.navigation2.R;
import cn.softbankrobotics.navigation2.callback.InitServiceCallBack;
import cn.softbankrobotics.navigation2.utils.FolderUtils;
import cn.softbankrobotics.navigation2.utils.LogUtils;
import cn.softbankrobotics.navigation2.utils.ToolUtils;

public class CreateMapFragment extends BaseFragment {
    private final String TAG = "CreateMapFragment";
    private View view;
    private Button startLocalizeAndMapButton;
    private Button cancelMapButton;
    private Button saveFrameButton;
    public ImageView mapImageView;
    private ProgressBar progressbar_create_map;
    private ImageView checkbox_localize_and_map;
    private Button mBackupFrames;
    private ImageView mBackupCheckbox;
    private ProgressBar mProgressBar;

    private Localize mLocalize;
    private ExplorationMap mExplorationMap;
    private Future<Void> mLocalizeFuture;
    private LocalizeAndMap mLocalizeAndMap;
    private Future<Void> mLocalizeAndMapFuture;
    private ArrayAdapter<String> mFrameSpinnerAdapter;
    private String mSelectedFrame;
    private String mCurrentMapName;
    private LinkedHashMap<String, AttachedFrame> mSavedFrames;

    @Override
    public View initView() {
        if (view == null) {
            view = View.inflate(mActivity, R.layout.fragment_create_map, null);
            progressbar_create_map = view.findViewById(R.id.progressbar_create_map);
            mapImageView = view.findViewById(R.id.imageView_map);
            startLocalizeAndMapButton = view.findViewById(R.id.start_localizeandmap_button);
            checkbox_localize_and_map = view.findViewById(R.id.checkbox_localize_and_map);
            saveFrameButton = view.findViewById(R.id.save_frame_button);
            saveFrameButton.setEnabled(false);
            EditText editText = view.findViewById(R.id.editText);
            cancelMapButton = view.findViewById(R.id.cancel_map_button);
            cancelMapButton.setEnabled(false);
            mBackupFrames = view.findViewById(R.id.backup_frames_button);
            mBackupFrames.setEnabled(false);
            mBackupCheckbox = view.findViewById(R.id.checkbox_backup);
            mProgressBar = view.findViewById(R.id.progressbar);
            initFrameSpinner(view);
            //??????????????????????????????
            startLocalizeAndMapButton.setOnClickListener(view1 -> {
                mapImageView.setImageResource(R.drawable.ic_map_default);
                checkbox_localize_and_map.setBackgroundResource(R.drawable.unchecked);
                mActivity.holdAbilities();
                mActivity.initNavigationService(new InitServiceCallBack() {
                    @Override
                    public void onInitServiceSuccess() {
                        mActivity.runOnUiThread(() -> startLocalizeAndMapButton.setEnabled(false));
                        ToolUtils.sayAsync(mActivity.getString(R.string.say_start_map), () -> startMapping(MyApplication.getInstance().getQiContext()));
                    }
                });
            });

            //????????????
            cancelMapButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Cancel the LocalizeAndMap action.
                    mLocalizeAndMapFuture.requestCancellation();
                    cancelMapButton.setEnabled(false);
                }
            });

            //???????????????
            saveFrameButton.setOnClickListener(v -> {
                String frame = editText.getText().toString();
                if (frame.isEmpty()) {
                    ToolUtils.showToast(mActivity, getString(R.string.enter_frame_name));
                    return;
                }
                if (mSavedFrames.containsKey(frame)) {
                    ToolUtils.showToast(mActivity, getString(R.string.frame_name_already_exists));
                    return;
                }
                editText.setText("");
                // Save location
                addFrame(frame);
            });

            mBackupFrames.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    backupFrames();
                }
            });
        }
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        initData();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            //Fragment???????????????
        } else {
            //Fragment???????????????
            initData();
            if (mActivity.getDoLocalize()) {
                mActivity.setDoLocalize(false);
                Executor executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    startLocalize();
                });
            }
        }
    }

    //???????????????
    private void initData() {
        mExplorationMap = mActivity.getExplorationMap();
        mCurrentMapName = mActivity.getCurrentMapName();
        mSavedFrames = mActivity.getSavedFrames();
    }

    //??????????????????????????????
    private void initFrameSpinner(View view) {
        Spinner localizeSpinner = view.findViewById(R.id.frame_spinner);
        // Store location on selection.
        localizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSelectedFrame = (String) parent.getItemAtPosition(position);
                LogUtils.d(TAG, "mLocalize onItemSelected: " + mSelectedFrame);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mSelectedFrame = null;
                LogUtils.d(TAG, "mLocalize onNothingSelected");
            }
        });
        // Setup spinner adapter.
        mFrameSpinnerAdapter = new ArrayAdapter<>(mActivity, android.R.layout.simple_spinner_item, new ArrayList<>());
        mFrameSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        localizeSpinner.setAdapter(mFrameSpinnerAdapter);
    }

    //????????????
    private void startMapping(QiContext qiContext) {
        // Create a LocalizeAndMap action.
        mActivity.runOnUiThread(() -> {
            progressbar_create_map.setVisibility(View.VISIBLE);
            saveFrameButton.setEnabled(false);
        });
        clearLocalize();
        mLocalizeAndMap = LocalizeAndMapBuilder.with(qiContext).build();
        // Add an on status changed listener on the LocalizeAndMap action for the robot to say when he is localized.
        mLocalizeAndMap.addOnStatusChangedListener(status -> {
            switch (status) {
                case SCANNING:
                    LogUtils.d(TAG, "LocalizeAndMap status:SCANNING");
                    break;
                case LOCALIZED:
                    LogUtils.d(TAG, "LocalizeAndMap status:LOCALIZED");
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressbar_create_map.setVisibility(View.GONE);
                            startLocalizeAndMapButton.setEnabled(true);
                            startLocalizeAndMapButton.setText(R.string.restart_map);
                            checkbox_localize_and_map.setBackgroundResource(R.drawable.checked);
                            cancelMapButton.setEnabled(true);
                        }
                    });
                    ToolUtils.sayAsync(mActivity.getString(R.string.continue_map));
                    break;
            }
        });
        // Execute the LocalizeAndMap action asynchronously.
        mLocalizeAndMapFuture = mLocalizeAndMap.async().run();

        // Add a lambda to the action execution.
        mLocalizeAndMapFuture.thenConsume(future -> {
            if (future.isSuccess()) {
                LogUtils.d(TAG, "LocalizeAndMapFuture future is Success");
            } else if (future.isCancelled()) {
                // Handle cancelled state.
                LogUtils.d(TAG, "LocalizeAndMapFuture future is Cancelled");
                // Dump the ExplorationMap.
                mExplorationMap = mLocalizeAndMap.dumpMap();
                mActivity.setExplorationMap(mExplorationMap);
                ToolUtils.sayAsync("?????????????????????????????????????????????");
                mActivity.runOnUiThread(() -> enterMapNameAndSave());
            } else {
                LogUtils.e(TAG, "????????????" + future.getError());
                ToolUtils.sayAsync("????????????");
            }
        });
    }

    //???????????????????????????
    private void enterMapNameAndSave() {
        EditText editText = new EditText(mActivity);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(getResources().getString(R.string.enter_map_name)).setIcon(android.R.drawable.ic_dialog_info).setView(editText)
                .setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
        builder.setPositiveButton(getResources().getString(R.string.confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String name = editText.getText().toString();
                if (name.isEmpty() || name.trim().length() == 0) {
                    ToolUtils.showToast(mActivity, "??????????????????????????????????????????");
                    return;
                }
                mProgressBar.setVisibility(View.VISIBLE);
                Executor executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    try {
                        String mapData = mExplorationMap.serialize();
                        File file = new File(FolderUtils.getMapFolder(mActivity, name), Constant.MAP_FILE_NAME);
                        FileUtils.writeStringToFile(file, mapData, Charset.defaultCharset());
                        mActivity.runOnUiThread(() -> {
                            cancelMapButton.setEnabled(true);
                            mProgressBar.setVisibility(View.GONE);
                        });
                        LogUtils.d(TAG, "?????????" + name + "????????????");
                        ToolUtils.sayAsync("?????????" + name + "???????????????????????????????????????");
                        mCurrentMapName = name;
                        mActivity.setCurrentMapName(mCurrentMapName);
                        ToolUtils.loadMapPic(mActivity, mActivity.getNavigationService(), name, mapImageView);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        });
        builder.show();
    }

    //????????????
    public void startLocalize() {
        mLocalize = LocalizeBuilder.with(MyApplication.getInstance().getQiContext())
                .withMap(mExplorationMap)
                .build();
        mLocalize.addOnStartedListener(() -> ToolUtils.sayAsync("????????????????????????"));
        mLocalize.addOnStatusChangedListener(status -> {
            switch (status) {
                case NOT_STARTED:
                    mActivity.runOnUiThread(() -> saveFrameButton.setEnabled(false));
                    break;
                case SCANNING:
                    mActivity.runOnUiThread(() -> saveFrameButton.setEnabled(false));
                    break;
                case LOCALIZED:
                    ToolUtils.sayAsync("?????????????????????????????????????????????");
                    mActivity.runOnUiThread(() -> saveFrameButton.setEnabled(true));
                    break;
                default:
                    break;
            }
        });
        // ?????????????????????
        //??????1???????????????????????????????????????????????????????????????????????????????????????
        //mLocalizeFuture = mLocalize.async().run();
        //??????2???????????????????????????????????????????????????????????????????????????????????????????????????????????????
        Transform transform = TransformBuilder.create().fromTranslation(new Vector3(0, 0, 0));
        mLocalizeFuture = mLocalize.async().runWithLocalizationHint(transform);

        mLocalizeFuture.thenConsume(localizeResult -> {
            if (localizeResult.hasError()) {
                LogUtils.e(TAG, "Localize failed: " + localizeResult.getErrorMessage());
                clearLocalize();
                ToolUtils.sayAsync("??????????????????????????????", () -> startLocalize());
            }
        });
    }

    //???????????????????????????
    private void addFrame(final String frameName) {
        Actuation actuation = MyApplication.getInstance().getQiContext().getActuation();
        actuation.async()
                .robotFrame()
                .andThenApply(robotFrame -> {
                    Mapping mapping = MyApplication.getInstance().getQiContext().getMapping();
                    Frame mapFrame = mapping.async().mapFrame().getValue();
                    Transform transform = ToolUtils.getTransform(mapFrame, robotFrame);
                    return mapFrame.makeAttachedFrame(transform);
                }).andThenConsume(attachedFrame -> {
                    mSavedFrames.put(frameName, attachedFrame);
                    ToolUtils.sayAsync("??????????????????" + frameName + "??????");
                    Frame mapFrame = MyApplication.getInstance().getQiContext().getMapping().mapFrame();
                    Transform transform = ToolUtils.getTransform(mapFrame, attachedFrame.frame());
                    LogUtils.d(TAG, "??????????????????" + frameName + "??????;" + transform.toString());
                    mActivity.runOnUiThread(() -> {
                        mFrameSpinnerAdapter.add(frameName);
                        mBackupFrames.setEnabled(true);
                    });
                }
        );
    }

    //????????????????????????????????????
    private void backupFrames() {
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            LinkedHashMap<String, Transform> framesToBackup = new LinkedHashMap<>();
            Frame mapFrame = MyApplication.getInstance().getQiContext().getMapping().mapFrame();

            for (Map.Entry<String, AttachedFrame> entry : mSavedFrames.entrySet()) {
                AttachedFrame destination = entry.getValue();
                Frame frame = destination.async().frame().getValue();
                Transform transform = ToolUtils.getTransform(mapFrame, frame);
                framesToBackup.put(entry.getKey(), transform);
            }
            saveFramesToFile(mActivity, framesToBackup);
        });
    }

    //??????????????????????????????
    private void saveFramesToFile(Context context, Map<String, Transform> locationsToBackup) {
        Gson gson = new Gson();
        String framesJson = gson.toJson(locationsToBackup);
        File file = new File(FolderUtils.getMapFolder(context, mCurrentMapName), Constant.FRAME_FILE_NAME);
        try {
            FileUtils.writeStringToFile(file, framesJson, Charset.defaultCharset());
            LogUtils.d(TAG, "???????????????????????????");
            ToolUtils.sayAsync("???????????????????????????");
            mActivity.runOnUiThread(() -> {
                mBackupCheckbox.setBackgroundResource(R.drawable.checked);
            });
            clearLocalize();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //??????????????????????????????
    private void clearLocalize() {
        if (mLocalizeAndMap != null) {
            mLocalizeAndMap.removeAllOnStatusChangedListeners();
            mLocalizeAndMap = null;
            LogUtils.d(TAG, "??????localizeAndMap");
        }
        if (mLocalizeAndMapFuture != null && !mLocalizeAndMapFuture.isDone()) {
            mLocalizeAndMapFuture.cancel(true);
            mLocalizeAndMapFuture = null;
            LogUtils.d(TAG, "??????localizeAndMapFuture");
        }
        if (mLocalizeFuture != null && !mLocalizeFuture.isDone()) {
            mLocalizeFuture.cancel(true);
            mLocalizeFuture = null;
            LogUtils.d(TAG, "??????LocalizeFuture");
        }
        if (mLocalize != null) {
            mLocalize.removeAllOnStartedListeners();
            mLocalize.removeAllOnStatusChangedListeners();
            mLocalize = null;
            LogUtils.d(TAG, "??????Localize");
        }
    }
}
