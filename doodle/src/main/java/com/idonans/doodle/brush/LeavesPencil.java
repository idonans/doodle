package com.idonans.doodle.brush;

import android.graphics.Paint;
import android.os.Parcel;
import android.support.annotation.NonNull;

/**
 * 柳叶笔
 * Created by pengji on 16-6-15.
 */
public class LeavesPencil extends Pencil {

    public LeavesPencil(@NonNull Parcel in) {
        super(in);
    }

    public LeavesPencil(int color, int size, int alpha) {
        super(color, size, alpha);
    }

    @Override
    public Brush cloneWithColor(int color) {
        return new LeavesPencil(color, size, alpha);
    }

    @Override
    public Brush cloneWithSize(int size) {
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

    @Override
    public void writeToParcel(@NonNull Parcel out) {
        super.writeToParcel(out);
    }

}
