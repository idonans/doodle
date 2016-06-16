package com.idonans.doodle.drawstep;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Parcel;
import android.support.annotation.NonNull;

import com.idonans.doodle.brush.Pencil;

/**
 * 画一个点
 * Created by idonans on 16-5-17.
 */
public class PointDrawStep extends DrawStep {

    private final Paint mPaint;
    private final float mX;
    private final float mY;

    public PointDrawStep(Parcel in) {
        super(in);
        mPaint = mDrawBrush.createPaint();
        mX = in.readFloat();
        mY = in.readFloat();
    }

    /**
     * 用铅笔画点
     */
    public PointDrawStep(@NonNull Pencil pencil, float x, float y) {
        super(pencil);
        mPaint = pencil.createPaint();
        mX = x;
        mY = y;
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        canvas.drawPoint(mX, mY, mPaint);
    }

    @Override
    public void writeToParcel(Parcel out) {
        super.writeToParcel(out);
        out.writeFloat(mX);
        out.writeFloat(mY);
    }

}
