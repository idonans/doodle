package com.idonans.doodle.demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.idonans.acommon.app.CommonActivity;
import com.idonans.acommon.util.ViewUtil;
import com.idonans.doodle.DoodleViewPlayer;

/**
 * Created by pengji on 16-7-4.
 */
public class DoodlePlayActivity extends CommonActivity {

    public static final String EXTRA_DD_FILE_PATH = "extra.DD_FILE_PATH";
    public static final String EXTRA_DD_FILE_IGNORE_EMPTY_DRAW_STEP = "extra.DD_FILE_IGNORE_EMPTY_DRAW_STEP";

    private DoodleViewPlayer mDoodleViewPlayer;

    public static Intent start(Context context, String ddFilePath, boolean ignoreEmptyDrawStep) {
        Intent intent = new Intent(context, DoodlePlayActivity.class);
        intent.putExtra(EXTRA_DD_FILE_PATH, ddFilePath);
        intent.putExtra(EXTRA_DD_FILE_IGNORE_EMPTY_DRAW_STEP, ignoreEmptyDrawStep);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String ddFilePath = getIntent().getStringExtra(EXTRA_DD_FILE_PATH);
        boolean ignoreEmptyDrawStep = getIntent().getBooleanExtra(EXTRA_DD_FILE_IGNORE_EMPTY_DRAW_STEP, true);

        setContentView(R.layout.doodle_play_activity);
        mDoodleViewPlayer = ViewUtil.findViewByID(this, R.id.doodle_view_player);
        mDoodleViewPlayer.play(ddFilePath, ignoreEmptyDrawStep);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDoodleViewPlayer.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDoodleViewPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDoodleViewPlayer.close();
    }

}
