package com.idonans.doodle;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.idonans.acommon.AppContext;
import com.idonans.acommon.lang.Available;
import com.idonans.acommon.lang.CommonLog;
import com.idonans.acommon.lang.TaskQueue;
import com.idonans.acommon.lang.Threads;
import com.idonans.acommon.util.ViewUtil;
import com.idonans.doodle.dd.DoodleDataEditor;
import com.idonans.doodle.dd.v1.DoodleDataEditorV1;

import java.util.ArrayList;
import java.util.Collections;

/**
 * DoodleView 播放器
 * Created by pengji on 16-7-4.
 */
public class DoodleViewPlayer extends FrameLayout {

    private DoodleView mDoodleView;
    private TaskQueue mTaskQueue;
    private PlayController mPlayController;
    private long mSpeed = 1000L;

    public DoodleViewPlayer(Context context) {
        super(context);
        init();
    }

    public DoodleViewPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DoodleViewPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DoodleViewPlayer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        if (isInEditMode()) {
            return;
        }

        Context context = getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        layoutInflater.inflate(R.layout.doodle_player_layout, this, true);

        mDoodleView = ViewUtil.findViewByID(this, R.id.doodle_view);
        mDoodleView.setReadOnly(true);

        mTaskQueue = new TaskQueue(1);
    }

    public long getSpeed() {
        return mSpeed;
    }

    public void setSpeed(long speed) {
        mSpeed = speed;
    }

    public void play(final String ddFile) {
        // TODO 如何快速清空当前内容 ？ 设置播放的标示？终止当前正在播放的逻辑？
        final PlayController playController = new PlayController();
        mPlayController = playController;

        mDoodleView.postShowLoading();
        mTaskQueue.enqueue(new Runnable() {
            @Override
            public void run() {
                Object[] ret = DoodleDataEditorV1Loader.load(ddFile);

                if (!playController.isAvailable()) {
                    return;
                }

                final int errorCodeDDFile = (int) ret[0];
                if (errorCodeDDFile == ERROR_CODE_DD_FILE_OK) {
                    // dd 文件解析成功
                    DoodleData doodleData = (DoodleData) ret[1];
                    // play doodle data
                    resetDoodleData(doodleData);
                    if (playController.isAvailable()) {
                        mDoodleView.load(doodleData);
                        playController.play();
                    }
                } else {
                    // 清空 doodle view
                    mDoodleView.setAspectRatio(1, 1);
                    showDDFileErrorMessage(errorCodeDDFile);
                }
            }
        });
    }

    public boolean isPlaying() {
        return mPlayController != null;
    }

    public void pause() {
        mPlayController = null;
    }

    public void resume() {
        if (!isPlaying()) {
            final PlayController playController = new PlayController();
            mPlayController = playController;
            mTaskQueue.enqueue(new Runnable() {
                @Override
                public void run() {
                    if (playController.isAvailable()) {
                        playController.play();
                    }
                }
            });
        }
    }

    private void showDDFileErrorMessage(int errorCodeDDFile) {
        switch (errorCodeDDFile) {
            case ERROR_CODE_DD_FILE_NOT_FOUND:
                showLog("播放文件未找到");
                break;
            case ERROR_CODE_DD_FILE_ERROR:
                showLog("播放文件被破坏");
                break;
            case ERROR_CODE_DD_FILE_VERION_UNSUPPORT:
                showLog("播放文件是在另一个版本中录制的");
                break;
            case ERROR_CODE_DD_FILE_OK:
            default:
                new RuntimeException().printStackTrace();
                break;
        }
    }

    public void close() {
        mPlayController = null;
    }

    private class PlayController implements Available, Runnable {

        private static final String TAG = "PlayController";
        private boolean mCalledPlay;

        public void play() {
            if (mCalledPlay) {
                CommonLog.e(TAG + " play already called");
                new RuntimeException().printStackTrace();
                return;
            }
            mCalledPlay = true;

            pendingPlay();
        }

        /**
         * 等待 doodle view 初始化完成之后，开始 play
         */
        private void pendingPlay() {
            mTaskQueue.enqueue(new Runnable() {
                @Override
                public void run() {
                    if (PlayController.this.isAvailable()) {
                        mDoodleView.isInitOk(new DoodleView.ActionCallback() {
                            @Override
                            public void onActionResult(boolean success) {
                                if (success) {
                                    enqueueNext();
                                } else {
                                    Threads.sleepQuietly(200L);
                                    pendingPlay();
                                }
                            }
                        });
                    }
                }
            });
        }

        private void enqueueNext() {
            mTaskQueue.enqueue(this);
        }

        private void playNext() {
            mDoodleView.undo();
        }

        @Override
        public void run() {
            if (!isAvailable()) {
                return;
            }
            playNext();

            Threads.sleepQuietly(getSpeed());
            mDoodleView.canUndo(new DoodleView.ActionCallback() {
                @Override
                public void onActionResult(boolean success) {
                    if (success) {
                        enqueueNext();
                    }
                }
            });
        }

        @Override
        public boolean isAvailable() {
            return DoodleViewPlayer.this.mPlayController == this;
        }
    }

    private void resetDoodleData(@NonNull DoodleData doodleData) {
        // 清空现有的 redo, 并将现有绘画步骤全部移动到 redo 中
        doodleData.drawStepDatasRedo = null;
        ArrayList<DoodleData.DrawStepData> drawStepDatas = doodleData.drawStepDatas;
        doodleData.drawStepDatas = null;
        if (drawStepDatas != null && drawStepDatas.size() > 0) {
            Collections.reverse(drawStepDatas);
            doodleData.drawStepDatasRedo = drawStepDatas;
        }
    }

    public static final int ERROR_CODE_DD_FILE_OK = 0;
    /**
     * dd 文件没有找到
     */
    public static final int ERROR_CODE_DD_FILE_NOT_FOUND = 1;
    /**
     * dd 文件版本不支持
     */
    public static final int ERROR_CODE_DD_FILE_VERION_UNSUPPORT = 2;
    /**
     * dd 文件内容错误
     */
    public static final int ERROR_CODE_DD_FILE_ERROR = 3;

    private static class DoodleDataEditorV1Loader {

        public static Object[] load(final String ddFilePath) {
            Object[] ret = new Object[2];
            ret[0] = ERROR_CODE_DD_FILE_OK;
            if (TextUtils.isEmpty(ddFilePath)) {
                ret[0] = ERROR_CODE_DD_FILE_NOT_FOUND;
                return ret;
            }

            int version = DoodleDataEditor.getVersion(ddFilePath);
            if (version == -1) {
                // 文件解析错误
                ret[0] = ERROR_CODE_DD_FILE_ERROR;
                return ret;
            } else if (version == 1) {
                // 版本 1
                DoodleData doodleData = DoodleDataEditorV1.readFromFile(ddFilePath);
                if (doodleData == null) {
                    // 版本 1 dd 文件解析失败
                    ret[0] = ERROR_CODE_DD_FILE_ERROR;
                    return ret;
                } else {
                    // 版本 1 dd 文件解析成功
                    ret[0] = ERROR_CODE_DD_FILE_OK;
                    ret[1] = doodleData;
                    return ret;
                }
            } else {
                // 不支持其他版本
                ret[0] = ERROR_CODE_DD_FILE_VERION_UNSUPPORT;
                return ret;
            }
        }
    }

    private static void showLog(@NonNull String log) {
        Toast.makeText(AppContext.getContext(), log, Toast.LENGTH_SHORT).show();
    }

}
