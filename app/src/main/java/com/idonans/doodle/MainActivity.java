package com.idonans.doodle;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.idonans.acommon.app.CommonActivity;
import com.idonans.acommon.util.ViewUtil;

public class MainActivity extends CommonActivity {

    private DoodleView mDoodleView;
    private ViewGroup mDoodleActionPanel;
    private View mUndo;
    private View mRedo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDoodleView = ViewUtil.findViewByID(this, R.id.doodle_view);
        mDoodleView.setBrush(DoodleView.Brush.createPencil(Color.BLACK, 50));

        mDoodleActionPanel = ViewUtil.findViewByID(this, R.id.doodle_action_panel);
        mUndo = ViewUtil.findViewByID(mDoodleActionPanel, R.id.undo);
        mRedo = ViewUtil.findViewByID(mDoodleActionPanel, R.id.redo);

        mUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableDoodleAction();
                mDoodleView.undo(new DoodleView.ActionCallback() {
                    @Override
                    public void onActionResult(boolean success) {
                        syncUndoRedoStatus();
                    }
                });
            }
        });

        mRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disableDoodleAction();
                mDoodleView.redo(new DoodleView.ActionCallback() {
                    @Override
                    public void onActionResult(boolean success) {
                        syncUndoRedoStatus();
                    }
                });
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

}
