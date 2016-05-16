package com.idonans.doodle;

import android.graphics.Color;
import android.os.Bundle;

import com.idonans.acommon.app.CommonActivity;
import com.idonans.acommon.util.ViewUtil;

public class MainActivity extends CommonActivity {

    private DoodleView mDoodleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDoodleView = ViewUtil.findViewByID(this, R.id.doodle_view);
        mDoodleView.setBrush(DoodleView.Brush.createPencil(Color.BLACK, 10));
    }

}
