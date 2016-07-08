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
                                start();
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

    public void start() {
        if (mPlayController != null) {
            startPlayEngine(mPlayController);
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
        mPlayEngine = new PlayEngine(playController);
        mPlayEngine.start();
    }

    private class PlayEngine implements Available, Runnable {

        private final PlayController mPlayController;

        private PlayEngine(PlayController playController) {
            mPlayController = playController;
        }

        private void start() {
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

            mDoodleView.isInitOk(new DoodleView.ActionCallback() {
                @Override
                public void onActionResult(boolean success) {
                    if (success) {
                        mDoodleView.redoByStep(1, new DoodleView.ActionCallback2() {
                            @Override
                            public void onActionResult(boolean success, int value) {
                                // ignore
                            }
                        });
                    }

                    mPlayController.pendingRunWithDelay(PlayEngine.this, getSpeedDelay());
                }
            });

        }

        @Override
        public boolean isAvailable() {
            return mPlayEngine == this
                    && mPlayController.isAvailable()
                    && mPlayController.isPlaying();
        }

    }

}
