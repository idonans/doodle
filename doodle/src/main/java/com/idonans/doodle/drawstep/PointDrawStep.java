package com.idonans.doodle.drawstep;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;

import com.idonans.doodle.brush.Brush;

/**
 * 画一个点
 * Created by idonans on 16-5-17.
 */
public class PointDrawStep extends DrawStep {

    private final Paint mPaint;
    private final float mX;
    private final float mY;

    /**
     * 用铅笔画点
     */
    public PointDrawStep(@NonNull Brush brush, float x, float y) {
        super(brush);
        mPaint = brush.createPaint();
        mX = x;
        mY = y;
    }

    public float getX() {
        return mX;
    }

    public float getY() {
        return mY;
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        canvas.drawPoint(mX, mY, mPaint);
    }

}
