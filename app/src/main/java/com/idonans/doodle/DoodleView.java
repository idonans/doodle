package com.idonans.doodle;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import com.idonans.acommon.lang.Available;
import com.idonans.acommon.lang.CommonLog;
import com.idonans.acommon.lang.TaskQueue;

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
    private RootView mRootView;
    private TextureView mTextureView;
    private Brush mBrush;

    private void init() {
        mRender = new Render(getContext());

        mRootView = new RootView(getContext());
        mTextureView = new TextureView(getContext());
        mRootView.addView(mTextureView, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        FrameLayout.LayoutParams rootViewLayouts = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        rootViewLayouts.gravity = Gravity.CENTER;
        addView(mRootView, rootViewLayouts);

        mTextureView.setSurfaceTextureListener(new TextureListener());

        setAspectRatio(3, 4);
        setCanvasBackgroundColor(Color.DKGRAY);
    }

    public void setCanvasBackgroundColor(int color) {
        mRootView.setBackgroundColor(color);
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

    private class RootView extends FrameLayout {

        private final String TAG = "DoodleView$RootView";

        public RootView(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            mRender.onTouchEvent(event);
            return true;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            final int originalWidth = View.MeasureSpec.getSize(widthMeasureSpec);
            final int originalHeight = View.MeasureSpec.getSize(heightMeasureSpec);
            if (originalWidth <= 0 || originalHeight <= 0) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }

            // 先按照整宽计算
            int targetWidth = originalWidth;
            int targetHeight = Float.valueOf(1f * targetWidth * mRender.mAspectHeight / mRender.mAspectWidth).intValue();
            if (targetHeight <= originalHeight) {
                // 调整，使得宽高比完美匹配
                targetWidth -= targetWidth % mRender.mAspectWidth;
                targetHeight = targetWidth * mRender.mAspectHeight / mRender.mAspectWidth;
                super.onMeasure(
                        View.MeasureSpec.makeMeasureSpec(targetWidth, MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(targetHeight, MeasureSpec.EXACTLY));
                return;
            }

            // 按照整高计算
            targetHeight = originalHeight;
            targetWidth = Float.valueOf(1f * targetHeight * mRender.mAspectWidth / mRender.mAspectHeight).intValue();
            if (targetWidth > originalWidth) {
                throw new RuntimeException("measure error");
            }

            // 调整，使得宽高比完美匹配
            targetHeight -= targetHeight % mRender.mAspectHeight;
            targetWidth = targetHeight * mRender.mAspectWidth / mRender.mAspectHeight;

            super.onMeasure(
                    View.MeasureSpec.makeMeasureSpec(targetWidth, MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(targetHeight, MeasureSpec.EXACTLY));
        }

    }

    private class TextureListener implements TextureView.SurfaceTextureListener {

        private final String TAG = "DoodleView$TextureListener";

        /**
         * Invoked when a {@link TextureView}'s SurfaceTexture is ready for use.
         *
         * @param surface The surface returned by
         *                {@link TextureView#getSurfaceTexture()}
         * @param width   The width of the surface
         * @param height  The height of the surface
         */
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            CommonLog.d(TAG + " onSurfaceTextureAvailable width:" + width + ", height:" + height);
            mRender.init(width, height);
            mRender.setTextureEnable(true);
        }

        /**
         * Invoked when the {@link SurfaceTexture}'s buffers size changed.
         *
         * @param surface The surface returned by
         *                {@link TextureView#getSurfaceTexture()}
         * @param width   The new width of the surface
         * @param height  The new height of the surface
         */
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            CommonLog.d(TAG + " onSurfaceTextureSizeChanged width:" + width + ", height:" + height);
            mRender.init(width, height);
        }

        /**
         * Invoked when the specified {@link SurfaceTexture} is about to be destroyed.
         * If returns true, no rendering should happen inside the surface texture after this method
         * is invoked. If returns false, the client needs to call {@link SurfaceTexture#release()}.
         * Most applications should return true.
         *
         * @param surface The surface about to be destroyed
         */
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            CommonLog.d(TAG + " onSurfaceTextureDestroyed");
            mRender.setTextureEnable(false);
            return true;
        }

        /**
         * Invoked when the specified {@link SurfaceTexture} is updated through
         * {@link SurfaceTexture#updateTexImage()}.
         *
         * @param surface The surface just updated
         */
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            CommonLog.d(TAG + " onSurfaceTextureUpdated");
        }
    }

    private class Render implements Available {

        private static final String TAG = "Render";

        // 画布的宽高比
        private int mAspectWidth = 1;
        private int mAspectHeight = 1;

        private final Object mBufferLock = new Object();
        private volatile CanvasBuffer mCanvasBuffer;

        private boolean mTextureEnable;

        private final TaskQueue mTaskQueue = new TaskQueue(1);

        private final ScaleGestureDetector mCanvasScaleGestureDetector;
        private final GestureDetectorCompat mCanvasTranslationGestureDetectorCompat;
        private final GestureDetectorCompat mTextureActionGestureDetectorCompat;

        private final Paint mPaint;

        private Render(Context context) {
            mCanvasScaleGestureDetector = new ScaleGestureDetector(context, new CanvasScaleGestureListener());
            mCanvasTranslationGestureDetectorCompat = new GestureDetectorCompat(context, new CanvasTranslationGestureListener());
            mCanvasTranslationGestureDetectorCompat.setIsLongpressEnabled(false);
            mTextureActionGestureDetectorCompat = new GestureDetectorCompat(context, new TextureActionGestureListener());
            mTextureActionGestureDetectorCompat.setIsLongpressEnabled(false);

            mPaint = new Paint();
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
                    CommonLog.d(TAG + " canvas buffer found [" + mAspectWidth + ", " + mAspectHeight + "]:[" + width + ", " + height + "]");
                    return;
                }

                // 校验宽高比是否匹配
                if (mAspectWidth * height != mAspectHeight * width) {
                    CommonLog.d(TAG + " aspect radio not match , [" + mAspectWidth + ", " + mAspectHeight + "]:[" + width + ", " + height + "]");
                    return;
                }
                mCanvasBuffer = new CanvasBuffer(width, height);
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

            private static final String TAG = "Render$CanvasScaleGestureListener";

            private float mPx = -1;
            private float mPy = -1;

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                CanvasBuffer canvasBuffer = mCanvasBuffer;
                if (!isAvailable()) {
                    return false;
                }

                if (mPx < 0 || mPy < 0) {
                    CommonLog.d(TAG + " px or py invalid [" + mPx + ", " + mPy + "]");
                    return false;
                }

                Matrix matrix = canvasBuffer.getMatrix();
                float[] values = new float[9];
                matrix.getValues(values);

                float oldScale = values[Matrix.MSCALE_X];
                float scaleFactor = detector.getScaleFactor();
                float targetScale = oldScale * scaleFactor;

                if (targetScale >= oldScale && oldScale >= CanvasBuffer.MAX_SCALE) {
                    // 已经最大，不需要再放大
                    // 需要返回 true, 后续可能有缩小手势
                    return true;
                }

                if (targetScale <= oldScale && oldScale <= CanvasBuffer.MIN_SCALE) {
                    // 已经最小，不需要再缩小
                    // 需要返回 true, 后续可能有放大手势
                    return true;
                }

                if (targetScale > CanvasBuffer.MAX_SCALE) {
                    scaleFactor = CanvasBuffer.MAX_SCALE / oldScale;
                }
                if (targetScale < CanvasBuffer.MIN_SCALE) {
                    scaleFactor = CanvasBuffer.MIN_SCALE / oldScale;
                }

                matrix.postScale(scaleFactor, scaleFactor, mPx, mPy);
                canvasBuffer.setMatrix(matrix);
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                CanvasBuffer canvasBuffer = mCanvasBuffer;
                if (!isAvailable()) {
                    return false;
                }

                int bufferWidth = canvasBuffer.getBufferWidth();
                int bufferHeight = canvasBuffer.getBufferHeight();

                long eventTime = detector.getEventTime();
                MotionEvent motionEvent = MotionEvent.obtain(eventTime, eventTime, MotionEvent.ACTION_DOWN, detector.getFocusX(), detector.getFocusY(), 0);
                motionEvent.transform(canvasBuffer.getMatrixInverse());
                float x = motionEvent.getX();
                float y = motionEvent.getY();
                boolean canScale = new RectF(0, 0, bufferWidth, bufferHeight).contains(x, y);

                CommonLog.d(TAG + " onScaleBegin [" + detector.getFocusX() + ", " + detector.getFocusY() + "] -> [" + x + ", " + y + "], buffer size[" + bufferWidth + ", " + bufferHeight + "], " + canScale);

                if (canScale) {
                    mPx = x;
                    mPy = y;
                } else {
                    mPx = -1;
                    mPy = -1;
                }
                return canScale;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                mPx = -1;
                mPy = -1;
            }

        }

        private class CanvasTranslationGestureListener implements GestureDetector.OnGestureListener {

            private static final String TAG = "Render$CanvasTranslationGestureListener";

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
                Matrix matrix = canvasBuffer.getMatrix();
                float[] values = new float[9];
                matrix.getValues(values);
                float oldX = values[Matrix.MTRANS_X];
                float oldY = values[Matrix.MTRANS_Y];

                float targetX = oldX - distanceX;
                float targetY = oldY - distanceY;

                CommonLog.d(TAG + " matrix translate [" + oldX + ", " + oldY + "] ([" + distanceX + ", " + distanceY + "]) -> [" + targetX + ", " + targetY + "]");

                // 限制移动边界
                float pointXMax = (canvasBuffer.getBufferWidth() - canvasBuffer.getBufferWidth() * CanvasBuffer.MIN_SCALE) / 2;
                float pointYMax = (canvasBuffer.getBufferHeight() - canvasBuffer.getBufferHeight() * CanvasBuffer.MIN_SCALE) / 2;
                if (targetX > pointXMax) {
                    distanceX = oldX - pointXMax;
                }
                if (targetY > pointYMax) {
                    distanceY = oldY - pointYMax;
                }
                float scale = values[Matrix.MSCALE_X];
                float pointXMin = -canvasBuffer.getBufferWidth() * scale + canvasBuffer.getBufferWidth() * CanvasBuffer.MIN_SCALE + pointXMax;
                float pointYMin = -canvasBuffer.getBufferHeight() * scale + canvasBuffer.getBufferHeight() * CanvasBuffer.MIN_SCALE + pointYMax;
                if (targetX < pointXMin) {
                    distanceX = oldX - pointXMin;
                }
                if (targetY < pointYMin) {
                    distanceY = oldY - pointYMin;
                }

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

            @Override
            public boolean onDown(MotionEvent e) {
                if (!isAvailable()) {
                    return false;
                }

                enqueueGestureAction(new CancelGestureAction());
                return true;
            }

            @Override
            public void onShowPress(MotionEvent e) {
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                CanvasBuffer canvasBuffer = mCanvasBuffer;
                if (!isAvailable()) {
                    return false;
                }

                MotionEvent event = MotionEvent.obtain(e);
                event.transform(canvasBuffer.getMatrixInverse());

                CommonLog.d(TAG + " onSingleTapUp [" + e.getX() + ", " + e.getY() + "] -> [" + event.getX() + ", " + event.getY() + "]");

                enqueueGestureAction(new SinglePointGestureAction(event));
                enqueueGestureAction(new CancelGestureAction());
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                CanvasBuffer canvasBuffer = mCanvasBuffer;
                if (!isAvailable()) {
                    return false;
                }

                if (e2.getPointerCount() > 1) {
                    enqueueGestureAction(new CancelGestureAction());
                    return false;
                }

                // 单指移动
                Matrix matrixInverse = canvasBuffer.getMatrixInverse();
                MotionEvent downEvent = MotionEvent.obtain(e1);
                MotionEvent currentEvent = MotionEvent.obtain(e2);

                // down event 在此处变换时可能对应的点已经有偏差，需要确保在 down 到目前位置画布没有缩放或者移动
                downEvent.transform(matrixInverse);

                currentEvent.transform(matrixInverse);
                enqueueGestureAction(new ScrollGestureAction(downEvent, currentEvent));
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
                    canvas.drawColor(Color.WHITE);

                    // 将缓冲区中的内容绘画到 canvas 上
                    canvasBuffer.draw(canvas, mPaint);
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
                    canvasBuffer.dispatchGestureAction(gestureAction);
                }
            });
            postInvalidate();
        }


        private class CanvasBuffer {

            private static final String TAG = "Render$CanvasBuffer";
            private static final int FRAMES_SIZE_MAX = 4;
            // 关键帧之间至多间隔的 action 数量
            private static final int FRAMES_STEP_INTERVAL_MAX = 8;
            // 关键帧缓存图像
            private final ArrayList<FrameDrawStep> mFrames = new ArrayList<>(FRAMES_SIZE_MAX);
            private final ArrayList<DrawStep> mDrawSteps = new ArrayList<>();

            private static final float MAX_SCALE = 2.75f;
            private static final float MIN_SCALE = 0.75f;

            private final Bitmap mBitmap; // 当前画布图像(绘画缓冲区)
            private final int mBitmapWidth; // 当前画布图像宽度
            private final int mBitmapHeight; // 当前画布图像高度
            private final Canvas mBitmapCanvas; // 原始画布

            private final Matrix mMatrixTmp;
            private final Matrix mMatrixInvertTmp;

            public CanvasBuffer(int canvasWidth, int canvasHeight) {
                mBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
                mBitmapWidth = mBitmap.getWidth();
                mBitmapHeight = mBitmap.getHeight();
                mBitmapCanvas = new Canvas(mBitmap);

                mMatrixTmp = new Matrix();
                mMatrixInvertTmp = new Matrix();
            }

            public void setMatrix(Matrix matrix) {
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

            public Matrix getMatrix() {
                mMatrixTmp.reset();
                mTextureView.getTransform(mMatrixTmp);
                return mMatrixTmp;
            }

            public Matrix getMatrixInverse() {
                mMatrixInvertTmp.reset();
                getMatrix().invert(mMatrixInvertTmp);
                return mMatrixInvertTmp;
            }

            public void draw(Canvas canvas, Paint paint) {
                CommonLog.d(TAG + " draw");
                refreshBuffer(paint);
                canvas.drawBitmap(mBitmap, 0f, 0f, paint);
            }

            // 重新绘制缓冲区
            private void refreshBuffer(Paint paint) {
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
                    f1.onDraw(mBitmapCanvas, paint);
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
                    mDrawSteps.get(i).onDraw(mBitmapCanvas, mPaint);
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
                        lastFrameBitmap = Bitmap.createBitmap(mBitmapWidth, mBitmapHeight, Bitmap.Config.RGB_565);
                    }
                    new Canvas(lastFrameBitmap).drawBitmap(mBitmap, 0f, 0f, paint);
                    FrameDrawStep lastestFrame = new FrameDrawStep(getBrush(), drawStepSize - 2, lastFrameBitmap);

                    if (reuseLastFrame) {
                        mFrames.set(framesSize - 1, lastestFrame);
                    } else {
                        if (framesSize >= FRAMES_SIZE_MAX) {
                            // 关键帧过多，删除第一个，依次向前移动，新的关键帧放到最后一个位置
                            for (int i = 0; i < framesSize - 1; i++) {
                                mFrames.set(i, mFrames.get(i + 1));
                            }
                            mFrames.set(framesSize - 1, lastestFrame);
                        } else {
                            mFrames.add(lastestFrame);
                        }
                    }
                }

                // 绘画最后一个绘画步骤
                if (drawStepSize > 0) {
                    mDrawSteps.get(drawStepSize - 1).onDraw(mBitmapCanvas, paint);
                }
            }

            /**
             * 将手势结合当前画笔，处理为绘画绘画步骤
             */
            public void dispatchGestureAction(GestureAction gestureAction) {
                int drawStepSize = mDrawSteps.size();
                if (drawStepSize <= 0) {
                    // 第一个动作
                    DrawStep drawStep = DrawStep.create(gestureAction, getBrush());
                    if (drawStep == null) {
                        CommonLog.e(TAG + " dispatchGestureAction create draw step null.");
                        return;
                    }
                    mDrawSteps.add(drawStep);
                    return;
                }

                DrawStep lastDrawStep = mDrawSteps.get(drawStepSize - 1);
                if (lastDrawStep.dispatchGestureAction(gestureAction, getBrush())) {
                    // 当前绘画手势被最后一个绘画步骤继续处理
                    return;
                }

                // 开始一个新的绘画步骤
                DrawStep drawStep = DrawStep.create(gestureAction, getBrush());
                if (drawStep == null) {
                    CommonLog.e(TAG + " last draw step ignore current gesture action,  dispatchGestureAction create draw step null.");
                    return;
                }

                if (lastDrawStep instanceof EmptyDrawStep) {
                    // 最后一步是一个空步骤， 覆盖该空步骤
                    mDrawSteps.set(drawStepSize - 1, drawStep);
                } else {
                    mDrawSteps.add(drawStep);
                }
            }
        }

        /**
         * root 上的触摸事件
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

        public final int color; // 画刷的颜色
        public final int size; // 画刷的大小
        public final int type; // 画刷的类型

        public Brush(int color, int size, int type) {
            this.color = color;
            this.size = size;
            this.type = type;
        }

        public static Brush createPencil(int color, int size) {
            return new Brush(color, size, TYPE_PENCIL);
        }

        public static void mustPencil(Brush brush) {
            if (brush == null || brush.type != TYPE_PENCIL) {
                throw new BrushNotSupportException(brush);
            }
        }

        /**
         * 将当前画刷配置到指定画笔上
         */
        public void fillPaint(Paint paint) {
            paint.reset();
            paint.setColor(color);
            paint.setAntiAlias(true); // 抗锯齿
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

        public DrawStep(Brush drawBrush) {
            mDrawBrush = drawBrush;
        }

        @CallSuper
        public void onDraw(@NonNull Canvas canvas, @NonNull Paint paint) {
            CommonLog.d(TAG + " onDraw");
        }

        /**
         * 当前的绘画步骤是否继续消费该绘画手势，如果继续消费返回 true, 否则返回 false.
         */
        public boolean dispatchGestureAction(GestureAction gestureAction, Brush brush) {
            if (mDrawBrush != brush) {
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

            // TODO

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

        public FrameDrawStep(Brush drawBrush, int drawStepIndex, Bitmap bitmap) {
            super(drawBrush);
            mDrawStepIndex = drawStepIndex;
            mBitmap = bitmap;
        }

        @Override
        public void onDraw(@NonNull Canvas canvas, @NonNull Paint paint) {
            super.onDraw(canvas, paint);
            canvas.drawBitmap(mBitmap, 0f, 0f, paint);
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
        public void onDraw(@NonNull Canvas canvas, @NonNull Paint paint) {
            super.onDraw(canvas, paint);
            mDrawBrush.fillPaint(paint);
            canvas.drawCircle(mX, mY, mDrawBrush.size, paint);
        }
    }

}
