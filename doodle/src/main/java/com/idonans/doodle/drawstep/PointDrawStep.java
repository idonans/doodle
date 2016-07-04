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

    // 画点只有一个播放步骤
    private boolean mPlayed;

    @Override
    public void resetPlayStep() {
        mPlayed = false;
    }

    @Override
    public int getPlayStepCountTotal() {
        return 1;
    }

    @Override
    public int getPlayStepCountPlayed() {
        return mPlayed ? 1 : 0;
    }

    @Override
    public int getPlayStepCountRemain() {
        return mPlayed ? 0 : 1;
    }

    @Override
    public int playSteps(int stepSize) {
        if (mPlayed) {
            return 0;
        }
        mPlayed = true;
        return 1;
    }

}
