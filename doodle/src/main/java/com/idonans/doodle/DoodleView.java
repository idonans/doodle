package com.idonans.doodle;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import com.idonans.acommon.lang.Available;
import com.idonans.acommon.lang.CommonLog;
import com.idonans.acommon.lang.TaskQueue;
import com.idonans.acommon.lang.Threads;
import com.idonans.acommon.util.DimenUtil;
import com.idonans.acommon.util.MotionEventUtil;
import com.idonans.acommon.util.ViewUtil;
import com.idonans.doodle.brush.Brush;
import com.idonans.doodle.brush.Empty;
import com.idonans.doodle.drawstep.DrawStep;
import com.idonans.doodle.drawstep.EmptyDrawStep;

import java.util.ArrayList;


/**
 * 涂鸦板
 * Created by idonans on 16-5-10.
 */
public class DoodleView extends FrameLayout {

    public DoodleView(Context context) {
        super(context);
        init();
    }

    public DoodleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DoodleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DoodleView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private static final String TAG = "DoodleView";

    @NonNull
    private View mLoadingView;

    @NonNull
    private Render mRender;

    @NonNull
    private TextureView mTextureView;

    @NonNull
    private Brush mBrush;

    private void init() {
        if (isInEditMode()) {
            return;
        }

        Context context = getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        layoutInflater.inflate(R.layout.doodle_layout, this, true);

        mLoadingView = ViewUtil.findViewByID(this, R.id.doodle_loading);

        mRender = new Render(getContext());

        mTextureView = ViewUtil.findViewByID(this, R.id.doodle_texture);
        mTextureView.setSurfaceTextureListener(new TextureListener());
        mBrush = new Empty();
    }

    /**
     * 设置画布的背景色
     */
    public void setCanvasBackgroundColor(int color) {
        setBackgroundColor(color);
        mRender.setCanvasBackgroundColor(color);
    }

    /**
     * 判断当前是否处于 loading 状态，例如当 doodle 初始化或者恢复数据时，会处于 loading 状态.
     */
    protected boolean isLoadingShown() {
        return mLoadingView.getVisibility() == View.VISIBLE;
    }

    /**
     * 显示 loading 视图，当 loading 视图显示时，所有的 touch 操作都会被忽略.
     */
    protected void showLoading() {
        mLoadingView.setVisibility(View.VISIBLE);
    }

    protected void postShowLoading() {
        Threads.runOnUi(new Runnable() {
            @Override
            public void run() {
                showLoading();
            }
        });
    }

    /**
     * 隐藏 loading 视图.
     */
    protected void hideLoading() {
        mLoadingView.setVisibility(View.GONE);
    }

    protected void postHideLoading() {
        Threads.runOnUi(new Runnable() {
            @Override
            public void run() {
                hideLoading();
            }
        });
    }

    /**
     * 设置画刷
     */
    public void setBrush(@NonNull Brush brush) {
        mBrush = brush;
    }

    /**
     * 获得当前的画刷, 如果要更改画刷属性，需要新建一个画刷并设置
     */
    @NonNull
    public Brush getBrush() {
        return mBrush;
    }

    /**
     * 设置画布的宽高比， 此方法会删除之前的所有绘画内容
     */
    public void setAspectRatio(int width, int height) {
        //////
        postShowLoading();
        mRender.setAspectRatio(width, height);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        if (action == MotionEvent.ACTION_DOWN) {
            requestDisallowInterceptTouchEvent(true);
        }

        if (!isLoadingShown()) {
            mRender.onTouchEvent(event);
        }
        return true;
    }

    /**
     * 根据指定宽高比将 width, height 调整为最佳尺寸 (不超出范围并且宽高比完美匹配)
     */
    private static int[] calculatePerfectSizeWithAspect(int width, int height, int aspectWidth, int aspectHeight) {
        // 先按照整宽计算
        int targetWidth = width;
        int targetHeight = Float.valueOf(1f * targetWidth * aspectHeight / aspectWidth).intValue();
        if (targetHeight <= height) {
            // 调整，使得宽高比完美匹配
            targetWidth -= targetWidth % aspectWidth;
            targetHeight = targetWidth * aspectHeight / aspectWidth;
            return new int[]{targetWidth, targetHeight};
        }

        // 按照整高计算
        targetHeight = height;
        targetWidth = Float.valueOf(1f * targetHeight * aspectWidth / aspectHeight).intValue();
        if (targetWidth > width) {
            throw new RuntimeException("measure error");
        }

        // 调整，使得宽高比完美匹配
        targetHeight -= targetHeight % aspectHeight;
        targetWidth = targetHeight * aspectWidth / aspectHeight;
        return new int[]{targetWidth, targetHeight};
    }

    private class TextureListener implements TextureView.SurfaceTextureListener {

        private final String TAG = "DoodleView$TextureListener";

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            CommonLog.d(TAG + " onSurfaceTextureAvailable width:" + width + ", height:" + height);
            mRender.init(width, height);
            mRender.setTextureEnable(true);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            CommonLog.d(TAG + " onSurfaceTextureSizeChanged width:" + width + ", height:" + height);
            mRender.init(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            CommonLog.d(TAG + " onSurfaceTextureDestroyed");
            mRender.setTextureEnable(false);
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            CommonLog.d(TAG + " onSurfaceTextureUpdated");
        }
    }

    private DoodleBufferChangedListener mDoodleBufferChangedListener;

    public interface DoodleBufferChangedListener {
        void onDoodleBufferChanged(boolean canUndo, boolean canRedo);
    }

    /**
     * 监听涂鸦板中回退和恢复的状态变化. if null, clear last listener. 在 ui 线程中回调.
     */
    public void setDoodleBufferChangedListener(@Nullable final DoodleBufferChangedListener doodleBufferChangedListener) {
        if (doodleBufferChangedListener == null) {
            // clear last listener
            mDoodleBufferChangedListener = null;
            return;
        }

        mDoodleBufferChangedListener = new DoodleBufferChangedListener() {
            @Override
            public void onDoodleBufferChanged(final boolean canUndo, final boolean canRedo) {
                Threads.runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        doodleBufferChangedListener.onDoodleBufferChanged(canUndo, canRedo);
                    }
                });
            }
        };
    }

    /**
     * 指定操作的结果回调， 如回退和恢复操作
     */
    public interface ActionCallback {
        void onActionResult(boolean success);
    }

    /**
     * 是否可以回退. 在 ui 线程中回调.
     */
    public void canUndo(final ActionCallback callback) {
        mRender.canUndo(new ActionCallback() {
            @Override
            public void onActionResult(final boolean success) {
                Threads.runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        callback.onActionResult(success);
                    }
                });
            }
        });
    }

    /**
     * 回退
     *
     * @see #setDoodleBufferChangedListener(DoodleBufferChangedListener)
     */
    public void undo() {
        mRender.undo(new ActionCallback() {
            @Override
            public void onActionResult(final boolean success) {
                if (success) {
                    mRender.postInvalidate();
                }
            }
        });
    }

    /**
     * 载入数据, 当前画板的 canvas 可能还没有初始化完成
     */
    public void load(DoodleData doodleData) {
        if (doodleData == null) {
            return;
        }
        postShowLoading();
        mRender.load(doodleData);
    }

    /**
     * 是否可以前进, undo 之后的反向恢复. 在 ui 线程中回调.
     */
    public void canRedo(final ActionCallback callback) {
        mRender.canRedo(new ActionCallback() {
            @Override
            public void onActionResult(final boolean success) {
                Threads.runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        callback.onActionResult(success);
                    }
                });
            }
        });
    }

    /**
     * 反向恢复
     *
     * @see #setDoodleBufferChangedListener(DoodleBufferChangedListener)
     */
    public void redo() {
        mRender.redo(new ActionCallback() {
            @Override
            public void onActionResult(final boolean success) {
                if (success) {
                    mRender.postInvalidate();
                }
            }
        });
    }

    public interface SaveDataActionCallback {
        void onDataSaved(@Nullable DoodleData doodleData);
    }

    public void save(final SaveDataActionCallback callback) {
        if (callback == null) {
            return;
        }

        postShowLoading();
        mRender.save(new SaveDataActionCallback() {
            @Override
            public void onDataSaved(@Nullable final DoodleData doodleData) {
                Threads.runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        callback.onDataSaved(doodleData);
                        postHideLoading();
                    }
                });
            }
        });
    }

    private class Render implements Available {

        private static final String TAG = "DoodleView$Render";

        // 画布的宽高比, 恢复数据时不受该比例影响, 只是在新建画布时，用来辅助计算 canvas 的尺寸
        private int mAspectWidth = 1;
        private int mAspectHeight = 1;

        private volatile CanvasBuffer mCanvasBuffer;

        private boolean mTextureEnable;

        private int mCanvasBackgroundColor = Color.BLACK;

        /**
         * 所有与画布数据相关的操作都使用该队列处理， 如绘画手势（缩放和移动手势除外），刷新，undo, redo, 数据保存与恢复等。
         */
        private final TaskQueue mTaskQueue = new TaskQueue(1);

        private DoodleData mPendingDoodleData;

        private final TwoPointScaleGestureDetector mCanvasScaleGestureDetector;
        private final GestureDetectorCompat mCanvasTranslationGestureDetectorCompat;
        private final GestureDetectorCompat mTextureActionGestureDetectorCompat;

        private Render(Context context) {
            mCanvasScaleGestureDetector = new TwoPointScaleGestureDetector(context, new CanvasScaleGestureListener());
            mCanvasTranslationGestureDetectorCompat = new GestureDetectorCompat(context, new CanvasTranslationGestureListener());
            mCanvasTranslationGestureDetectorCompat.setIsLongpressEnabled(false);
            mTextureActionGestureDetectorCompat = new GestureDetectorCompat(context, new TextureActionGestureListener());
            mTextureActionGestureDetectorCompat.setIsLongpressEnabled(false);
        }

        /**
         * 设置画布的背景色
         */
        public void setCanvasBackgroundColor(int color) {
            mCanvasBackgroundColor = color;
            resumeDoodle();
        }

        public int getCanvasBackgroundColor() {
            return mCanvasBackgroundColor;
        }

        private void enqueue(Runnable runnable) {
            this.mTaskQueue.enqueue(runnable);
        }

        /**
         * 载入数据, 当前画板的 canvas 可能还没有初始化完成
         */
        private void load(@NonNull final DoodleData doodleData) {
            enqueue(new Runnable() {
                @Override
                public void run() {
                    if (mCanvasBuffer == null) {
                        // 画布还没有准备好，延迟恢复
                        mPendingDoodleData = doodleData;
                        return;
                    }

                    // 恢复数据
                    CanvasBuffer canvasBufferOld = mCanvasBuffer;
                    mCanvasBuffer = null;
                    mPendingDoodleData = null;

                    mCanvasBuffer = createCanvasBuffer(
                            canvasBufferOld.mTextureWidth, canvasBufferOld.mTextureHeight,
                            doodleData);
                    Threads.runOnUi(new Runnable() {
                        @Override
                        public void run() {
                            DoodleView.this.setCanvasBackgroundColor(doodleData.backgroundColor);
                        }
                    });
                }
            });
        }

        private void save(@NonNull final SaveDataActionCallback callback) {
            enqueue(new Runnable() {
                @Override
                public void run() {
                    if (mCanvasBuffer == null) {
                        // 画布还没有准备好
                        callback.onDataSaved(null);
                        return;
                    }

                    if (!mCanvasBuffer.hasAnyNoneEmptyDrawStep()) {
                        // 没有有效的绘画步骤，这是一张空白
                        callback.onDataSaved(null);
                        return;
                    }

                    DoodleData doodleData = new DoodleData();
                    doodleData.setBackgroundColor(getCanvasBackgroundColor());
                    doodleData.setSize(mCanvasBuffer.mBitmapWidth, mCanvasBuffer.mBitmapHeight);
                    doodleData.setDrawSteps(mCanvasBuffer.mDrawSteps);
                    doodleData.setDrawStepsRedo(mCanvasBuffer.mDrawStepsRedo);
                    callback.onDataSaved(doodleData);
                }
            });
        }

        private CanvasBuffer createCanvasBuffer(int textureWidth, int textureHeight, @NonNull DoodleData doodleData) {
            CanvasBuffer canvasBuffer = new CanvasBuffer(
                    textureWidth, textureHeight,
                    doodleData.width, doodleData.height);
            if (doodleData.drawStepDatas != null) {
                for (DoodleData.DrawStepData drawStepData : doodleData.drawStepDatas) {
                    canvasBuffer.mDrawSteps.add(drawStepData.create());
                }
            }
            if (doodleData.drawStepDatasRedo != null) {
                for (DoodleData.DrawStepData drawStepData : doodleData.drawStepDatasRedo) {
                    canvasBuffer.mDrawStepsRedo.add(drawStepData.create());
                }
            }

            ///////
            postHideLoading();

            return canvasBuffer;
        }

        private CanvasBuffer createCanvasBuffer(int textureWidth, int textureHeight) {
            int[] canvasBufferSize = calculatePerfectSizeWithAspect(textureWidth, textureHeight, mAspectWidth, mAspectHeight);
            CommonLog.d(new StringBuilder()
                    .append(TAG)
                    .append(" create canvas buffer")
                    .append(", texture size [" + textureWidth + ", " + textureHeight + "]")
                    .append(", current aspect [" + mAspectWidth + ", " + mAspectHeight + "]")
                    .append(", canvas buffer size [" + canvasBufferSize[0] + ", " + canvasBufferSize[1] + "]"));

            CanvasBuffer canvasBuffer = new CanvasBuffer(textureWidth, textureHeight, canvasBufferSize[0], canvasBufferSize[1]);

            ///////
            postHideLoading();

            return canvasBuffer;
        }

        private class TwoPointScaleGestureDetector extends ScaleGestureDetector {

            private boolean mDownStart;

            public TwoPointScaleGestureDetector(Context context, OnScaleGestureListener listener) {
                super(context, listener);
            }

            @Override
            public boolean onTouchEvent(MotionEvent event) {
                final int action = MotionEventCompat.getActionMasked(event);
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        mDownStart = false;
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        mDownStart = true;
                        break;
                    default:
                        break;
                }

                return super.onTouchEvent(event);
            }

        }

        private void setAspectRatio(final int aspectWidth, final int aspectHeight) {
            enqueue(new Runnable() {
                @Override
                public void run() {
                    mAspectWidth = aspectWidth;
                    mAspectHeight = aspectHeight;

                    if (mCanvasBuffer == null) {
                        // 画布还没有准备好
                        return;
                    }

                    // 使用当前的 texture 重新构建 canvas buffer
                    CanvasBuffer canvasBufferOld = mCanvasBuffer;
                    mCanvasBuffer = null;
                    mCanvasBuffer = createCanvasBuffer(canvasBufferOld.mTextureWidth, canvasBufferOld.mTextureHeight);
                    resumeDoodle();
                }
            });
        }

        private void init(final int textureWidth, final int textureHeight) {
            enqueue(new Runnable() {
                @Override
                public void run() {
                    if (mPendingDoodleData != null) {
                        DoodleData doodleData = mPendingDoodleData;

                        mCanvasBuffer = null;
                        mPendingDoodleData = null;

                        mCanvasBuffer = createCanvasBuffer(textureWidth, textureHeight, doodleData);
                        return;
                    }

                    if (mCanvasBuffer != null) {
                        CommonLog.d(TAG + " canvas buffer found " + mCanvasBuffer.toShortString()
                                + ", current aspect [" + mAspectWidth + ", " + mAspectHeight + "], [" + textureWidth + ", " + textureHeight + "]");
                        if (mCanvasBuffer.mTextureWidth != textureWidth || mCanvasBuffer.mTextureHeight != textureHeight) {
                            CommonLog.e(TAG + " current canvas buffer texture size not match");
                        }
                        return;
                    }

                    mCanvasBuffer = createCanvasBuffer(textureWidth, textureHeight);
                }
            });
        }

        public void setTextureEnable(boolean textureEnable) {
            mTextureEnable = textureEnable;
            resumeDoodle();
        }

        private void resumeDoodle() {
            enqueue(new Runnable() {
                @Override
                public void run() {
                    CanvasBuffer canvasBuffer = mCanvasBuffer;
                    if (isAvailable()) {
                        canvasBuffer.postInvalidate();
                        postInvalidate();
                        canvasBuffer.notifyUndoRedoChanged();
                    }
                }
            });
        }

        @Override
        public boolean isAvailable() {
            return mTextureEnable && mCanvasBuffer != null;
        }

        private class CanvasScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

            private float mPx;
            private float mPy;

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                CanvasBuffer canvasBuffer = mCanvasBuffer;
                if (!isAvailable()) {
                    return false;
                }

                Matrix matrix = canvasBuffer.getMatrix();
                float[] values = new float[9];
                matrix.getValues(values);
                float scaleFactor = detector.getScaleFactor();

                if (scaleFactor > 1f) {
                    // 如果还可以放大，再放大
                    if (values[Matrix.MSCALE_X] < canvasBuffer.mMaxScale) {
                        matrix.postScale(scaleFactor, scaleFactor, mPx, mPy);
                    }
                } else if (scaleFactor < 1f) {
                    // 如果还可以缩小，再缩小
                    if (values[Matrix.MSCALE_X] > canvasBuffer.mMinScale) {
                        matrix.postScale(scaleFactor, scaleFactor, mPx, mPy);
                    }
                }

                canvasBuffer.setMatrix(matrix);
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                CanvasBuffer canvasBuffer = mCanvasBuffer;
                if (!isAvailable()) {
                    return false;
                }

                if (!mCanvasScaleGestureDetector.mDownStart) {
                    return false;
                }

                // 缩放的变化比例使用外层 touch 点计算，但是缩放的锚点需要映射到 texture 的内容上
                long eventTime = detector.getEventTime();
                MotionEvent motionEvent = MotionEvent.obtain(eventTime, eventTime, MotionEvent.ACTION_DOWN, detector.getFocusX(), detector.getFocusY(), 0);
                motionEvent.transform(canvasBuffer.getMatrixInvert());
                float x = motionEvent.getX();
                float y = motionEvent.getY();

                mPx = x;
                mPy = y;
                return true;
            }

        }

        private class CanvasTranslationGestureListener implements GestureDetector.OnGestureListener {

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public void onShowPress(MotionEvent e) {
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                CanvasBuffer canvasBuffer = mCanvasBuffer;
                if (!isAvailable()) {
                    return false;
                }

                if (e2.getPointerCount() <= 1) {
                    return false;
                }

                // 多指移动画布
                // 移动的距离仍然使用外层 touch 点计算，使得移动画布效果明显。
                Matrix matrix = canvasBuffer.getMatrix();
                matrix.postTranslate(-distanceX, -distanceY);
                canvasBuffer.setMatrix(matrix);
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return false;
            }

        }

        private class TextureActionGestureListener implements GestureDetector.OnGestureListener {
            private static final String TAG = "Render$TextureActionGestureListener";
            private boolean mDownStart = false;

            @Override
            public boolean onDown(MotionEvent e) {
                if (!isAvailable()) {
                    return false;
                }

                mDownStart = true;

                enqueueGestureAction(new CancelGestureAction());
                return true;
            }

            @Override
            public void onShowPress(MotionEvent e) {
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (!mDownStart) {
                    return false;
                }

                CanvasBuffer canvasBuffer = mCanvasBuffer;
                if (!isAvailable()) {
                    mDownStart = false;
                    return false;
                }

                MotionEvent event = MotionEvent.obtain(e);
                event.transform(canvasBuffer.getMatrixInvert());

                CommonLog.d(TAG + " onSingleTapUp [" + e.getX() + ", " + e.getY() + "] -> [" + event.getX() + ", " + event.getY() + "]");

                enqueueGestureAction(new SinglePointGestureAction(event));
                enqueueGestureAction(new CancelGestureAction());

                mDownStart = false;
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (!mDownStart) {
                    return false;
                }

                CanvasBuffer canvasBuffer = mCanvasBuffer;
                if (!isAvailable()) {
                    mDownStart = false;
                    return false;
                }

                if (e2.getPointerCount() > 1) {
                    enqueueGestureAction(new CancelGestureAction());
                    mDownStart = false;
                    return false;
                }

                // 单指移动
                Matrix matrixInverse = canvasBuffer.getMatrixInvert();
                MotionEvent downEvent = MotionEvent.obtain(e1);
                MotionEvent currentEvent = MotionEvent.obtain(e2);

                // down event 在此处变换时可能对应的点已经有偏差，需要确保在 down 到目前位置画布没有缩放或者移动
                downEvent.transform(matrixInverse);

                currentEvent.transform(matrixInverse);
                enqueueGestureAction(new ScrollGestureAction(downEvent, currentEvent));

                // 在快速滑动时，需要避排队过多的手势事件，适当让事件等待绘画渲染完成，在下一次 scroll 中，可以从 history 中找回期间触发过的所有点。
                Threads.sleepQuietly(Math.max(5L, canvasBuffer.getLastDrawingTime()));

                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return false;
            }
        }

        private class Draw implements Runnable {

            private static final String TAG = "Render$Draw";

            @Override
            public void run() {
                CanvasBuffer canvasBuffer = mCanvasBuffer;
                if (!isAvailable()) {
                    CommonLog.d(TAG + " available is false, ignore");
                    return;
                }

                Canvas canvas = null;
                try {
                    canvas = mTextureView.lockCanvas();
                    if (canvas == null) {
                        CommonLog.d(TAG + " canvas is null, ignore");
                        return;
                    }

                    // 清空背景
                    canvas.drawColor(getCanvasBackgroundColor());

                    // 将缓冲区中的内容绘画到 canvas 上
                    long timeStart = System.currentTimeMillis();
                    canvasBuffer.draw(canvas);
                    long lastDrawingTime = System.currentTimeMillis() - timeStart;
                    canvasBuffer.setLastDrawingTime(lastDrawingTime);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (canvas != null) {
                        mTextureView.unlockCanvasAndPost(canvas);
                    }
                }
            }

        }

        public void postInvalidate() {
            this.enqueue(new Draw());
        }

        public void enqueueGestureAction(final GestureAction gestureAction) {
            this.enqueue(new Runnable() {
                @Override
                public void run() {
                    CanvasBuffer canvasBuffer = mCanvasBuffer;
                    if (!isAvailable()) {
                        CommonLog.d(TAG + " available is false, ignore GestureAction " + gestureAction);
                        return;
                    }
                    boolean changed = canvasBuffer.dispatchGestureAction(gestureAction);
                    if (changed) {
                        canvasBuffer.notifyUndoRedoChanged();
                    }
                }
            });
            postInvalidate();
        }

        /**
         * 是否可以回退
         */
        public void canUndo(final ActionCallback callback) {
            this.enqueue(new Runnable() {
                @Override
                public void run() {
                    CanvasBuffer canvasBuffer = mCanvasBuffer;
                    if (!isAvailable()) {
                        CommonLog.d(TAG + " available is false, ignore canUndo");
                        return;
                    }

                    callback.onActionResult(canvasBuffer.canUndo());
                }
            });
        }

        /**
         * 回退操作，回退成功，返回 true, 否则返回 false.
         */
        public void undo(final ActionCallback callback) {
            this.enqueue(new Runnable() {
                @Override
                public void run() {
                    CanvasBuffer canvasBuffer = mCanvasBuffer;
                    if (!isAvailable()) {
                        CommonLog.d(TAG + " available is false, ignore undo");
                        return;
                    }

                    callback.onActionResult(canvasBuffer.undo());
                }
            });
        }

        /**
         * 是否可以前进, undo 之后的反向恢复
         */
        public void canRedo(final ActionCallback callback) {
            this.enqueue(new Runnable() {
                @Override
                public void run() {
                    CanvasBuffer canvasBuffer = mCanvasBuffer;
                    if (!isAvailable()) {
                        CommonLog.d(TAG + " available is false, ignore canRedo");
                        return;
                    }

                    callback.onActionResult(canvasBuffer.canRedo());
                }
            });
        }

        /**
         * 反向恢复，恢复成功，返回 true, 否则返回 false.
         */
        public void redo(final ActionCallback callback) {
            this.enqueue(new Runnable() {
                @Override
                public void run() {
                    CanvasBuffer canvasBuffer = mCanvasBuffer;
                    if (!isAvailable()) {
                        CommonLog.d(TAG + " available is false, ignore redo");
                        return;
                    }

                    callback.onActionResult(canvasBuffer.redo());
                }
            });
        }

        private class CanvasBuffer {

            private static final String TAG = "Render$CanvasBuffer";
            private static final int FRAMES_SIZE_MAX = 4;
            // 关键帧之间至多间隔的绘画步骤数量
            private static final int FRAMES_STEP_INTERVAL_MAX = 8;
            // 关键帧缓存图像
            private final ArrayList<FrameDrawStep> mFrames = new ArrayList<>(FRAMES_SIZE_MAX);
            // 绘画步骤末尾可能至多存在一个 EmptyDrawStep, 用来标记上一个绘画步骤结束
            private final ArrayList<DrawStep> mDrawSteps = new ArrayList<>();
            private final ArrayList<DrawStep> mDrawStepsRedo = new ArrayList<>();

            private final float mMaxScale;
            private final float mMinScale;
            private final float mBestScale;

            private final int mTextureWidth;
            private final int mTextureHeight;

            private final Bitmap mBitmap; // 当前画布图像(绘画缓冲区)
            private final int mBitmapWidth; // 当前画布图像宽度
            private final int mBitmapHeight; // 当前画布图像高度
            private final Canvas mBitmapCanvas; // 原始画布

            private final Matrix mMatrixTmp;
            private final Matrix mMatrixInvertTmp;

            private long mLastDrawingTime;

            public CanvasBuffer(int textureWidth, int textureHeight, int canvasWidth, int canvasHeight) {
                mTextureWidth = textureWidth;
                mTextureHeight = textureHeight;

                mBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
                mBitmapWidth = mBitmap.getWidth();
                mBitmapHeight = mBitmap.getHeight();
                mBitmapCanvas = new Canvas(mBitmap);

                mMatrixTmp = new Matrix();
                mMatrixInvertTmp = new Matrix();

                final int dp10 = DimenUtil.dp2px(10);

                // 计算缩放范围
                float bestScaleX = Math.max(dp10, mTextureWidth - dp10) * 1f / mBitmapWidth;
                float bestScaleY = Math.max(dp10, mTextureHeight - dp10) * 1f / mBitmapHeight;
                float bestScale = Math.min(bestScaleX, bestScaleY);

                // 如果 bitmap 比 texture 小，则默认比例是 1， 按照原图尺寸显示
                if (bestScale > 1) {
                    bestScale = 1;
                }

                mBestScale = bestScale;
                mMaxScale = Math.max(1, mBestScale * 2.75f);
                mMinScale = mBestScale * 0.75f;

                Threads.runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        // 调整图像位置，使得 bitmap 居中
                        // 当前画布可能不在左上角，需要动态计算
                        Matrix matrix = getMatrix();
                        // 先计算缩放
                        matrix.setScale(mBestScale, mBestScale, 0, 0);
                        matrix.postTranslate((mTextureWidth - mBitmapWidth * mBestScale) / 2f,
                                (mTextureHeight - mBitmapHeight * mBestScale) / 2f);
                        setMatrix(matrix);
                    }
                });
            }

            public String toShortString() {
                return new StringBuilder()
                        .append("CanvasBuffer bitmap size [" + mBitmapWidth + ", " + mBitmapHeight + "]")
                        .append(", texture size [" + mTextureWidth + ", " + mTextureHeight + "]")
                        .toString();
            }

            /**
             * 判断在绘画步骤末尾是否存在一个 EmptyDrawStep
             */
            private boolean hasEmptyDrawStepOnEnd() {
                int size = mDrawSteps.size();
                if (size <= 0) {
                    return false;
                }
                return mDrawSteps.get(size - 1) instanceof EmptyDrawStep;
            }

            /**
             * 是否有有效的绘画步骤 (不包含 redo 中的步骤)
             */
            public boolean hasAnyNoneEmptyDrawStep() {
                int size = mDrawSteps.size();
                if (size <= 0) {
                    return false;
                }
                if (size == 1) {
                    // 如果只有一个绘画步骤，需要校验该步骤是否是一个空步骤
                    return !(mDrawSteps.get(0) instanceof EmptyDrawStep);
                }
                return true;
            }

            /**
             * redo steps changed return true
             */
            private boolean clearRedo() {
                if (mDrawStepsRedo.size() > 0) {
                    mDrawStepsRedo.clear();
                    return true;
                }
                return false;
            }

            private void notifyUndoRedoChanged() {
                if (mDoodleBufferChangedListener != null) {
                    mDoodleBufferChangedListener.onDoodleBufferChanged(canUndo(), canRedo());
                }
            }

            /**
             * 小心线程. 是否可以回退
             */
            public boolean canUndo() {
                // 如果当前绘画步骤中只有一个 EmptyDrawStep, 则不能回退
                if (hasEmptyDrawStepOnEnd()) {
                    return mDrawSteps.size() > 1;
                }
                return mDrawSteps.size() > 0;
            }

            /**
             * 小心线程. 回退操作，回退成功，返回 true, 否则返回 false.
             */
            public boolean undo() {
                boolean canUndo = canUndo();
                if (!canUndo) {
                    return false;
                }

                DrawStep lastDrawStep;
                int size = mDrawSteps.size();
                // 如果当前绘画步骤末尾是 EmptyDrawStep, 则回退时跳过末尾，回退末尾的前一步
                // 此处不必考虑当前绘画步骤中只有一个 EmptyDrawStep 的情况，前面的 canUndo 判断中已经过滤了此种情形。
                if (hasEmptyDrawStepOnEnd()) {
                    lastDrawStep = mDrawSteps.remove(size - 2);
                } else {
                    lastDrawStep = mDrawSteps.remove(size - 1);
                }

                mDrawStepsRedo.add(lastDrawStep);

                // 如果最后一个关键帧在绘画步骤之外，则删除之(如果最后一个关键帧此时对应的刚好是最后一个绘画步骤，也需要删除之)
                int frameSize = mFrames.size();
                if (frameSize > 0) {
                    FrameDrawStep lastFrame = mFrames.get(frameSize - 1);
                    if (lastFrame.mDrawStepIndex >= size - 2) {
                        mFrames.remove(frameSize - 1);
                    }
                }

                notifyUndoRedoChanged();
                return true;
            }

            /**
             * 小心线程. 是否可以前进, undo 之后的反向恢复
             */
            public boolean canRedo() {
                return mDrawStepsRedo.size() > 0;
            }

            /**
             * 小心线程. 反向恢复，恢复成功，返回 true, 否则返回 false.
             */
            public boolean redo() {
                int size = mDrawStepsRedo.size();
                if (size <= 0) {
                    return false;
                }

                DrawStep lastDrawStep = mDrawStepsRedo.remove(size - 1);

                // 如果当前绘画步骤末尾是 EmptyDrawStep, 则将恢复的绘画步骤插入到末尾之前。
                if (hasEmptyDrawStepOnEnd()) {
                    mDrawSteps.add(mDrawSteps.size() - 1, lastDrawStep);
                } else {
                    mDrawSteps.add(lastDrawStep);
                }

                notifyUndoRedoChanged();
                return true;
            }

            public void setMatrix(final Matrix matrix) {
                Threads.runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        setMatrixInternal(matrix);
                    }
                });
            }

            public void setMatrixInternal(Matrix matrix) {
                CommonLog.d(TAG + " set matrix internal");
                // 需要约束变换范围, 先处理 scale, 再处理 translation
                float[] values = new float[9];

                matrix.getValues(values);
                float scale = values[Matrix.MSCALE_X];
                boolean changed = false;
                if (scale > mMaxScale) {
                    float scaleFactorAdjust = mMaxScale / scale;
                    matrix.postScale(scaleFactorAdjust, scaleFactorAdjust, values[Matrix.MTRANS_X], values[Matrix.MTRANS_Y]);
                    changed = true;
                } else if (scale < mMinScale) {
                    float scaleFactorAdjust = mMinScale / scale;
                    matrix.postScale(scaleFactorAdjust, scaleFactorAdjust, values[Matrix.MTRANS_X], values[Matrix.MTRANS_Y]);
                    changed = true;
                }

                // 如果 scale 被调整，则重新获取
                if (changed) {
                    matrix.getValues(values);
                    scale = values[Matrix.MSCALE_X];
                }
                float translationX = values[Matrix.MTRANS_X];
                float translationY = values[Matrix.MTRANS_Y];
                float translationXMax = mTextureWidth / 2f;
                float translationYMax = mTextureHeight / 2f;
                float translationXMin = translationXMax - mBitmapWidth * scale;
                float translationYMin = translationYMax - mBitmapHeight * scale;

                float translationXAdjust = 0;
                float translationYAdjust = 0;
                if (translationX > translationXMax) {
                    translationXAdjust = translationXMax - translationX;
                } else if (translationX < translationXMin) {
                    translationXAdjust = translationXMin - translationX;
                }
                if (translationY > translationYMax) {
                    translationYAdjust = translationYMax - translationY;
                } else if (translationY < translationYMin) {
                    translationYAdjust = translationYMin - translationY;
                }
                if (translationXAdjust != 0 || translationYAdjust != 0) {
                    // 移动范围越界，需要调整
                    matrix.postTranslate(translationXAdjust, translationYAdjust);
                }

                debugMatrix(matrix);

                mTextureView.setTransform(matrix);
                mTextureView.postInvalidate();
                DoodleView.this.postInvalidate();
            }

            private void debugMatrix(Matrix matrix) {
                float[] values = new float[9];
                matrix.getValues(values);
                StringBuilder buffer = new StringBuilder();
                buffer.append("translate x, y -> ").append(values[Matrix.MTRANS_X]).append(", ").append(values[Matrix.MTRANS_Y]).append("\n");
                buffer.append("scale x, y -> ").append(values[Matrix.MSCALE_X]).append(", ").append(values[Matrix.MSCALE_Y]).append("\n");
                CommonLog.d(TAG + " " + buffer);
            }

            public void postInvalidate() {
                Threads.runOnUi(new Runnable() {
                    @Override
                    public void run() {
                        Matrix matrix = getMatrix();
                        setMatrix(matrix);
                    }
                });
            }

            public void setLastDrawingTime(long lastDrawingTime) {
                mLastDrawingTime = lastDrawingTime;
            }

            public long getLastDrawingTime() {
                return mLastDrawingTime;
            }

            public Matrix getMatrix() {
                mMatrixTmp.reset();
                mTextureView.getTransform(mMatrixTmp);
                return mMatrixTmp;
            }

            public Matrix getMatrixInvert() {
                mMatrixInvertTmp.reset();
                getMatrix().invert(mMatrixInvertTmp);
                return mMatrixInvertTmp;
            }

            public void draw(Canvas canvas) {
                CommonLog.d(TAG + " draw");
                if (!restoreBuffer()) {
                    refreshBuffer();
                }
                canvas.drawBitmap(mBitmap, 0f, 0f, null);
            }

            /**
             * 恢复绘制缓冲区, 如果是恢复逻辑返回 true, 否则返回 false.
             */
            private boolean restoreBuffer() {
                // 该涂鸦板从回收状态恢复或者从草稿中恢复的处理逻辑, 或者当用户 undo 到所有的关键帧都消耗完时。

                final int framesSize = mFrames.size();
                final int drawStepSize = mDrawSteps.size();

                if (framesSize > 0) {
                    // 存在历史关键帧，不是恢复逻辑
                    return false;
                }

                if (drawStepSize < FRAMES_STEP_INTERVAL_MAX || drawStepSize < 2) {
                    // 如果当前没有历史关键帧，并且绘画步骤数量不足构建一个关键帧的长度，则认为不是恢复逻辑
                    return false;
                }

                CommonLog.d(TAG + " restore frame");

                // 绘画所有步骤，并且恢复所有关键帧
                // 清空背景
                mBitmapCanvas.drawColor(Color.WHITE);

                // 参与关键帧绘画的步骤数量, 最后的这些步骤需要处理关键帧的绘制和保存
                final int stepSizeInFrames = Math.min(drawStepSize, FRAMES_SIZE_MAX * FRAMES_STEP_INTERVAL_MAX);

                // 关键帧中第一帧的 index (总是不小于0)
                final int firstStepIndexInFrames = drawStepSize - stepSizeInFrames;
                // 绘制第一个关键帧之前的所有步骤
                for (int i = 0; i < firstStepIndexInFrames; i++) {
                    mDrawSteps.get(i).onDraw(mBitmapCanvas);
                }
                // 绘制除最后一个绘画步骤外余下的步骤，并且按照间隔存储关键帧
                for (int i = firstStepIndexInFrames; i < drawStepSize - 1; i++) {
                    mDrawSteps.get(i).onDraw(mBitmapCanvas);
                    // 将当前的图像存储为一个关键帧
                    Bitmap bitmap = Bitmap.createBitmap(mBitmapWidth, mBitmapHeight, Bitmap.Config.ARGB_8888);
                    new Canvas(bitmap).drawBitmap(mBitmap, 0f, 0f, null);
                    FrameDrawStep frame = new FrameDrawStep(i, bitmap);
                    appendFrame(frame);

                    // 绘画该帧之后的 FRAMES_STEP_INTERVAL_MAX - 1 个绘画步骤
                    for (int j = 0; j < FRAMES_STEP_INTERVAL_MAX; j++) {
                        i++;
                        // 注意不要绘画最后一个绘画步骤
                        if (i < drawStepSize - 1) {
                            mDrawSteps.get(i).onDraw(mBitmapCanvas);
                        } else {
                            break;
                        }
                    }
                }

                // 如果目前恢复的最后一个关键帧所在的位置不是倒数第二个绘画步骤，需要将目前的图像存储为一个新的关键帧
                boolean saveFrame;
                int currentFrameSize = mFrames.size();
                if (currentFrameSize <= 0) {
                    saveFrame = true;
                } else {
                    FrameDrawStep lastFrame = mFrames.get(currentFrameSize - 1);
                    saveFrame = lastFrame.mDrawStepIndex < drawStepSize - 2;
                    if (lastFrame.mDrawStepIndex > drawStepSize - 2) {
                        throw new RuntimeException("logic error. last frame draw step index[" + lastFrame.mDrawStepIndex + "], drawStepSize[" + drawStepSize + "]");
                    }
                }
                if (saveFrame) {
                    // 将当前的图像存储为一个关键帧
                    Bitmap bitmap = Bitmap.createBitmap(mBitmapWidth, mBitmapHeight, Bitmap.Config.ARGB_8888);
                    new Canvas(bitmap).drawBitmap(mBitmap, 0f, 0f, null);
                    FrameDrawStep frame = new FrameDrawStep(drawStepSize - 2, bitmap);
                    appendFrame(frame);
                }

                // 绘画最后一个绘画步骤
                mDrawSteps.get(drawStepSize - 1).onDraw(mBitmapCanvas);
                return true;
            }

            /**
             * 将该关键帧存储到关键帧数组末尾
             */
            private void appendFrame(FrameDrawStep frame) {
                int currentFrameSize = mFrames.size();

                if (currentFrameSize > 0) {
                    // 校验最后一帧的位置必须比新增加这一帧靠前
                    if (mFrames.get(currentFrameSize - 1).mDrawStepIndex >= frame.mDrawStepIndex) {
                        throw new RuntimeException(
                                "append frame error, index out of range. ["
                                        + (mFrames.get(currentFrameSize - 1).mDrawStepIndex)
                                        + ", " + (frame.mDrawStepIndex) + "]");
                    }
                }

                if (currentFrameSize >= FRAMES_SIZE_MAX) {
                    // 关键帧过多，删除第一个，依次向前移动，新的关键帧放到最后一个位置
                    for (int index = 0; index < currentFrameSize - 1; index++) {
                        mFrames.set(index, mFrames.get(index + 1));
                    }
                    mFrames.set(currentFrameSize - 1, frame);
                } else {
                    mFrames.add(frame);
                }
            }

            // 重新绘制缓冲区
            private void refreshBuffer() {
                // 清空背景
                mBitmapCanvas.drawColor(Color.WHITE);

                // 取目前关键帧中的最后两个关键帧
                FrameDrawStep f1 = null; // 最后一个关键帧
                FrameDrawStep f2 = null; // 倒数第二个关键帧
                final int framesSize = mFrames.size();
                if (framesSize > 1) {
                    // 目前关键帧数量至少有两个
                    f1 = mFrames.get(framesSize - 1);
                    f2 = mFrames.get(framesSize - 2);
                } else if (framesSize > 0) {
                    // 目前关键帧只有一个
                    f1 = mFrames.get(0);
                    f2 = null;
                }

                // 绘画最后一个关键帧 (最后一个关键帧之前的图像不必重新绘画)
                if (f1 != null) {
                    f1.onDraw(mBitmapCanvas);
                }

                final int drawStepSize = mDrawSteps.size();
                // 绘画最后一个关键帧之后除最后一个绘画步骤外的所有绘画步骤
                // 如果没有关键帧，则从第一个绘画步骤开始绘画
                boolean foundDrawStepsAfterLastFrame = false;
                int drawStepIndexStart = -1;
                if (f1 != null) {
                    drawStepIndexStart = f1.mDrawStepIndex;
                }
                for (int i = drawStepIndexStart + 1; i < drawStepSize - 1; i++) {
                    foundDrawStepsAfterLastFrame = true;
                    mDrawSteps.get(i).onDraw(mBitmapCanvas);
                }

                if (foundDrawStepsAfterLastFrame) {
                    // 将目前的图像存储为一个新的关键帧(该关键帧与最终图像只差最后一个绘画步骤)

                    // 如果最后一个关键帧可以覆盖，则复用最后一个关键帧的内存
                    boolean reuseLastFrame = false;
                    if (f1 != null) {
                        if (f2 == null && f1.mDrawStepIndex < FRAMES_STEP_INTERVAL_MAX) {
                            reuseLastFrame = true;
                        } else if (f2 != null && f1.mDrawStepIndex - f2.mDrawStepIndex < FRAMES_STEP_INTERVAL_MAX) {
                            reuseLastFrame = true;
                        }
                    }

                    Bitmap lastFrameBitmap;
                    if (reuseLastFrame) {
                        lastFrameBitmap = f1.mBitmap;
                    } else {
                        lastFrameBitmap = Bitmap.createBitmap(mBitmapWidth, mBitmapHeight, Bitmap.Config.ARGB_8888);
                    }
                    new Canvas(lastFrameBitmap).drawBitmap(mBitmap, 0f, 0f, null);
                    FrameDrawStep latestFrame = new FrameDrawStep(drawStepSize - 2, lastFrameBitmap);

                    if (reuseLastFrame) {
                        mFrames.set(framesSize - 1, latestFrame);
                    } else {
                        appendFrame(latestFrame);
                    }
                }

                // 绘画最后一个绘画步骤
                if (drawStepSize > 0) {
                    mDrawSteps.get(drawStepSize - 1).onDraw(mBitmapCanvas);
                }
            }

            /**
             * 将手势结合当前画笔，处理为绘画步骤. undo or redo changed return true.
             */
            public boolean dispatchGestureAction(GestureAction gestureAction) {
                // 标记 undo or redo 是否产生了变化
                boolean changed = false;
                // 开始新的动作，清空可能存在的 redo 内容, cancel 动作不清空 redo
                if (!(gestureAction instanceof CancelGestureAction)) {
                    changed |= clearRedo();
                }

                int drawStepSize = mDrawSteps.size();
                if (drawStepSize <= 0) {
                    // 第一个绘画步骤
                    DrawStep drawStep = mBrush.createDrawStep(gestureAction);
                    mDrawSteps.add(drawStep);
                    // 如果新步骤是一个空步骤，则 undo 不会变更
                    boolean isNewEmptyDrawStep = drawStep instanceof EmptyDrawStep;
                    changed |= !isNewEmptyDrawStep;
                    return changed;
                }

                DrawStep lastDrawStep = mDrawSteps.get(drawStepSize - 1);
                if (lastDrawStep.dispatchGestureAction(gestureAction, getBrush())) {
                    // 当前绘画手势被最后一个绘画步骤继续处理
                    // undo 不产生变化
                    changed |= false;
                    return changed;
                }

                // 开始一个新的绘画步骤
                DrawStep drawStep = mBrush.createDrawStep(gestureAction);
                // 如果新步骤是一个空步骤，则 undo 不会变更
                boolean isNewEmptyDrawStep = drawStep instanceof EmptyDrawStep;

                // 覆盖空步骤或者添加新步骤， undo 可能变化
                changed |= false;

                if (lastDrawStep instanceof EmptyDrawStep) {
                    // 最后一步是一个空步骤， 覆盖该空步骤

                    if (drawStepSize == 1) {
                        // 当前只有一个空步骤， undo 取决于新步骤是否也是空步骤
                        changed |= !isNewEmptyDrawStep;
                    }
                    mDrawSteps.set(drawStepSize - 1, drawStep);
                } else {
                    // 之前有非空步骤，不管新步骤是否是空步骤，undo 都不会产生变化. (总是可以 undo)
                    mDrawSteps.add(drawStep);
                }
                return changed;
            }
        }

        /**
         * DoodleView 上的触摸事件
         */
        public boolean onTouchEvent(MotionEvent event) {
            if (!isAvailable()) {
                // 当 doodle view 没有准备好时，将触摸事件处理为 cancel 事件
                int action = MotionEventCompat.getActionMasked(event);
                if (action != MotionEvent.ACTION_CANCEL) {
                    event = MotionEventUtil.createCancelTouchMotionEvent();
                }
            }

            mCanvasScaleGestureDetector.onTouchEvent(event);
            mCanvasTranslationGestureDetectorCompat.onTouchEvent(event);
            mTextureActionGestureDetectorCompat.onTouchEvent(event);

            return true;
        }

    }

    /**
     * 绘画手势
     *
     * @see CancelGestureAction
     * @see SinglePointGestureAction
     * @see ScrollGestureAction
     */
    public interface GestureAction {
    }

    /**
     * 手势取消，开启一个新的手势或者标记上一个手势完结
     */
    public static final class CancelGestureAction implements GestureAction {
    }

    /**
     * 单指点击
     */
    public static final class SinglePointGestureAction implements GestureAction {
        public final MotionEvent event;

        public SinglePointGestureAction(MotionEvent event) {
            this.event = event;
        }
    }

    /**
     * 单指移动
     */
    public static final class ScrollGestureAction implements GestureAction {
        public final MotionEvent downEvent;
        public final MotionEvent currentEvent;

        public ScrollGestureAction(MotionEvent downEvent, MotionEvent currentEvent) {
            this.downEvent = downEvent;
            this.currentEvent = currentEvent;
        }
    }

    /**
     * 帧图像, 画一张图
     */
    private static final class FrameDrawStep extends DrawStep {
        private final int mDrawStepIndex; // 该帧对应的 draw step index, 绘画步骤从 0 开始
        private final Bitmap mBitmap; // 从0到该 draw step index (包含) 所有绘画步骤完成之后的图像

        public FrameDrawStep(int drawStepIndex, Bitmap bitmap) {
            super(null);
            mDrawStepIndex = drawStepIndex;
            mBitmap = bitmap;
        }

        @Override
        public void onDraw(@NonNull Canvas canvas) {
            canvas.drawBitmap(mBitmap, 0f, 0f, null);
        }
    }

}
