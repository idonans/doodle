package com.idonans.app;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import com.idonans.acommon.app.CommonActivity;
import com.idonans.acommon.data.StorageManager;
import com.idonans.acommon.lang.CommonLog;
import com.idonans.acommon.util.ViewUtil;
import com.idonans.doodle.DoodleData;
import com.idonans.doodle.DoodleDataCompile;
import com.idonans.doodle.DoodleView;
import com.idonans.doodle.brush.Brush;
import com.idonans.doodle.brush.Pencil;

import java.util.UUID;

public class MainActivity extends CommonActivity implements BrushSettingFragment.BrushSettingListener {

    private static final String TAG = "MainActivity";
    private static final String EXTRA_DOODLE_DATA_KEY = "doodle_data";
    private DoodleView mDoodleView;
    private ViewGroup mDoodleActionPanel;
    private View mUndo;
    private View mRedo;
    private View mSetBrush;

    private int mAspectType = 1;

    private String mDoodleDataKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        DoodleData doodleDataSaved = null;
        if (savedInstanceState != null) {
            mDoodleDataKey = savedInstanceState.getString(EXTRA_DOODLE_DATA_KEY);
            if (mDoodleDataKey != null) {
                String value = StorageManager.getInstance().getCache(mDoodleDataKey);
                doodleDataSaved = DoodleDataCompile.valueOf(value);
            }
        }

        mDoodleView = ViewUtil.findViewByID(this, R.id.doodle_view);
        mDoodleView.setBrush(new Pencil(Color.BLACK, 50, 255));

        // restore doodle data
        if (doodleDataSaved != null) {
            mDoodleView.load(doodleDataSaved);
        } else {
            mDoodleView.setCanvasBackgroundColor(Color.YELLOW);
        }

        mDoodleActionPanel = ViewUtil.findViewByID(this, R.id.doodle_action_panel);
        mUndo = ViewUtil.findViewByID(mDoodleActionPanel, R.id.undo);
        mRedo = ViewUtil.findViewByID(mDoodleActionPanel, R.id.redo);

        View viewSetAspect = ViewUtil.findViewByID(mDoodleActionPanel, R.id.set_aspect);
        viewSetAspect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAspectType == 1) {
                    mAspectType = 0;
                    mDoodleView.setAspectRatio(3, 4);
                } else {
                    mAspectType = 1;
                    mDoodleView.setAspectRatio(1, 1);
                }
            }
        });

        mUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDoodleView.undo();
            }
        });

        mRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDoodleView.redo();
            }
        });

        syncUndoRedoStatus();

        mDoodleView.setDoodleBufferChangedListener(new DoodleView.DoodleBufferChangedListener() {
            @Override
            public void onDoodleBufferChanged(boolean canUndo, boolean canRedo) {
                CommonLog.d(TAG + " onDoodleBufferChanged canUndo:" + canUndo + ", canRedo:" + canRedo);
                mUndo.setEnabled(canUndo);
                mRedo.setEnabled(canRedo);
            }
        });

        mSetBrush = ViewUtil.findViewByID(mDoodleActionPanel, R.id.set_brush);
        mSetBrush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBrushSetting();
            }
        });
    }

    private void disableDoodleAction() {
        mUndo.setEnabled(false);
        mRedo.setEnabled(false);
    }

    private void syncUndoRedoStatus() {
        disableDoodleAction();
        mDoodleView.canUndo(new DoodleView.ActionCallback() {
            @Override
            public void onActionResult(boolean success) {
                mUndo.setEnabled(success);
            }
        });
        mDoodleView.canRedo(new DoodleView.ActionCallback() {
            @Override
            public void onActionResult(boolean success) {
                mRedo.setEnabled(success);
            }
        });
    }

    private void showBrushSetting() {
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.content_panel, new BrushSettingFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void setBrushColor(int color) {
        Brush brush = mDoodleView.getBrush();
        mDoodleView.setBrush(brush.cloneWithColor(color));
    }

    @Override
    public void setBrushAlpha(int alpha) {
        Brush brush = mDoodleView.getBrush();
        mDoodleView.setBrush(brush.cloneWithAlpha(alpha));
    }

    @Override
    public void setBrushSize(int size) {
        Brush brush = mDoodleView.getBrush();
        mDoodleView.setBrush(brush.cloneWithSize(size));
    }

    @Override
    public void setBrushType(int type) {
        // TODO
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mDoodleDataKey == null) {
            mDoodleDataKey = UUID.randomUUID().toString();
        }
        outState.putString(EXTRA_DOODLE_DATA_KEY, mDoodleDataKey);

        mDoodleView.save(new DoodleView.SaveDataActionCallback() {
            @Override
            public void onDataSaved(@Nullable DoodleData doodleData) {
                String json = DoodleDataCompile.toJson(doodleData);
                StorageManager.getInstance().setCache(mDoodleDataKey, json);
            }
        });
    }

    private DoodleData mDoodleDataSaved;

    @Override
    public void saveDoodleData() {
        mDoodleView.save(new DoodleView.SaveDataActionCallback() {
            @Override
            public void onDataSaved(@Nullable DoodleData doodleData) {
                mDoodleDataSaved = doodleData;
            }
        });
    }

    @Override
    public void restoreDoodleData() {
        mDoodleView.load(mDoodleDataSaved);
    }

}
