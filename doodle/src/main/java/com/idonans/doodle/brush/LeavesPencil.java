package com.idonans.doodle.brush;

import android.graphics.Paint;

/**
 * 柳叶笔
 * Created by pengji on 16-6-15.
 */
public class LeavesPencil extends Pencil {

    public LeavesPencil(int color, float size, int alpha) {
        super(color, size, alpha);
    }

    @Override
    public Brush cloneWithColor(int color) {
        return new LeavesPencil(color, size, alpha);
    }

    @Override
    public Brush cloneWithSize(float size) {
        return new LeavesPencil(color, size, alpha);
    }

    @Override
    public Brush cloneWithAlpha(int alpha) {
        return new LeavesPencil(color, size, alpha);
    }

    @Override
    public Paint createPaint() {
        Paint paint = super.createPaint();
        paint.setStyle(Paint.Style.FILL);
        return paint;
    }

}
