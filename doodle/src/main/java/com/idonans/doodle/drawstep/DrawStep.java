package com.idonans.doodle.drawstep;

import android.graphics.Canvas;
import android.support.annotation.NonNull;

import com.idonans.doodle.DoodleView;
import com.idonans.doodle.DoodleViewPlayer;
import com.idonans.doodle.brush.Brush;

/**
 * 一个绘画步骤
 * Created by idonans on 16-5-17.
 */
public abstract class DrawStep implements DoodleViewPlayer.IStepPlayable {

    protected final Brush mDrawBrush;

    public DrawStep(Brush drawBrush) {
        mDrawBrush = drawBrush;
    }

    public Brush getDrawBrush() {
        return mDrawBrush;
    }

    public abstract void onDraw(@NonNull Canvas canvas);

    /**
     * 当前的绘画步骤是否消费该绘画手势，如果消费了返回 true, 否则返回 false.
     */
    public boolean dispatchGestureAction(DoodleView.GestureAction gestureAction, Brush brush) {
        if (mDrawBrush != brush) {
            // 画笔变更，开始新的绘画步骤
            return false;
        }
        if (gestureAction == null || brush == null) {
            return false;
        }
        return onGestureAction(gestureAction);
    }

    /**
     * 当前的绘画步骤是否消费该绘画手势，如果消费了返回 true, 否则返回 false.
     */
    protected boolean onGestureAction(@NonNull DoodleView.GestureAction gestureAction) {
        return false;
    }

}
