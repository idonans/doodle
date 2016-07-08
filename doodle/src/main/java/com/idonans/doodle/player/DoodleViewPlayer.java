package com.idonans.doodle.player;

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
import com.idonans.acommon.util.ViewUtil;
import com.idonans.doodle.DoodleData;
import com.idonans.doodle.DoodleView;
import com.idonans.doodle.R;
import com.idonans.doodle.dd.DoodleDataEditor;
import com.idonans.doodle.dd.v1.DoodleDataEditorV1;

import java.util.ArrayList;
import java.util.Collections;

/**
 * DoodleView 播放器
 * Created by pengji on 16-7-4.
 */
public class DoodleViewPlayer extends FrameLayout {

    private static final String TAG = "DoodleViewPlayer";
    private DoodleView mDoodleView;
    private TaskQueue mTaskQueue;
    private PlayController mPlayController;
    private long mSpeedDelay = 10L;

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

    /**
     * package, used by PlayController
     */
    TaskQueue getTaskQueue() {
        return mTaskQueue;
    }

    /**
     * package, used by PlayController
     */
    PlayController getPlayController() {
        return mPlayController;
    }

    public long getSpeedDelay() {
        return mSpeedDelay;
    }

    /**
     * 设置播放速度延迟(ms)，值越小，播放速度越快
     */
    public void setSpeedDelay(long speedDelay) {
        if (speedDelay < 0) {
            speedDelay = 0;
        }
        mSpeedDelay = speedDelay;
    }

    /**
     * 设置播放资源, dd 文件路径
     */
    public void setDoodleData(String ddFilePath) {
        setDoodleData(ddFilePath, true);
    }

    public void setDoodleData(final String ddFilePath, final boolean ignoreEmptyDrawStep) {
        setDoodleData(ddFilePath, ignoreEmptyDrawStep, true);
    }

    public void setDoodleData(final String ddFilePath, final boolean ignoreEmptyDrawStep, final boolean autoPlay) {
        final PlayController playController = new PlayController.Default(this);
        mPlayController = playController;
        playController.prepareing(new Runnable() {
            @Override
            public void run() {
                Object[] ret = DoodleDataEditorV1Loader.load(ddFilePath, ignoreEmptyDrawStep);

                final int errorCodeDDFile = (int) ret[0];
                if (errorCodeDDFile == ERROR_CODE_DD_FILE_OK) {
                    // dd 文件解析成功
                    DoodleData doodleData = (DoodleData) ret[1];
                    // play doodle data
                    resetDoodleData(doodleData);

                    if (playController.isAvailable()) {
                        mDoodleView.load(doodleData);
                    }

                    playController.prepared(new Runnable() {
                        @Override
                        public void run() {
                            if (autoPlay) {
                                // 准备完成之后自动播放
                                play();
                            }
                        }
                    });
                } else {
                    // 播放资源加载失败
                    playController.error(new Runnable() {
                        @Override
                        public void run() {
                            // 清空 doodle view
                            mDoodleView.setAspectRatio(1, 1);
                            showDDFileErrorMessage(errorCodeDDFile);
                        }
                    });
                }
            }
        });
    }

    public void pause() {
        if (mPlayController != null) {
            mPlayController.pause(new Runnable() {
                @Override
                public void run() {
                    // ignore
                }
            });
        }
    }

    public void play() {
        if (mPlayController != null) {
            startPlayEngine(mPlayController);
        }
    }

    public void seekBy(int seekSize) {
        if (mPlayController != null) {
            startSeekBy(mPlayController, seekSize);
        }
    }

    public void stop() {
        if (mPlayController != null) {
            mPlayController.stop(new Runnable() {
                @Override
                public void run() {
                    // ignore
                }
            });
        }
    }

    public boolean isPlaying() {
        return mPlayController != null && mPlayController.isPlaying();
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
                new RuntimeException("logic error").printStackTrace();
                break;
        }
    }

    private static void resetDoodleData(@NonNull DoodleData doodleData) {
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

        public static Object[] load(final String ddFilePath, final boolean ignoreEmptyDrawStep) {
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
                DoodleData doodleData = DoodleDataEditorV1.readFromFile(ddFilePath, ignoreEmptyDrawStep);
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

    private PlayEngine mPlayEngine;

    private void startPlayEngine(PlayController playController) {
        mPlayEngine = new PlayEngine(playController, this);
        mPlayEngine.start();
    }

    private static class PlayEngine implements Available, Runnable {

        protected final String TAG = getClass().getSimpleName();
        protected final PlayController mPlayController;
        protected final DoodleViewPlayer mPlayer;

        private PlayEngine(PlayController playController, DoodleViewPlayer player) {
            mPlayController = playController;
            mPlayer = player;
        }

        protected void start() {
            if (isAvailable()) {
                mPlayController.pendingRunWithDelay(this, 0L);
            } else {
                mPlayController.play(this);
            }
        }

        @Override
        public void run() {
            if (!isAvailable()) {
                return;
            }

            mPlayer.mDoodleView.isInitOk(new DoodleView.ActionCallback() {
                @Override
                public void onActionResult(boolean success) {
                    if (success) {
                        final int seekSize = getSeekSize();
                        if (seekSize > 0) {
                            // redo
                            mPlayer.mDoodleView.redoByStep(seekSize, new DoodleView.ActionCallback2() {
                                @Override
                                public void onActionResult(boolean success, int value) {
                                    onSizeSeeked(value);
                                    if (success) {
                                        mPlayController.pendingRunWithDelay(PlayEngine.this, getSpeedDelay());
                                    }
                                }
                            });
                        } else if (seekSize < 0) {
                            // undo
                            mPlayer.mDoodleView.undoByStep(-seekSize, new DoodleView.ActionCallback2() {
                                @Override
                                public void onActionResult(boolean success, int value) {
                                    onSizeSeeked(-value);
                                    if (success) {
                                        mPlayController.pendingRunWithDelay(PlayEngine.this, getSpeedDelay());
                                    }
                                }
                            });
                        } else {
                            CommonLog.d(TAG + " no seek size");
                        }
                    } else {
                        // wait for init ok
                        mPlayController.pendingRunWithDelay(PlayEngine.this, 100L);
                    }
                }
            });

        }

        @Override
        public boolean isAvailable() {
            return mPlayer.mPlayEngine == this
                    && mPlayController.isAvailable()
                    && mPlayController.isPlaying();
        }

        /**
         * 如果 <0, 则向左 seek, 如果 >0, 则向右, 否则终止 seek
         */
        protected int getSeekSize() {
            return 1;
        }

        protected void onSizeSeeked(int sizeSeeked) {
            if (sizeSeeked == 0) {
                CommonLog.d(TAG + " onSizeSeeked with 0, try complete");
                // 播放完成
                if (isAvailable()) {
                    mPlayController.complete(new Runnable() {
                        @Override
                        public void run() {
                            CommonLog.d(TAG + " play complete");
                        }
                    });
                }
            }
        }

        protected long getSpeedDelay() {
            return mPlayer.getSpeedDelay();
        }

    }

    private void startSeekBy(PlayController playController, int seekSize) {
        if (seekSize == 0) {
            CommonLog.d(TAG + " seek size is 0, ignore seek by");
            return;
        }
        mPlayEngine = new SeekEngine(playController, this, seekSize);
        mPlayEngine.start();
    }

    private static class SeekEngine extends PlayEngine {

        private int mSeekSize;

        private SeekEngine(PlayController playController, DoodleViewPlayer player, int seekSize) {
            super(playController, player);
            mSeekSize = seekSize;
            if (mSeekSize == 0) {
                throw new IllegalArgumentException("seek size invalid " + mSeekSize);
            }
        }

        @Override
        protected void start() {
            if (isAvailable()) {
                mPlayController.pendingRunWithDelay(this, 0L);
            } else {
                // can not seek
                new IllegalStateException("can not seek").printStackTrace();
            }
        }

        @Override
        public int getSeekSize() {
            return mSeekSize;
        }

        @Override
        protected void onSizeSeeked(int sizeSeeked) {
            CommonLog.d(TAG + " onSizeSeeked mSeekSize:" + mSeekSize + ", sizeSeeked:" + sizeSeeked);

            if (mSeekSize > 0 && sizeSeeked > 0 && mSeekSize >= sizeSeeked) {
                mSeekSize -= sizeSeeked;
            } else if (mSeekSize < 0 && sizeSeeked < 0 && mSeekSize <= sizeSeeked) {
                mSeekSize -= sizeSeeked;
            } else if (sizeSeeked != 0) {
                new IllegalStateException("error size seeked:" + sizeSeeked + ", mSeekSize:" + mSeekSize).printStackTrace();
            }

            if (sizeSeeked == 0 || mSeekSize == 0) {
                // seek 完成，恢复之前的播放状态 (所有期望的 seek 已经完成 或者 当前 player 中没有更多的内容可以 seek)
                if (isAvailable()) {
                    resumePlayStatusAfterSeekFinished();
                }
            }
        }

        private void resumePlayStatusAfterSeekFinished() {
            if (mPlayController.isCompleted()) {
                mPlayController.pause(new Runnable() {
                    @Override
                    public void run() {
                        CommonLog.d(TAG + " pause player from status completed");
                    }
                });
            } else if (mPlayController.isPrepared()) {
                mPlayController.pause(new Runnable() {
                    @Override
                    public void run() {
                        CommonLog.d(TAG + " pause player from status prepared");
                    }
                });
            } else if (mPlayController.isPlaying()) {
                CommonLog.d(TAG + " continue playing after seek finished");
                mPlayer.startPlayEngine(mPlayController);
            }
        }

        @Override
        protected long getSpeedDelay() {
            return 0L;
        }

        @Override
        public boolean isAvailable() {
            // 在播放，暂停，完成 或者 资源已准备好的状态下可以 seek
            return mPlayer.mPlayEngine == this
                    && mPlayController.isAvailable()
                    && (mPlayController.isPlaying() || mPlayController.isPrepared() || mPlayController.isPaused() || mPlayController.isCompleted());
        }

    }

}
