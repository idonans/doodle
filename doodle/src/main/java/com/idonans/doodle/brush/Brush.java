package com.idonans.doodle.brush;

import android.graphics.Paint;
import android.os.Parcel;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.idonans.doodle.DoodleView;
import com.idonans.doodle.drawstep.DrawStep;
import com.idonans.doodle.drawstep.EmptyDrawStep;

/**
 * 画刷 子类必须要重写 Brush#Parcel 构造函数 和 #writeToParcel 方法
 * Created by idonans on 16-5-17.
 * @see #Brush(Parcel)
 * @see #writeToParcel(Parcel)
 */
public abstract class Brush {

    /**
     * 画刷的颜色 ARGB
     */
    public final int color;

    /**
     * 画刷的大小
     */
    public final int size;

    /**
     * 画刷的透明度 [0, 255], 值越大越不透明
     */
    public final int alpha;

    /**
     * 子类需要重写此构造函数，并且提供同样的参数列表 在涂鸦板做数据的保存与恢复时会用到
     */
    public Brush(@NonNull Parcel in) {
        this.color = in.readInt();
        this.size = in.readInt();
        this.alpha = in.readInt();
    }

    /**
     * 子类需要重写此方法，并且提供同样的参数列表 在涂鸦板做数据的保存与恢复时会用到
     */
    @CallSuper
    public void writeToParcel(@NonNull Parcel out) {
        out.writeInt(this.color);
        out.writeInt(this.size);
        out.writeInt(this.alpha);
    }

    public Brush(int color, int size, int alpha) {
        this.color = color;
        this.size = size;
        this.alpha = alpha;
    }

    /**
     * 使用新的颜色克隆一个新的画笔, 通常不能返回当前对象
     */
    public abstract Brush cloneWithColor(int color);

    /**
     * 使用新的 size 克隆一个新的画笔, 通常不能返回当前对象
     */
    public abstract Brush cloneWithSize(int size);

    /**
     * 使用新的透明度克隆一个新的画笔, 通常不能返回当前对象
     */
    public abstract Brush cloneWithAlpha(int alpha);

    /**
     * 根据当前画刷配置创建 paint
     */
    public Paint createPaint() {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setAlpha(alpha);
        paint.setStrokeWidth(size);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND); // 笔刷样式 圆形
        paint.setDither(true); // 图像抖动处理 使图像更清晰
        paint.setAntiAlias(true); // 抗锯齿
        return paint;
    }

    /**
     * 根据手势，结合当前画刷，创建一个新的绘画步骤， 不能返回 null
     */
    @NonNull
    public DrawStep createDrawStep(DoodleView.GestureAction gestureAction) {
        if (gestureAction == null) {
            return new EmptyDrawStep();
        }
        DrawStep drawStep = onCreateDrawStep(gestureAction);
        if (drawStep == null) {
            drawStep = new EmptyDrawStep();
        }
        return drawStep;
    }

    /**
     * 根据手势，结合当前画刷，创建一个新的绘画步骤， 如果返回 null, 将会被替换为 EmptyDrawStep
     */
    @Nullable
    protected abstract DrawStep onCreateDrawStep(@NonNull DoodleView.GestureAction gestureAction);

}
