package com.idonans.doodle.brush;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.idonans.doodle.DoodleView;
import com.idonans.doodle.drawstep.DrawStep;
import com.idonans.doodle.drawstep.PointDrawStep;
import com.idonans.doodle.drawstep.ScribbleDrawStep;

/**
 * 铅笔
 * Created by idonans on 16-5-17.
 */
public class Pencil extends Brush {

    public Pencil(int color, float size, int alpha) {
        super(color, size, alpha);
    }

    @Override
    public Brush cloneWithColor(int color) {
        return new Pencil(color, size, alpha);
    }

    @Override
    public Brush cloneWithSize(float size) {
        return new Pencil(color, size, alpha);
    }

    @Override
    public Brush cloneWithAlpha(int alpha) {
        return new Pencil(color, size, alpha);
    }

    /**
     * 根据手势，结合当前画刷，创建一个新的绘画步骤， 如果返回 null, 将会被替换为 EmptyDrawStep
     */
    @Nullable
    @Override
    protected DrawStep onCreateDrawStep(@NonNull DoodleView.GestureAction gestureAction) {

        // 铅笔可以用来画点
        if (gestureAction instanceof DoodleView.SinglePointGestureAction) {
            DoodleView.SinglePointGestureAction singlePointGestureAction = (DoodleView.SinglePointGestureAction) gestureAction;
            return new PointDrawStep(this, singlePointGestureAction.event.getX(), singlePointGestureAction.event.getY());
        }

        // 铅笔可以用来自由绘制
        if (gestureAction instanceof DoodleView.ScrollGestureAction) {
            DoodleView.ScrollGestureAction scrollGestureAction = (DoodleView.ScrollGestureAction) gestureAction;
            ScribbleDrawStep drawStep = new ScribbleDrawStep(this,
                    scrollGestureAction.downEvent.getX(),
                    scrollGestureAction.downEvent.getY());
            drawStep.toPoint(scrollGestureAction.currentEvent.getX(),
                    scrollGestureAction.currentEvent.getY());
            return drawStep;
        }

        return null;
    }

}
