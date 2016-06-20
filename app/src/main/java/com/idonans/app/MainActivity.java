package com.idonans.app;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.idonans.acommon.app.CommonActivity;
import com.idonans.acommon.lang.CommonLog;
import com.idonans.acommon.util.ViewUtil;
import com.idonans.doodle.DoodleView;
import com.idonans.doodle.brush.Brush;
import com.idonans.doodle.brush.Pencil;

public class MainActivity extends CommonActivity implements BrushSettingFragment.BrushSettingListener {

    private static final String TAG = "MainActivity";
    private DoodleView mDoodleView;
    private ViewGroup mDoodleActionPanel;
    private View mUndo;
    private View mRedo;
    private View mSetBrush;

    private int mAspectType = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mDoodleView = ViewUtil.findViewByID(this, R.id.doodle_view);
        mDoodleView.setBrush(new Pencil(Color.BLACK, 50, 255));

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

}
