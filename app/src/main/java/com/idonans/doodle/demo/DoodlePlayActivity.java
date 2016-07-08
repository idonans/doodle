package com.idonans.doodle.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.idonans.acommon.app.CommonActivity;
import com.idonans.acommon.util.ViewUtil;
import com.idonans.doodle.player.DoodleViewPlayer;

/**
 * Created by pengji on 16-7-4.
 */
public class DoodlePlayActivity extends CommonActivity {

    public static final String EXTRA_DD_FILE_PATH = "extra.DD_FILE_PATH";
    public static final String EXTRA_DD_FILE_IGNORE_EMPTY_DRAW_STEP = "extra.DD_FILE_IGNORE_EMPTY_DRAW_STEP";

    private DoodleViewPlayer mDoodleViewPlayer;
    private PlayControllerPanel mPlayControllerPanel;
    private boolean mResumeToPlay;

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
        mDoodleViewPlayer.setDoodleData(ddFilePath, ignoreEmptyDrawStep, true);

        mPlayControllerPanel = new PlayControllerPanel(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mResumeToPlay) {
            mDoodleViewPlayer.play();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mResumeToPlay = mDoodleViewPlayer.isPlaying();
        mDoodleViewPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDoodleViewPlayer.stop();
    }

    private class PlayControllerPanel {

        private final View mView;
        private final View mSeekBack;
        private final View mSeekForward;
        private final View mPlayOrPause;
        private final View mSpeedUp;
        private final View mSpeedDown;

        private long mSpeedDelay = 20L;

        private PlayControllerPanel(Activity activity) {
            mView = ViewUtil.findViewByID(activity, R.id.play_controller_panel);
            mSeekBack = ViewUtil.findViewByID(mView, R.id.seek_back);
            mSeekForward = ViewUtil.findViewByID(mView, R.id.seek_forward);
            mPlayOrPause = ViewUtil.findViewByID(mView, R.id.play_pause);
            mSpeedUp = ViewUtil.findViewByID(mView, R.id.speed_up);
            mSpeedDown = ViewUtil.findViewByID(mView, R.id.speed_down);

            mSeekBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mDoodleViewPlayer.seekBy(-10);
                }
            });
            mSeekForward.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mDoodleViewPlayer.seekBy(10);
                }
            });
            mPlayOrPause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mDoodleViewPlayer.isPlaying()) {
                        mDoodleViewPlayer.pause();
                    } else {
                        mDoodleViewPlayer.play();
                    }
                }
            });
            mSpeedUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    syncSpeed((long) (mSpeedDelay * 0.8f));
                }
            });
            mSpeedDown.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    syncSpeed((long) (mSpeedDelay * 1.2f));
                }
            });

            syncSpeed(mSpeedDelay);
        }

        private void syncSpeed(long speedDelay) {
            if (speedDelay < 0L) {
                speedDelay = 0L;
            } else if (speedDelay > 500L) {
                speedDelay = 500L;
            }

            mSpeedDelay = speedDelay;
            mDoodleViewPlayer.setSpeedDelay(mSpeedDelay);
        }

    }

}
