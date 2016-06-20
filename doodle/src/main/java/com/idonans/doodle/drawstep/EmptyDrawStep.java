package com.idonans.doodle.drawstep;

import android.graphics.Canvas;
import android.support.annotation.NonNull;

/**
 * 空的绘画步骤, 一般用来辅助标记前一个绘画步骤的结束
 * Created by idonans on 16-5-17.
 */
public final class EmptyDrawStep extends DrawStep {

    public EmptyDrawStep() {
        super(null);
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        // ignore
    }

}
