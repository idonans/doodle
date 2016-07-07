package com.idonans.doodle.drawstep;

import android.graphics.Canvas;
import android.support.annotation.NonNull;

/**
 * DrawStep 内部可以进一步细分的子步骤
 * Created by pengji on 16-7-7.
 */
public interface SubStep {

    /**
     * 重置子步骤到初始状态
     */
    void resetSubStep();

    /**
     * 获取包含的子步骤数量
     */
    int getSubStepCount();

    /**
     * 获取当前已经移动的子步骤数量
     */
    int getSubStepMoved();

    /**
     * <pre>
     * 移动指定数量的子步骤（带方向），如果 count > 0, 表示向后移动；如果 count < 0, 表示向前移动；
     * 返回实际移动的步骤数量（带方向）, 如果没有移动步骤，返回 0.
     * 例如：
     * 向前移动 2 步，实际只是向前移动了 1 步，则返回 -1
     * int stepMoved = moveSubStepBy(-2); // return -1
     * </pre>
     */
    int moveSubStepBy(int count);

    abstract class Helper {

        private final int mCount;
        private int mMoved;

        public Helper(int count) {
            if (count < 0) {
                throw new IllegalArgumentException("count must >= 0");
            }
            mCount = count;
        }

        public int getMoved() {
            return mMoved;
        }

        public int getCount() {
            return mCount;
        }

        public int moveBy(int count) {
            int tmpMoved = mMoved + count;
            if (tmpMoved < 0) {
                tmpMoved = 0;
            } else if (tmpMoved > mCount) {
                tmpMoved = mCount;
            }

            int movedCountThis = tmpMoved - mMoved;
            mMoved = tmpMoved;
            return movedCountThis;
        }

        public abstract void onDraw(@NonNull Canvas canvas);
    }

}
