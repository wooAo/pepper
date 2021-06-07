package cn.softbankrobotics.navigation2.fragment;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.aldebaran.qi.sdk.builder.ExplorationMapBuilder;
import com.aldebaran.qi.sdk.object.actuation.AttachedFrame;
import com.aldebaran.qi.sdk.object.actuation.Frame;
import com.aldebaran.qi.sdk.object.actuation.Mapping;
import com.aldebaran.qi.sdk.object.geometry.Transform;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.softbankrobotics.navigation2.Constant;
import cn.softbankrobotics.navigation2.MyApplication;
import cn.softbankrobotics.navigation2.R;
import cn.softbankrobotics.navigation2.utils.FolderUtils;
import cn.softbankrobotics.navigation2.utils.LogUtils;
import cn.softbankrobotics.navigation2.utils.ToolUtils;

public class LoadMapFragment extends BaseFragment {
    private final String TAG = "LoadMapFragment";

    private ProgressBar mProgressBar;
    public ImageView mapImageView;
    private Button mLoadSelectedMapButton;
    private ArrayAdapter<String> mapSpinnerAdapter;
    private String mSelectedMap;//列表中选中的地图
    private ArrayAdapter<String> mFrameSpinnerAdapter;
    private String mSelectedFrame;//列表中选中的标记点

    private LinkedHashMap<String, AttachedFrame> mSavedFrames;

    @Override
    public View initView() {
        View view = View.inflate(mActivity, R.layout.fragment_load_map, null);
        Button loadMapListButton = view.findViewById(R.id.load_map_list_button);
        loadMapListButton.setOnClickListener(v -> {
            loadMapList();
        });
        mProgressBar = view.findViewById(R.id.progressbar);
        mapImageView = view.findViewById(R.id.imageView_map);
        mLoadSelectedMapButton = view.findViewById(R.id.load_selected_map_button);
        //加载选中的地图
        mLoadSelectedMapButton.setOnClickListener(view1 -> {

            if (mSelectedMap == null || mSelectedMap.isEmpty()) {
                ToolUtils.showToast(mActivity, mActivity.getString(R.string.select_map));
                return;
            }
            mActivity.holdAbilities();
            mActivity.initNavigationService(() -> loadSelectedMap());
        });
        initMapSpinner(view);
        initFrameSpinner(view);
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
            //Fragment隐藏时调用
        } else {
            //Fragment显示时调用
            initData();
        }
    }

    //初始化数据
    private void initData() {
        mSavedFrames = mActivity.getSavedFrames();
    }

    //初始化地图下拉菜单
    private void initMapSpinner(View view) {
        Spinner mapSpinner = view.findViewById(R.id.map_spinner);
        mapSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSelectedMap = (String) parent.getItemAtPosition(position);
                LogUtils.d(TAG, "map onItemSelected: " + mSelectedMap);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mSelectedMap = null;
                LogUtils.d(TAG, "map onNothingSelected");
            }
        });
        mapSpinnerAdapter = new ArrayAdapter<>(mActivity, android.R.layout.simple_spinner_item, new ArrayList<>());
        mapSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mapSpinner.setAdapter(mapSpinnerAdapter);
    }

    //初始化标记点下拉菜单
    private void initFrameSpinner(View view) {
        Spinner localizeSpinner = view.findViewById(R.id.frame_spinner);
        // Store location on selection.
        localizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSelectedFrame = (String) parent.getItemAtPosition(position);
                LogUtils.d(TAG, "frame onItemSelected: " + mSelectedFrame);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mSelectedFrame = null;
                LogUtils.d(TAG, "frame onNothingSelected");
            }
        });
        // Setup spinner adapter.
        mFrameSpinnerAdapter = new ArrayAdapter<>(mActivity, android.R.layout.simple_spinner_item, new ArrayList<>());
        mFrameSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        localizeSpinner.setAdapter(mFrameSpinnerAdapter);
    }

    //从本地加载地图列表
    private void loadMapList() {
        File[] files = FolderUtils.getDataFolder(mActivity).listFiles();
        if (files == null || files.length == 0) {
            ToolUtils.showToast(mActivity, mActivity.getString(R.string.no_maps_locally));
            return;
        }
        mapSpinnerAdapter.clear();
        for (int i = 0; i < files.length; i++) {
            File childFile = files[i];
            mapSpinnerAdapter.add(childFile.getName());
        }
    }

    //加载选中的地图
    private void loadSelectedMap() {
        try {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mLoadSelectedMapButton.setEnabled(false);
                    mapImageView.setImageResource(R.drawable.ic_map_default);
                }
            });
            String mapData = FileUtils.readFileToString(new File(FolderUtils.getMapFolder(mActivity, mSelectedMap), Constant.MAP_FILE_NAME), Charset.defaultCharset());
            long startTime = System.currentTimeMillis();
            ExplorationMapBuilder.with(MyApplication.getInstance().getQiContext())
                    .withMapString(mapData)
                    .buildAsync().thenConsume(result -> {
                if (result.isSuccess()) {
                    mActivity.setExplorationMap(result.get());
                    mActivity.setCurrentMapName(mSelectedMap);
                    long durationTime = System.currentTimeMillis() - startTime;
                    LogUtils.d(TAG, "Load selected map duration time:" + durationTime);
                    mActivity.runOnUiThread(() -> {
                        mProgressBar.setVisibility(View.GONE);
                        mLoadSelectedMapButton.setEnabled(true);
                    });
                    ToolUtils.loadMapPic(mActivity, mActivity.getNavigationService(), mSelectedMap, mapImageView);
                    ToolUtils.sayAsync("加载地图成功,用时：" + durationTime / 1000 + "秒", () -> loadFrames());
                } else {
                    if (result.hasError()) {
                        LogUtils.e(TAG, getActivity().getString(R.string.load_maps_failed) + ":" + result.getErrorMessage());
                        ToolUtils.sayAsync(getActivity().getString(R.string.load_maps_failed));
                    }
                }
            });
        } catch (Exception e) {
            LogUtils.e(TAG, "Cannot load mapData: " + e.getMessage());
            ToolUtils.sayAsync(mActivity.getString(R.string.load_maps_failed));
        }
    }

    //加载标记点
    private void loadFrames() {
        Gson gson = new Gson();
        File file = new File(FolderUtils.getMapFolder(mActivity, mSelectedMap), Constant.FRAME_FILE_NAME);
        if (file.exists()) {
            try {
                mActivity.runOnUiThread(() -> mFrameSpinnerAdapter.clear());
                String json = FileUtils.readFileToString(file, Charset.defaultCharset());
                Type vectorMap = new TypeToken<LinkedHashMap<String, Transform>>() {
                }.getType();
                LinkedHashMap<String, Transform> vectors = gson.fromJson(json, vectorMap);
                mSavedFrames.clear();
                Mapping mapping = MyApplication.getInstance().getQiContext().getMapping();
                Frame mapFrame = mapping.async().mapFrame().getValue();

                for (Map.Entry<String, Transform> entry : vectors.entrySet()) {
                    Transform transform = entry.getValue();
                    AttachedFrame attachedFrame = mapFrame.async().makeAttachedFrame(transform).getValue();

                    mSavedFrames.put(entry.getKey(), attachedFrame);
                    mActivity.runOnUiThread(() -> mFrameSpinnerAdapter.add(entry.getKey()));
                }
                LogUtils.d(TAG, "Load frames successfully");
            } catch (IOException e) {
                LogUtils.e(TAG, "Load frames failed:" + e.toString());
                e.printStackTrace();
            }
        } else {
            LogUtils.d(TAG, mActivity.getString(R.string.no_frames_locally));
            ToolUtils.sayAsync(mActivity.getString(R.string.no_frames_locally), () -> mActivity.openCreateMapFragment());
        }
    }

}
