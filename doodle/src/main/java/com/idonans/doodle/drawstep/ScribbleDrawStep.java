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

}
