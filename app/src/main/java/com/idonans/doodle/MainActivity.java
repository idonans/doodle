package com.idonans.doodle;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.idonans.acommon.util.ViewUtil;

public class MainActivity extends AppCompatActivity {

    private DoodleView mDoodleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDoodleView = ViewUtil.findViewByID(this, R.id.doodle_view);
    }

}
