package com.idonans.doodle.brush;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.idonans.doodle.DoodleView;
import com.idonans.doodle.drawstep.DrawStep;

/**
 * 空画笔，什么都不做
 * Created by idonans on 16-20-17.
 */
public final class Empty extends Brush {

    public Empty() {
        super(0, 0, 0);
    }

    @Override
    public Brush cloneWithColor(int color) {
        return this;
    }

    @Override
    public Brush cloneWithSize(int size) {
        return this;
    }

    @Override
    public Brush cloneWithAlpha(int alpha) {
        return this;
    }

    /**
     * 根据手势，结合当前画刷，创建一个新的绘画步骤， 如果返回 null, 将会被替换为 EmptyDrawStep
     */
    @Nullable
    @Override
    protected DrawStep onCreateDrawStep(@NonNull DoodleView.GestureAction gestureAction) {
        return null;
    }

}
