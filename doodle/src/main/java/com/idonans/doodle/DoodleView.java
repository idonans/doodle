package com.idonans.doodle;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.widget.FrameLayout;

import com.idonans.acommon.lang.Available;
import com.idonans.acommon.lang.CommonLog;
import com.idonans.acommon.lang.TaskQueue;
import com.idonans.acommon.lang.Threads;

import java.util.ArrayList;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;


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
    private Render mRender;
    private TextureView mTextureView;
    private Brush mBrush;

    private void init() {
        mRender = new Render(getContext());

        mTextureView = new TextureView(getContext());
        addView(mTextureView, new LayoutParams(MATCH_PARENT, MATCH_PARENT));

        mTextureView.setSurfaceTextureListener(new TextureListener());

        setAspectRatio(3, 4);
    }

    /**
     * 设置画刷
     */
    public void setBrush(Brush brush) {
        mBrush = brush;
    }

    /**
     * 获得当前的画刷, 如果要更改画刷属性，需要新建一个画刷并设置
     */
    public Brush getBrush() {
        return mBrush;
    }

    /**
     * 设置画布的宽高比， 此方法会删除之前的所有绘画内容
     */
    public void setAspectRatio(int width, int height) {
        mRender.setAspectRatio(width, height);
        requestLayout();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = MotionEventCompat.getActionMasked(event);
        if (action == MotionEvent.ACTION_DOWN) {
            requestDisallowInterceptTouchEvent(true);
        }

        mRender.onTouchEvent(event);
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

    private class Render implements Available {

        private static final String TAG = "DoodleView$Render";

        // 画布的宽高比
        private int mAspectWidth = 1;
        private int mAspectHeight = 1;

        private final Object mBufferLock = new Object();
        private volatile CanvasBuffer mCanvasBuffer;

        private boolean mTextureEnable;

        private final TaskQueue mTaskQueue = new TaskQueue(1);

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

        private void setAspectRatio(int aspectWidth, int aspectHeight) {
            synchronized (mBufferLock) {
                mCanvasBuffer = null;
                mAspectWidth = aspectWidth;
                mAspectHeight = aspectHeight;
            }
        }

        private void init(int width, int height) {
            synchronized (mBufferLock) {
                if (mCanvasBuffer != null) {
                    CommonLog.d(TAG + " canvas buffer found " + mCanvasBuffer.toShortString()
                            + ", current aspect [" + mAspectWidth + ", " + mAspectHeight + "], [" + width + ", " + height + "]");
                    if (mCanvasBuffer.mTextureWidth != width || mCanvasBuffer.mTextureHeight != height) {
                        CommonLog.e(TAG + " current canvas buffer texture size not match");
                    }
                    return;
                }

                int[] canvasBufferSize = calculatePerfectSizeWithAspect(width, height, mAspectWidth, mAspectHeight);
                CommonLog.d(new StringBuilder()
                        .append(TAG)
                        .append(" create canvas buffer")
                        .append(", texture size [" + width + ", " + height + "]")
                        .append(", current aspect [" + mAspectWidth + ", " + mAspectHeight + "]")
                        .append(", canvas buffer size [" + canvasBufferSize[0] + ", " + canvasBufferSize[1] + "]"));
                mCanvasBuffer = new CanvasBuffer(width, height, canvasBufferSize[0], canvasBufferSize[1]);
            }
        }

        public void setTextureEnable(boolean textureEnable) {
            mTextureEnable = textureEnable;
            resumeDoodle();
        }

        private void resumeDoodle() {
            CanvasBuffer canvasBuffer = mCanvasBuffer;
            if (isAvailable()) {
                canvasBuffer.postInvalidate();
                postInvalidate();
            }
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
                    if (values[Matrix.MSCALE_X] < CanvasBuffer.MAX_SCALE) {
                        matrix.postScale(scaleFactor, scaleFactor, mPx, mPy);
                    }
                } else if (scaleFactor < 1f) {
                    // 如果还可以缩小，再缩小
                    if (values[Matrix.MSCALE_X] > CanvasBuffer.MIN_SCALE) {
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
                Threads.sleepQuietly(canvasBuffer.getLastDrawingTime());

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
                    canvas.drawColor(Color.TRANSPARENT);

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
            mTaskQueue.enqueue(new Draw());
        }

        public void enqueueGestureAction(final GestureAction gestureAction) {
            mTaskQueue.enqueue(new Runnable() {
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
            mTaskQueue.enqueue(new Runnable() {
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
            mTaskQueue.enqueue(new Runnable() {
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
            mTaskQueue.enqueue(new Runnable() {
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
            mTaskQueue.enqueue(new Runnable() {
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

            private static final float MAX_SCALE = 2.75f;
            private static final float MIN_SCALE = 0.75f;

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

                // 调整图像位置，使得 bitmap 居中
                Matrix matrix = getMatrix();
                matrix.postTranslate((mTextureWidth - mBitmapWidth) / 2f, (mTextureHeight - mBitmapHeight) / 2f);
                setMatrix(matrix);
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

            public void setMatrix(Matrix matrix) {

                // 需要约束变换范围, 先处理 scale, 再处理 translation
                float[] values = new float[9];

                matrix.getValues(values);
                float scale = values[Matrix.MSCALE_X];
                boolean changed = false;
                if (scale > MAX_SCALE) {
                    float scaleFactorAdjust = MAX_SCALE / scale;
                    matrix.postScale(scaleFactorAdjust, scaleFactorAdjust, values[Matrix.MTRANS_X], values[Matrix.MTRANS_Y]);
                    changed = true;
                } else if (scale < MIN_SCALE) {
                    float scaleFactorAdjust = MIN_SCALE / scale;
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
                Matrix matrix = getMatrix();
                setMatrix(matrix);
            }

            public int getBufferWidth() {
                return mBitmapWidth;
            }

            public int getBufferHeight() {
                return mBitmapHeight;
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
                refreshBuffer();
                canvas.drawBitmap(mBitmap, 0f, 0f, null);
            }

            // 重新绘制缓冲区
            private void refreshBuffer() {
                // 清空背景
                mBitmapCanvas.drawColor(Color.WHITE);

                // 取目前关键帧中的最后两个关键帧
                FrameDrawStep f1 = null; // 最后一个关键帧
                FrameDrawStep f2 = null; // 倒数第二个关键帧
                int framesSize = mFrames.size();
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

                int drawStepSize = mDrawSteps.size();
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
                        if (framesSize >= FRAMES_SIZE_MAX) {
                            // 关键帧过多，删除第一个，依次向前移动，新的关键帧放到最后一个位置
                            for (int i = 0; i < framesSize - 1; i++) {
                                mFrames.set(i, mFrames.get(i + 1));
                            }
                            mFrames.set(framesSize - 1, latestFrame);
                        } else {
                            mFrames.add(latestFrame);
                        }
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
                // 开始新的动作，清空可能存在的 redo 内容
                changed |= clearRedo();

                int drawStepSize = mDrawSteps.size();
                if (drawStepSize <= 0) {
                    // 第一个动作
                    DrawStep drawStep = DrawStep.create(gestureAction, getBrush());
                    if (drawStep == null) {
                        CommonLog.e(TAG + " dispatchGestureAction create draw step null.");
                        return changed;
                    }
                    mDrawSteps.add(drawStep);
                    // 第一个绘画步骤， undo 从无到有
                    changed |= true;
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
                DrawStep drawStep = DrawStep.create(gestureAction, getBrush());
                if (drawStep == null) {
                    CommonLog.e(TAG + " last draw step ignore current gesture action,  dispatchGestureAction create draw step null.");
                    // 新步骤构建失败， undo 不产生变化
                    changed |= false;
                    return changed;
                }

                // 覆盖空步骤或者添加新步骤， undo 可能变化。如果当前只有一个空步骤，则变化从无到有，否则总是能够 undo, 不变化。
                changed |= false;
                if (lastDrawStep instanceof EmptyDrawStep) {
                    // 最后一步是一个空步骤， 覆盖该空步骤
                    if (drawStepSize == 1) {
                        // 当前只有一个空步骤， undo 会从无到有
                        changed |= true;
                    }
                    mDrawSteps.set(drawStepSize - 1, drawStep);
                } else {
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
                return false;
            }

            mCanvasScaleGestureDetector.onTouchEvent(event);
            mCanvasTranslationGestureDetectorCompat.onTouchEvent(event);
            mTextureActionGestureDetectorCompat.onTouchEvent(event);

            return true;
        }

    }

    /**
     * 绘画手势
     */
    public interface GestureAction {
    }

    /**
     * 手势取消，开启一个新的手势或者标记上一个手势完结
     */
    public static class CancelGestureAction implements GestureAction {
    }

    /**
     * 单指点击
     */
    public static class SinglePointGestureAction implements GestureAction {
        public final MotionEvent event;

        public SinglePointGestureAction(MotionEvent event) {
            this.event = event;
        }
    }

    /**
     * 单指移动
     */
    public static class ScrollGestureAction implements GestureAction {
        public final MotionEvent downEvent;
        public final MotionEvent currentEvent;

        public ScrollGestureAction(MotionEvent downEvent, MotionEvent currentEvent) {
            this.downEvent = downEvent;
            this.currentEvent = currentEvent;
        }
    }

    /**
     * 画刷
     */
    public static class Brush {

        /**
         * 铅笔
         */
        public final static int TYPE_PENCIL = 1;

        public final int color; // 画刷的颜色 ARGB
        public final int size; // 画刷的大小
        public final int alpha; // 画刷的透明度 [0, 255]
        public final int type; // 画刷的类型

        public Brush(int color, int size, int alpha, int type) {
            this.color = color;
            this.size = size;
            this.alpha = alpha;
            this.type = type;
        }

        public static Brush createPencil(int color, int size, int alpha) {
            return new Brush(color, size, alpha, TYPE_PENCIL);
        }

        public static void mustPencil(Brush brush) {
            if (brush == null || brush.type != TYPE_PENCIL) {
                throw new BrushNotSupportException(brush);
            }
        }

        /**
         * 根据当前画刷配置创建画笔
         */
        protected Paint createPaint() {
            Paint paint = new Paint();
            paint.setColor(color);
            paint.setAlpha(alpha);
            paint.setStrokeWidth(size);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND); // 笔刷样式 圆形
            paint.setDither(true); // 图像抖动处理 使图像更清晰
            paint.setAntiAlias(true); // 抗锯齿
            return paint;
        }

    }

    public static class BrushNotSupportException extends RuntimeException {
        private final Brush mBrush;

        public BrushNotSupportException(Brush brush) {
            super("不支持的画刷 " + brush);
            mBrush = brush;
        }

        public Brush getBrush() {
            return mBrush;
        }
    }

    /**
     * 绘画步骤
     */
    public static class DrawStep {

        private String TAG = "DrawStep#" + getClass().getSimpleName();
        protected final Brush mDrawBrush;
        protected final Paint mDrawPaint;

        public DrawStep(Brush drawBrush) {
            mDrawBrush = drawBrush;
            if (mDrawBrush != null) {
                mDrawPaint = mDrawBrush.createPaint();
            } else {
                mDrawPaint = null;
            }
        }

        @CallSuper
        public void onDraw(@NonNull Canvas canvas) {
            CommonLog.d(TAG + " onDraw");
        }

        /**
         * 当前的绘画步骤是否继续消费该绘画手势，如果继续消费返回 true, 否则返回 false.
         */
        public boolean dispatchGestureAction(GestureAction gestureAction, Brush brush) {
            if (mDrawBrush != brush) {
                // 画笔变更，开始新的绘画步骤
                return false;
            }
            if (gestureAction == null || brush == null) {
                return false;
            }
            return onGestureAction(gestureAction);
        }

        /**
         * 当前的绘画步骤是否继续消费该绘画手势，如果继续消费返回 true, 否则返回 false.
         */
        protected boolean onGestureAction(@NonNull GestureAction gestureAction) {
            return false;
        }

        public static DrawStep create(GestureAction gestureAction, Brush brush) {
            if (gestureAction == null
                    || gestureAction instanceof CancelGestureAction
                    || brush == null) {
                return new EmptyDrawStep(brush);
            }

            if (gestureAction instanceof SinglePointGestureAction) {
                SinglePointGestureAction singlePointGestureAction = (SinglePointGestureAction) gestureAction;
                if (brush.type == Brush.TYPE_PENCIL) {
                    return new PointDrawStep(brush, singlePointGestureAction.event.getX(), singlePointGestureAction.event.getY());
                }
            }

            if (gestureAction instanceof ScrollGestureAction) {
                ScrollGestureAction scrollGestureAction = (ScrollGestureAction) gestureAction;
                if (brush.type == Brush.TYPE_PENCIL) {
                    return new ScribbleDrawStep(brush,
                            scrollGestureAction.downEvent.getX(),
                            scrollGestureAction.downEvent.getY(),
                            scrollGestureAction.currentEvent.getX(),
                            scrollGestureAction.currentEvent.getY());
                }
            }

            // 其他绘画待扩展

            return new EmptyDrawStep(brush);
        }
    }

    /**
     * 空的绘画步骤
     */
    public final static class EmptyDrawStep extends DrawStep {

        public EmptyDrawStep(Brush drawBrush) {
            super(drawBrush);
        }

        @Override
        protected boolean onGestureAction(GestureAction gestureAction) {
            if (gestureAction instanceof CancelGestureAction) {
                return true;
            }
            return false;
        }

    }

    /**
     * 帧图像, 画一张图
     */
    public static class FrameDrawStep extends DrawStep {
        private final int mDrawStepIndex; // 该帧对应的 draw step index, 绘画步骤从 0 开始
        private final Bitmap mBitmap; // 从0到该 draw step index (包含) 所有绘画步骤完成之后的图像

        public FrameDrawStep(int drawStepIndex, Bitmap bitmap) {
            super(null);
            mDrawStepIndex = drawStepIndex;
            mBitmap = bitmap;
        }

        @Override
        public void onDraw(@NonNull Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawBitmap(mBitmap, 0f, 0f, null);
        }
    }

    /**
     * 画点
     */
    public static class PointDrawStep extends DrawStep {

        private final float mX;
        private final float mY;

        public PointDrawStep(Brush drawBrush, float x, float y) {
            super(drawBrush);
            Brush.mustPencil(drawBrush);

            mX = x;
            mY = y;
        }

        @Override
        public void onDraw(@NonNull Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawPoint(mX, mY, mDrawPaint);
        }
    }

    /**
     * 自由绘制
     */
    public static class ScribbleDrawStep extends DrawStep {

        private static final String TAG = "ScribbleDrawStep";
        private final Path mPath;
        private float mPreX;
        private float mPreY;

        public ScribbleDrawStep(Brush drawBrush, float startX, float startY, float moveX, float moveY) {
            super(drawBrush);
            Brush.mustPencil(drawBrush);

            mPath = new Path();
            mPath.moveTo(startX, startY);
            mPreX = startX;
            mPreY = startY;
            toPoint(moveX, moveY);
        }

        /**
         * 绘画平滑曲线
         */
        private void toPoint(float x, float y) {
            // 使用贝塞尔去绘制，线条更平滑
            mPath.quadTo(mPreX, mPreY, (mPreX + x) / 2, (mPreY + y) / 2);
            mPreX = x;
            mPreY = y;
        }

        @Override
        protected boolean onGestureAction(@NonNull GestureAction gestureAction) {
            if (!(gestureAction instanceof ScrollGestureAction)) {
                return false;
            }

            ScrollGestureAction scrollGestureAction = (ScrollGestureAction) gestureAction;

            int historySize = scrollGestureAction.currentEvent.getHistorySize();
            CommonLog.d(TAG + " history size: " + historySize);
            for (int i = 0; i < historySize; i++) {
                toPoint(scrollGestureAction.currentEvent.getHistoricalX(i),
                        scrollGestureAction.currentEvent.getHistoricalY(i));
            }

            toPoint(scrollGestureAction.currentEvent.getX(),
                    scrollGestureAction.currentEvent.getY());
            return true;
        }

        @Override
        public void onDraw(@NonNull Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawPath(mPath, mDrawPaint);
        }

    }

}
