package com.idonans.doodle.drawstep;

import android.graphics.Canvas;
import android.os.Parcel;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import com.idonans.doodle.DoodleView;
import com.idonans.doodle.brush.Brush;

/**
 * 一个绘画步骤
 * Created by idonans on 16-5-17.
 * @see #DrawStep(Parcel)
 * @see #writeToParcel(Parcel)
 */
public abstract class DrawStep {

    protected final Brush mDrawBrush;

    /**
     * 子类需要重写此构造函数，并且提供同样的参数列表 在涂鸦板做数据的保存与恢复时会用到
     */
    public DrawStep(Parcel in) {
        String brushClass = in.readString();
        if (brushClass == null) {
            mDrawBrush = null;
        } else {
            try {
                mDrawBrush = (Brush) Class.forName(brushClass).getConstructor(Parcel.class).newInstance(in);
            } catch (Exception e) {
                throw new RuntimeException("error to restore draw draw brush");
            }
        }
    }

    public DrawStep(Brush drawBrush) {
        mDrawBrush = drawBrush;
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
     * 子类需要重写此方法，并且提供同样的参数列表 在涂鸦板做数据的保存与恢复时会用到
     */
    @CallSuper
    public void writeToParcel(Parcel out) {
        if (mDrawBrush == null) {
            out.writeString(null);
        } else {
            out.writeString(mDrawBrush.getClass().getName());
            mDrawBrush.writeToParcel(out);
        }
    }

}
