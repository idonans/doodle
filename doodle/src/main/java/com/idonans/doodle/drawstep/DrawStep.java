package com.idonans.doodle.drawstep;

import android.graphics.Canvas;
import android.support.annotation.NonNull;

import com.idonans.doodle.DoodleView;
import com.idonans.doodle.brush.Brush;

import java.util.Collection;

/**
 * 一个绘画步骤
 * Created by idonans on 16-5-17.
 */
public abstract class DrawStep implements SubStep {

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

    /**
     * 是否有有效的绘画内容, 如画点，画线等. 不具有有效绘画内容的步骤可能会被忽略。
     */
    public boolean hasDrawContent() {
        return true;
    }

    /**
     * 判断指定的这些绘画步骤中是否包含任意有效的绘画内容
     */
    public static boolean hasDrawContent(Collection<DrawStep> drawSteps) {
        if (drawSteps == null || drawSteps.size() <= 0) {
            return false;
        }

        for (DrawStep drawStep : drawSteps) {
            if (drawStep.hasDrawContent()) {
                return true;
            }
        }

        return false;
    }

}
