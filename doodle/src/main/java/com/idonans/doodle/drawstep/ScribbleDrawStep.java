package com.idonans.doodle.drawstep;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.NonNull;

import com.idonans.doodle.DoodleView;
import com.idonans.doodle.brush.Brush;

import java.util.ArrayList;

/**
 * 自由绘制
 * Created by idonans on 16-5-17.
 */
public class ScribbleDrawStep extends DrawStep {

    private final Paint mPaint;

    private final Path mPath;
    private float mPreX;
    private float mPreY;

    private final ArrayList<Float> mAllPoints;

    /**
     * 自由绘制
     */
    public ScribbleDrawStep(@NonNull Brush brush, float x, float y) {
        super(brush);
        mPaint = brush.createPaint();

        mPath = new Path();
        mPath.moveTo(x, y);
        mPreX = x;
        mPreY = y;

        mAllPoints = new ArrayList<>();
        mAllPoints.add(x);
        mAllPoints.add(y);
    }

    public ArrayList<Float> getAllPoints() {
        return mAllPoints;
    }

    /**
     * 绘画平滑曲线
     */
    public void toPoint(float x, float y) {
        // 使用贝塞尔去绘制，线条更平滑
        mPath.quadTo(mPreX, mPreY, (mPreX + x) / 2, (mPreY + y) / 2);
        mPreX = x;
        mPreY = y;
        mAllPoints.add(x);
        mAllPoints.add(y);
    }

    @Override
    protected boolean onGestureAction(@NonNull DoodleView.GestureAction gestureAction) {
        if (!(gestureAction instanceof DoodleView.ScrollGestureAction)) {
            return false;
        }

        DoodleView.ScrollGestureAction scrollGestureAction = (DoodleView.ScrollGestureAction) gestureAction;

        // 快速滑动时，在两次 ACTION_MOVE 之间可能有未绘制的点，从历史中找回
        int historySize = scrollGestureAction.currentEvent.getHistorySize();
        for (int i = 0; i < historySize; i++) {
            toPoint(scrollGestureAction.currentEvent.getHistoricalX(i),
                    scrollGestureAction.currentEvent.getHistoricalY(i));
        }

        toPoint(scrollGestureAction.currentEvent.getX(),
                scrollGestureAction.currentEvent.getY());
        return true;
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        canvas.drawPath(mPath, mPaint);
    }

    // 已经播放的点的数量
    private int mPlayedPointSize;

    @Override
    public void resetPlayStep() {
        mPlayedPointSize = 0;
    }

    @Override
    public int getPlayStepCountTotal() {
        int size = mAllPoints.size();
        checkSize(size);
        return size / 2;
    }

    @Override
    public int getPlayStepCountPlayed() {
        checkSize(mPlayedPointSize);
        return mPlayedPointSize / 2;
    }

    @Override
    public int getPlayStepCountRemain() {
        int size = mAllPoints.size() - mPlayedPointSize;
        checkSize(size);
        return size / 2;
    }

    @Override
    public int playSteps(int stepSize) {
        if (stepSize < 1) {
            throw new IllegalArgumentException("step size error, must > 0, " + stepSize);
        }

        // 每一步播放两个点
        int stepPlayedThis = 0;
        while (stepSize > 0) {

            if (mPlayedPointSize < 0) {
                throw new IllegalAccessError("played point size error " + mPlayedPointSize);
            }

            int playStepCountRemain = getPlayStepCountRemain();
            if (playStepCountRemain < 0) {
                throw new IllegalAccessError("play step count remain < 0, " + playStepCountRemain);
            }
            if (playStepCountRemain == 0) {
                // 没有更多可以播放的步骤
                break;
            }

            if (mPlayedPointSize == 0) {
                mPath.reset();
                float x = mAllPoints.get(0);
                float y = mAllPoints.get(1);
                mPath.moveTo(mAllPoints.get(0), mAllPoints.get(1));
                mPreX = x;
                mPreY = y;
            } else {
                float x = mAllPoints.get(mPlayedPointSize);
                float y = mAllPoints.get(mPlayedPointSize + 1);
                mPath.quadTo(mPreX, mPreY, (mPreX + x) / 2, (mPreY + y) / 2);
                mPreX = x;
                mPreY = y;
            }

            mPlayedPointSize += 2;

            stepSize--;
            stepPlayedThis++;
        }

        return stepPlayedThis;
    }

    private static void checkSize(int size) {
        if (size % 2 != 0) {
            throw new IllegalArgumentException("size error, not covert as point. " + size);
        }
    }

}
