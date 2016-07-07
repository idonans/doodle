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
    public boolean hasDrawContent() {
        // 如果只有一个绘制点，则相当于没有内容
        return mAllPoints.size() > 2;
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        if (mSubStepHelper != null) {
            mSubStepHelper.onDraw(canvas);
        } else {
            canvas.drawPath(mPath, mPaint);
        }
    }

    private SubStepHelper mSubStepHelper;

    @Override
    public void resetSubStep() {
        int subStepCount = 0;
        ArrayList<Float> allPoints = new ArrayList<>(mAllPoints);
        if (allPoints.size() > 2) {
            subStepCount = (allPoints.size() - 2) / 2;
        }
        mSubStepHelper = new SubStepHelper(mPaint, allPoints, subStepCount);
    }

    @Override
    public int getSubStepCount() {
        return mSubStepHelper.getCount();
    }

    @Override
    public int getSubStepMoved() {
        return mSubStepHelper.getMoved();
    }

    @Override
    public int moveSubStepBy(int count) {
        return mSubStepHelper.moveBy(count);
    }


    private static class SubStepHelper extends Helper {

        private final Paint mPaint;

        private final Path mPath;
        private float mPreX;
        private float mPreY;

        private final ArrayList<Float> mAllPoints;

        public SubStepHelper(Paint paint, ArrayList<Float> allPoints, int count) {
            super(count);
            mPaint = paint;
            mAllPoints = allPoints;

            checkSize(mAllPoints.size());

            mPath = new Path();
            resetPath();
        }

        private void resetPath() {
            float x = mAllPoints.get(0);
            float y = mAllPoints.get(1);

            mPath.reset();
            mPath.moveTo(x, y);
            mPreX = x;
            mPreY = y;
        }

        public void toPoint(float x, float y) {
            mPath.quadTo(mPreX, mPreY, (mPreX + x) / 2, (mPreY + y) / 2);
            mPreX = x;
            mPreY = y;
        }

        @Override
        public int moveBy(int count) {
            final int movedCountBefore = getMoved();
            final int movedCountThis = super.moveBy(count);

            if (movedCountThis > 0) {
                // 向右移动
                for (int i = 0; i < movedCountThis; i++) {
                    // 在现有 path 后面追加移动的点
                    int indexAppendX = (1/*起始点*/ + movedCountBefore/*之前移动的点*/ + i/*本次移动的点*/) * 2/*每一点（一个坐标）对应两个位置*/;
                    int indexAppendY = indexAppendX + 1;
                    toPoint(mAllPoints.get(indexAppendX), mAllPoints.get(indexAppendY));
                }
            } else if (movedCountThis < 0) {
                // 向左移动
                // path 不支持回退，需要从起始点重新绘制
                int moved = getMoved(); /*相当于 (movedCountBefore + movedCountThis)*/
                resetPath();
                for (int i = 0; i < moved; i++) {
                    // 从起始点依次追加到当前移动的位置
                    int indexAppendX = (1/*起始点*/ + i/*追加的点*/) * 2/*每一点（一个坐标）对应两个位置*/;
                    int indexAppendY = indexAppendX + 1;
                    toPoint(mAllPoints.get(indexAppendX), mAllPoints.get(indexAppendY));
                }
            }

            return movedCountThis;
        }

        @Override
        public void onDraw(@NonNull Canvas canvas) {
            int moved = getMoved();
            if (moved > 0) {
                canvas.drawPath(mPath, mPaint);
            }
        }

    }

    private static void checkSize(int size) {
        if (size % 2 != 0) {
            throw new IllegalArgumentException("size error, not covert as point. " + size);
        }
    }

}
