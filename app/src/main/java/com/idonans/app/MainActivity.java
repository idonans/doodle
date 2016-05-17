package com.idonans.app;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.idonans.acommon.app.CommonActivity;
import com.idonans.acommon.app.CommonFragment;
import com.idonans.acommon.lang.CommonLog;
import com.idonans.acommon.util.ViewUtil;
import com.idonans.doodle.DoodleView;

import java.util.ArrayList;

public class MainActivity extends CommonActivity {

    private static final String TAG = "MainActivity";
    private DoodleView mDoodleView;
    private ViewGroup mDoodleActionPanel;
    private View mUndo;
    private View mRedo;
    private View mSetBrush;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDoodleView = ViewUtil.findViewByID(this, R.id.doodle_view);
        mDoodleView.setBrush(DoodleView.Brush.createPencil(Color.BLACK, 50, 255));

        mDoodleActionPanel = ViewUtil.findViewByID(this, R.id.doodle_action_panel);
        mUndo = ViewUtil.findViewByID(mDoodleActionPanel, R.id.undo);
        mRedo = ViewUtil.findViewByID(mDoodleActionPanel, R.id.redo);

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
                .add(R.id.content_panel, new SetBrushFragment())
                .addToBackStack(null)
                .commit();
    }

    private void setBrushColor(int color) {
        DoodleView.Brush brush = mDoodleView.getBrush();
        mDoodleView.setBrush(DoodleView.Brush.createPencil(color, brush.size, brush.alpha));
    }

    private void setBrushAlpha(int alpha) {
        DoodleView.Brush brush = mDoodleView.getBrush();
        mDoodleView.setBrush(DoodleView.Brush.createPencil(brush.color, brush.size, alpha));
    }

    private void setBrushSize(int size) {
        DoodleView.Brush brush = mDoodleView.getBrush();
        mDoodleView.setBrush(DoodleView.Brush.createPencil(brush.color, size, brush.alpha));
    }

    public static class SetBrushFragment extends CommonFragment {

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.activity_main_set_brush_fragment, container, false);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            {
                ArrayList<TextView> views = new ArrayList<>();
                views.add((TextView) ViewUtil.findViewByID(view, R.id.color_red));
                views.add((TextView) ViewUtil.findViewByID(view, R.id.color_black));
                views.add((TextView) ViewUtil.findViewByID(view, R.id.color_white));
                views.add((TextView) ViewUtil.findViewByID(view, R.id.color_yellow));
                for (TextView v : views) {
                    v.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((MainActivity) getActivity()).setBrushColor(Color.parseColor(((TextView) v).getText().toString()));
                        }
                    });
                }
            }

            {
                ArrayList<TextView> views = new ArrayList<>();
                views.add((TextView) ViewUtil.findViewByID(view, R.id.size_10));
                views.add((TextView) ViewUtil.findViewByID(view, R.id.size_20));
                views.add((TextView) ViewUtil.findViewByID(view, R.id.size_40));
                views.add((TextView) ViewUtil.findViewByID(view, R.id.size_80));
                views.add((TextView) ViewUtil.findViewByID(view, R.id.size_160));
                for (TextView v : views) {
                    v.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((MainActivity) getActivity()).setBrushSize(Integer.valueOf(((TextView) v).getText().toString()));
                        }
                    });
                }
            }

            {
                ArrayList<TextView> views = new ArrayList<>();
                views.add((TextView) ViewUtil.findViewByID(view, R.id.alpha_10));
                views.add((TextView) ViewUtil.findViewByID(view, R.id.alpha_50));
                views.add((TextView) ViewUtil.findViewByID(view, R.id.alpha_100));
                views.add((TextView) ViewUtil.findViewByID(view, R.id.alpha_150));
                views.add((TextView) ViewUtil.findViewByID(view, R.id.alpha_200));
                views.add((TextView) ViewUtil.findViewByID(view, R.id.alpha_255));
                for (TextView v : views) {
                    v.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((MainActivity) getActivity()).setBrushAlpha(Integer.valueOf(((TextView) v).getText().toString()));
                        }
                    });
                }
            }
        }

    }

}
