package com.idonans.doodle.brush;

import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.idonans.doodle.DoodleView;
import com.idonans.doodle.drawstep.DrawStep;
import com.idonans.doodle.drawstep.EmptyDrawStep;

/**
 * 画刷
 * Created by idonans on 16-5-17.
 */
public abstract class Brush {

    /**
     * 画刷的颜色 ARGB
     */
    public final int color;

    /**
     * 画刷的大小
     */
    public final float size;

    /**
     * 画刷的透明度 [0, 255], 值越大越不透明
     */
    public final int alpha;

    public Brush(int color, float size, int alpha) {
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
    public abstract Brush cloneWithSize(float size);

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
