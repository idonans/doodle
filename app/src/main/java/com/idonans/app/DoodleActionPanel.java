package com.idonans.app;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.idonans.acommon.lang.CommonLog;
import com.idonans.acommon.util.ViewUtil;
import com.idonans.doodle.DoodleData;
import com.idonans.doodle.DoodleView;
import com.idonans.doodle.brush.Brush;
import com.idonans.doodle.brush.Empty;
import com.idonans.doodle.brush.LeavesPencil;
import com.idonans.doodle.brush.Pencil;

/**
 * Created by pengji on 16-6-24.
 */
public class DoodleActionPanel {

    private static final String TAG = "DoodleActionPanel";

    private final View mSizeDown;
    private final View mSizeUp;
    private final TextView mSizeView;
    private final SeekBar mSizeSeekBar;
    private final View mAlphaDown;
    private final View mAlphaUp;
    private final TextView mAlphaView;
    private final SeekBar mAlphaSeekBar;
    private final View mUndo;
    private final View mRedo;
    private final TextView mSelectColor;
    private final View mMore;

    private final MorePanel mMorePanel;

    private static final float MIN_SIZE = 0f;
    private static final float MAX_SIZE = 200f;
    private static final int MIN_ALPHA = 0;
    private static final int MAX_ALPHA = 255;
    private static final String EXTRA_SIZE = "doodle_action_panel_size";
    private static final String EXTRA_ALPHA = "doodle_action_panel_alpha";
    private static final String EXTRA_COLOR = "doodle_action_panel_color";
    private static final String EXTRA_BRUSH_TYPE = "doodle_action_panel_brush_type";
    private float mSize;
    private int mAlpha;
    private int mColor;
    private int mBrushType;

    private static final ActionListener EMPTY_ACTION_LISTENER = new SimpleActionListener();
    private ActionListener mActionListener;

    public interface ActionListener {
        void saveAsBitmap();

        void onBrushChanged();
    }

    DoodleActionPanel(View rootView, Bundle savedInstanceState) {
        View doodlePanelView = ViewUtil.findViewByID(rootView, R.id.doodle_action_panel);

        View sizePanel = ViewUtil.findViewByID(doodlePanelView, R.id.size_panel);
        mSizeDown = ViewUtil.findViewByID(sizePanel, R.id.size_down);
        mSizeUp = ViewUtil.findViewByID(sizePanel, R.id.size_up);
        mSizeView = ViewUtil.findViewByID(sizePanel, R.id.size_view);
        mSizeSeekBar = ViewUtil.findViewByID(sizePanel, R.id.size_seekbar);
        mSizeSeekBar.setMax((int) MAX_SIZE);

        View alphaPanel = ViewUtil.findViewByID(doodlePanelView, R.id.alpha_panel);
        mAlphaDown = ViewUtil.findViewByID(alphaPanel, R.id.alpha_down);
        mAlphaUp = ViewUtil.findViewByID(alphaPanel, R.id.alpha_up);
        mAlphaView = ViewUtil.findViewByID(alphaPanel, R.id.alpha_view);
        mAlphaSeekBar = ViewUtil.findViewByID(alphaPanel, R.id.alpha_seekbar);
        mAlphaSeekBar.setMax(MAX_ALPHA);

        View actionPanel = ViewUtil.findViewByID(doodlePanelView, R.id.action_panel);
        mUndo = ViewUtil.findViewByID(actionPanel, R.id.undo);
        mRedo = ViewUtil.findViewByID(actionPanel, R.id.redo);
        mSelectColor = ViewUtil.findViewByID(actionPanel, R.id.select_color);
        mMore = ViewUtil.findViewByID(actionPanel, R.id.more);

        mMorePanel = new MorePanel(rootView);

        restoreSavedState(savedInstanceState);

        mSizeDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adjustSizeBy(-1);
            }
        });
        mSizeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adjustSizeBy(1);
            }
        });
        mSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    adjustSizeTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // ignore
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // ignore
            }
        });
        mAlphaDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adjustAlphaBy(-1);
            }
        });
        mAlphaUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adjustAlphaBy(1);
            }
        });
        mAlphaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    adjustAlphaTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMorePanel.show();
            }
        });
    }

    public void attach(final DoodleView doodleView) {
        syncUndoRedoStatus(doodleView);
        doodleView.setDoodleBufferChangedListener(new DoodleView.DoodleBufferChangedListener() {
            @Override
            public void onDoodleBufferChanged(boolean canUndo, boolean canRedo) {
                CommonLog.d(TAG + " onDoodleBufferChanged canUndo:" + canUndo + ", canRedo:" + canRedo);
                mUndo.setEnabled(canUndo);
                mRedo.setEnabled(canRedo);
            }
        });
        mUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doodleView.undo();
            }
        });

        mRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doodleView.redo();
            }
        });
    }

    public void setActionListener(ActionListener actionListener) {
        mActionListener = actionListener;
    }

    public float getSize() {
        return mSize;
    }

    public int getAlpha() {
        return mAlpha;
    }

    public int getColor() {
        return mColor;
    }

    public int getBrushType() {
        return mBrushType;
    }

    @NonNull
    public ActionListener getActionListener() {
        ActionListener listener = mActionListener;
        if (listener == null) {
            listener = EMPTY_ACTION_LISTENER;
        }
        return listener;
    }

    private void restoreSavedState(Bundle savedInstanceState) {
        float size = 10;
        int alpha = 255;
        int color = Color.BLACK;
        int brushType = DoodleData.BRUSH_TYPE_EMPTY;

        if (savedInstanceState != null) {
            size = savedInstanceState.getFloat(EXTRA_SIZE, size);
            alpha = savedInstanceState.getInt(EXTRA_ALPHA, alpha);
            color = savedInstanceState.getInt(EXTRA_COLOR, color);
            brushType = savedInstanceState.getInt(EXTRA_COLOR, brushType);
        }
        mSize = size;
        mAlpha = alpha;
        mColor = color;
        mBrushType = brushType;
        notifyBrushChanged();
    }

    private void notifyBrushChanged() {
        syncBrushView();
        getActionListener().onBrushChanged();
    }

    // show current brush state with view
    private void syncBrushView() {
        mSizeView.setText(String.valueOf(mSize));
        mSizeSeekBar.setProgress((int) mSize);
        mAlphaView.setText(String.valueOf(mAlpha));
        mAlphaSeekBar.setProgress(mAlpha);
        mSelectColor.setTextColor(removeAlpha(mColor));
    }

    private float trimSize(float size) {
        if (size < MIN_SIZE) {
            size = MIN_SIZE;
        }
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
        return size;
    }

    private void adjustSizeTo(float size) {
        mSize = trimSize(size);
        notifyBrushChanged();
    }

    private void adjustSizeBy(float dSize) {
        adjustSizeTo(mSize + dSize);
    }

    private int trimAlpha(int alpha) {
        if (alpha < MIN_ALPHA) {
            alpha = MIN_ALPHA;
        }
        if (alpha > MAX_ALPHA) {
            alpha = MAX_ALPHA;
        }
        return alpha;
    }

    private void adjustAlphaTo(int alpha) {
        mAlpha = trimAlpha(alpha);
        notifyBrushChanged();
    }

    private void adjustAlphaBy(int dAlpha) {
        adjustAlphaTo(mAlpha + dAlpha);
    }

    private void adjustBrushTypeTo(int brushType) {
        mBrushType = brushType;
        notifyBrushChanged();
    }

    Brush createBrush() {
        if (mBrushType == DoodleData.BRUSH_TYPE_EMPTY) {
            return new Empty();
        } else if (mBrushType == DoodleData.BRUSH_TYPE_LEAVES) {
            return new LeavesPencil(mColor, mSize, mAlpha);
        } else if (mBrushType == DoodleData.BRUSH_TYPE_PENCIL) {
            return new Pencil(mColor, mSize, mAlpha);
        }
        throw new IllegalArgumentException("error brush type:" + mBrushType);
    }

    private static int removeAlpha(int color) {
        return Color.argb(255, Color.alpha(color), Color.green(color), Color.blue(color));
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putFloat(EXTRA_SIZE, mSize);
        outState.putInt(EXTRA_ALPHA, mAlpha);
        outState.putInt(EXTRA_COLOR, mColor);
        outState.putInt(EXTRA_BRUSH_TYPE, mBrushType);
    }

    private void disableDoodleAction() {
        mUndo.setEnabled(false);
        mRedo.setEnabled(false);
    }

    private void syncUndoRedoStatus(DoodleView doodleView) {
        disableDoodleAction();
        doodleView.canUndo(new DoodleView.ActionCallback() {
            @Override
            public void onActionResult(boolean success) {
                mUndo.setEnabled(success);
            }
        });
        doodleView.canRedo(new DoodleView.ActionCallback() {
            @Override
            public void onActionResult(boolean success) {
                mRedo.setEnabled(success);
            }
        });
    }

    private class MorePanel {
        private final View mMoreOutsideTouch;
        private final View mMorePanelView;
        private final View mSave;
        private final View mBrushPencil;
        private final View mBrushLeaves;

        private MorePanel(View rootView) {
            mMoreOutsideTouch = ViewUtil.findViewByID(rootView, R.id.action_panel_more_touch_outside);
            mMoreOutsideTouch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    hide();
                }
            });
            mMorePanelView = ViewUtil.findViewByID(rootView, R.id.action_panel_more);
            mSave = ViewUtil.findViewByID(mMorePanelView, R.id.save);
            mSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getActionListener().saveAsBitmap();
                }
            });
            mBrushPencil = ViewUtil.findViewByID(mMorePanelView, R.id.brush_pencil);
            mBrushPencil.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    adjustBrushTypeTo(DoodleData.BRUSH_TYPE_PENCIL);
                }
            });
            mBrushLeaves = ViewUtil.findViewByID(mMorePanelView, R.id.brush_leaves);
            mBrushLeaves.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    adjustBrushTypeTo(DoodleData.BRUSH_TYPE_LEAVES);
                }
            });
        }

        void show() {
            mMoreOutsideTouch.setVisibility(View.VISIBLE);
            mMorePanelView.setVisibility(View.VISIBLE);
        }

        void hide() {
            mMoreOutsideTouch.setVisibility(View.GONE);
            mMorePanelView.setVisibility(View.GONE);
        }

    }

    public static class SimpleActionListener implements ActionListener {

        @Override
        public void saveAsBitmap() {
            // ignore
        }

        @Override
        public void onBrushChanged() {
        }

    }

}