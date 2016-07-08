package com.idonans.doodle.player;

import android.os.SystemClock;

import com.idonans.acommon.lang.Available;
import com.idonans.acommon.lang.CommonLog;
import com.idonans.acommon.lang.Threads;

/**
 * 播放控制器
 * Created by pengji on 16-7-8.
 */
public abstract class PlayController implements Available {

    private final String TAG = getClass().getSimpleName();

    /**
     * 空闲状态
     */
    private static final int STATUS_IDLE = 0;
    /**
     * 资源准备中
     */
    private static final int STATUS_PREPARING = 1;
    /**
     * 资源已经准备好，可以开始播放了.
     */
    private static final int STATUS_PREPARED = 2;
    /**
     * 暂停中
     */
    private static final int STATUS_PAUSED = 3;
    /**
     * 播放中
     */
    private static final int STATUS_PLAYING = 4;
    /**
     * 已经播放完成
     */
    private static final int STATUS_COMPLETE = 5;
    /**
     * 处于错误状态
     */
    private static final int STATUS_ERROR = 6;

    private int mStatus = STATUS_IDLE;

    private final DoodleViewPlayer mPlayer;

    public PlayController(DoodleViewPlayer doodleViewPlayer) {
        this(doodleViewPlayer, STATUS_IDLE);
    }

    private PlayController(DoodleViewPlayer doodleViewPlayer, int status) {
        mPlayer = doodleViewPlayer;
        mStatus = status;
    }

    public boolean isPlaying() {
        return mStatus == STATUS_PLAYING;
    }

    public int getStatus() {
        return mStatus;
    }

    private void printStatusError(int targetStatus) {
        String errorMessage = TAG + " fail to change status, " + mStatus + " -> " + targetStatus;
        CommonLog.e(errorMessage);
        new IllegalStateException(errorMessage).printStackTrace();
    }

    protected final boolean moveStatusToPreparing() {
        if (mStatus != STATUS_IDLE) {
            printStatusError(STATUS_PREPARING);
            return false;
        }
        mStatus = STATUS_PREPARING;
        return true;
    }

    public abstract void prepareing(Runnable runnable);

    protected final boolean moveStatusToPrepared() {
        if (mStatus != STATUS_PREPARING) {
            printStatusError(STATUS_PREPARED);
            return false;
        }
        mStatus = STATUS_PREPARED;
        return true;
    }

    public abstract void prepared(Runnable runnable);

    protected final boolean moveStatusToPlaying() {
        if (mStatus != STATUS_PREPARED
                && mStatus != STATUS_PAUSED) {
            printStatusError(STATUS_PLAYING);
            return false;
        }
        mStatus = STATUS_PLAYING;
        return true;
    }

    public abstract void play(Runnable runnable);

    protected final boolean moveStatusToPaused() {
        if (mStatus != STATUS_PREPARED
                && mStatus != STATUS_PLAYING) {
            printStatusError(STATUS_PAUSED);
            return false;
        }
        mStatus = STATUS_PAUSED;
        return true;
    }

    public abstract void pause(Runnable runnable);

    protected final boolean moveStatusToComplete() {
        if (mStatus != STATUS_PLAYING) {
            printStatusError(STATUS_COMPLETE);
            return false;
        }
        mStatus = STATUS_COMPLETE;
        return true;
    }

    public abstract void complete(Runnable runnable);

    protected final boolean moveStatusToIdle() {
        mStatus = STATUS_IDLE;
        return true;
    }

    public abstract void stop(Runnable runnable);

    protected final boolean moveStatusToError() {
        mStatus = STATUS_ERROR;
        return true;
    }

    public abstract void error(Runnable runnable);

    private PendingRunnable mPendingRunnable;

    /**
     * package, used by DoodleViewPlayer#PlayEngine
     */
    final void enqueuWithDeplay(final Runnable runnable, final long delay) {
        pendingRunWithDelay(runnable, delay);
    }

    protected final void pendingRunWithDelay(final Runnable runnable, final long delay) {
        mPendingRunnable = new PendingRunnable(runnable, delay);
        enqueue(mPendingRunnable);
    }

    private void enqueue(Runnable runnable) {
        mPlayer.getTaskQueue().enqueue(runnable);
    }

    @Override
    public boolean isAvailable() {
        return mPlayer.getPlayController() == this;
    }

    private boolean isPendingRunnableAvailable(PendingRunnable pendingRunnable) {
        return this.isAvailable() && mPendingRunnable == pendingRunnable;
    }

    private class PendingRunnable implements Runnable {

        private final Runnable mTarget;
        private final long mTimeStart;
        private final long mDelay;

        private static final long DELAY_INTERVAL_MAX = 100L; // 100ms

        private PendingRunnable(Runnable target, long delay) {
            mTarget = target;
            mTimeStart = SystemClock.uptimeMillis();
            mDelay = delay;
        }

        @Override
        public void run() {
            if (!isPendingRunnableAvailable(this)) {
                return;
            }

            long timeNow = SystemClock.uptimeMillis();
            long delayRemain = mDelay - (timeNow - mTimeStart);
            if (delayRemain <= 0L) {
                mTarget.run();
                return;
            }

            Threads.sleepQuietly(Math.min(delayRemain, DELAY_INTERVAL_MAX));
            enqueue(this);
        }

    }

    public static class Default extends PlayController {

        public Default(DoodleViewPlayer doodleViewPlayer) {
            super(doodleViewPlayer);
        }

        @Override
        public void prepareing(final Runnable runnable) {
            pendingRunWithDelay(new Runnable() {
                @Override
                public void run() {
                    if (moveStatusToPreparing()) {
                        if (runnable != null) {
                            runnable.run();
                        }
                    }
                }
            }, 0L);
        }

        @Override
        public void prepared(final Runnable runnable) {
            pendingRunWithDelay(new Runnable() {
                @Override
                public void run() {
                    if (moveStatusToPrepared()) {
                        if (runnable != null) {
                            runnable.run();
                        }
                    }
                }
            }, 0L);
        }

        @Override
        public void play(final Runnable runnable) {
            pendingRunWithDelay(new Runnable() {
                @Override
                public void run() {
                    if (moveStatusToPlaying()) {
                        if (runnable != null) {
                            runnable.run();
                        }
                    }
                }
            }, 0L);
        }

        @Override
        public void pause(final Runnable runnable) {
            pendingRunWithDelay(new Runnable() {
                @Override
                public void run() {
                    if (moveStatusToPaused()) {
                        if (runnable != null) {
                            runnable.run();
                        }
                    }
                }
            }, 0L);
        }

        @Override
        public void complete(final Runnable runnable) {
            pendingRunWithDelay(new Runnable() {
                @Override
                public void run() {
                    if (moveStatusToComplete()) {
                        if (runnable != null) {
                            runnable.run();
                        }
                    }
                }
            }, 0L);
        }

        @Override
        public void stop(final Runnable runnable) {
            pendingRunWithDelay(new Runnable() {
                @Override
                public void run() {
                    if (moveStatusToIdle()) {
                        if (runnable != null) {
                            runnable.run();
                        }
                    }
                }
            }, 0L);
        }

        @Override
        public void error(final Runnable runnable) {
            pendingRunWithDelay(new Runnable() {
                @Override
                public void run() {
                    if (moveStatusToError()) {
                        if (runnable != null) {
                            runnable.run();
                        }
                    }
                }
            }, 0L);
        }
    }

}
