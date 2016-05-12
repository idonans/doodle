package com.idonans.doolde;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.os.Build;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

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

    private Render mRender;
    private RootView mRootView;
    private TextureView mTextureView;

    private void init() {
        mRender = new Render(getContext());

        mRootView = new RootView(getContext());
        mTextureView = new TextureView(getContext());
        mRootView.addView(mTextureView, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        FrameLayout.LayoutParams rootViewLayouts = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        rootViewLayouts.gravity = Gravity.CENTER;
        addView(mRootView, rootViewLayouts);

        mTextureView.setSurfaceTextureListener(new TextureListener());
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
            super.onMeasure(
                    View.MeasureSpec.makeMeasureSpec(targetWidth, MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(targetHeight, MeasureSpec.EXACTLY));
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            CommonLog.d(TAG + " onTouchEvent " + event);
            mRender.onTouchEvent(event);
            return true;
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent event) {
            CommonLog.d(TAG + " onInterceptTouchEvent " + event);
            return true;
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            CommonLog.d(TAG + " dispatchTouchEvent " + event);
            return super.dispatchTouchEvent(event);
        }

        public void setAspectRadio(int aspectWidth, int aspectHeight) {

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

        private ScaleGestureDetector mScaleGestureDetector;
        private GestureDetectorCompat mGestureDetectorCompat;

        private static final float MAX_SCALE = 2.0f;
        private static final float MIN_SCALE = 0.75f;

        private final Paint mPaint;

        private Render(Context context) {
            mScaleGestureDetector = new ScaleGestureDetector(context, new RenderScaleGestureListener());
            mGestureDetectorCompat = new GestureDetectorCompat(context, new RenderGestureListener());
            mGestureDetectorCompat.setIsLongpressEnabled(false);

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
        }

        private class CanvasBuffer {

            private static final String TAG = "Render$CanvasBuffer";
            private static final int FRAMES_SIZE_MAX = 4;
            // 关键帧之间至多间隔的 action 数量
            private static final int FRAMES_STEP_INTERVAL_MAX = 8;
            // 关键帧缓存图像
            private final ArrayList<Frame> mFrames = new ArrayList<>(FRAMES_SIZE_MAX);
            private final ArrayList<Action> mActions = new ArrayList<>();

            private final Bitmap mBitmap; // 当前画布图像(绘画缓冲区)
            private final int mBitmapWidth; // 当前画布图像宽度
            private final int mBitmapHeight; // 当前画布图像高度
            private final Canvas mBitmapCanvas; // 原始画布

            public CanvasBuffer(int canvasWidth, int canvasHeight) {
                mBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888);
                mBitmapWidth = mBitmap.getWidth();
                mBitmapHeight = mBitmap.getHeight();
                mBitmapCanvas = new Canvas(mBitmap);
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
                Frame f1 = null; // 最后一个关键帧
                Frame f2 = null; // 倒数第二个关键帧
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

                int actionsSize = mActions.size();
                // 绘画最后一个关键帧之后除最后一个动作外的所有动作
                // 如果没有关键帧，则从第一个动作开始绘画
                boolean foundActionsAfterLastFrame = false;
                int actionIndexStart = -1;
                if (f1 != null) {
                    actionIndexStart = f1.actionIndex;
                }
                for (int i = actionIndexStart + 1; i < actionsSize - 1; i++) {
                    foundActionsAfterLastFrame = true;
                    mActions.get(i).onDraw(mBitmapCanvas, mPaint);
                }

                if (foundActionsAfterLastFrame) {
                    // 将目前的图像存储为一个新的关键帧(该关键帧与最终图像只差最后一个动作)

                    // 如果最后一个关键帧可以覆盖，则复用最后一个关键帧的内存
                    boolean reuseLastFrame = false;
                    if (f1 != null) {
                        if (f2 == null && f1.actionIndex < FRAMES_STEP_INTERVAL_MAX) {
                            reuseLastFrame = true;
                        } else if (f2 != null && f1.actionIndex - f2.actionIndex < FRAMES_STEP_INTERVAL_MAX) {
                            reuseLastFrame = true;
                        }
                    }

                    Bitmap lastFrameBitmap;
                    if (reuseLastFrame) {
                        lastFrameBitmap = f1.bitmap;
                    } else {
                        lastFrameBitmap = Bitmap.createBitmap(mBitmapWidth, mBitmapHeight, Bitmap.Config.RGB_565);
                    }
                    new Canvas(lastFrameBitmap).drawBitmap(mBitmap, 0f, 0f, paint);
                    Frame lastestFrame = new Frame(actionsSize - 1, lastFrameBitmap);

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

                // 绘画最后一个动作
                if (actionsSize > 0) {
                    mActions.get(actionsSize - 1).onDraw(mBitmapCanvas, paint);
                }
            }

            public void addAction(Action action) {
                mActions.add(action);
            }
        }

        @Override
        public boolean isAvailable() {
            return mTextureEnable && mCanvasBuffer != null;
        }

        private class RenderScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

            private static final String TAG = "Render$RenderScaleGestureListener";

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                CanvasBuffer canvasBuffer = mCanvasBuffer;
                if (!isAvailable()) {
                    return false;
                }

                CommonLog.d(TAG + " onScale scaleFactor");

                // 缩放画布
                int d = canvasBuffer.mBitmapWidth / 2;
                float pre = detector.getPreviousSpan();
                float cur = detector.getCurrentSpan();
                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();
                if (d <= 0 || pre <= 0 || cur <= 0 || focusX <= 0 || focusY <= 0 || focusX >= canvasBuffer.mBitmapWidth || focusY >= canvasBuffer.mBitmapHeight) {
                    CommonLog.d(TAG + " onScale ignore, d:" + d + ", pre:" + pre + ", cur:" + cur + ", focusX:" + focusX + ", focusY:" + focusY + ", width:" + canvasBuffer.mBitmapWidth + ", height:" + canvasBuffer.mBitmapHeight);
                    return false;
                }
                if (cur > pre) {
                    // 放大
                    float dx = cur - pre;
                    float ds = dx / d;
                    float textureScale = mTextureView.getScaleX();
                    float targetScale = textureScale + ds;
                    CommonLog.d(TAG + " scale up targetScale:" + targetScale + ", textureScale:" + textureScale + ", ds:" + ds + ", d:" + d + ", cur:" + cur + ", pre:" + pre + ", dx:" + dx);
                    if (targetScale > MAX_SCALE) {
                        targetScale = MAX_SCALE;
                    }
                    mTextureView.setScaleX(targetScale);
                    mTextureView.setScaleY(targetScale);
                } else if (cur < pre) {
                    // 缩小
                    float dx = pre - cur;
                    float ds = dx / d;
                    float textureScale = mTextureView.getScaleX();
                    float targetScale = textureScale - ds;
                    CommonLog.d(TAG + " scale down targetScale:" + targetScale + ", textureScale:" + textureScale + ", ds:" + ds + ", d:" + d + ", cur:" + cur + ", pre:" + pre + ", dx:" + dx);
                    if (targetScale < MIN_SCALE) {
                        targetScale = MIN_SCALE;
                    }
                    mTextureView.setScaleX(targetScale);
                    mTextureView.setScaleY(targetScale);
                }

                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                CanvasBuffer canvasBuffer = mCanvasBuffer;
                if (!isAvailable()) {
                    return false;
                }

                CommonLog.d(TAG + " onScaleBegin scaleFactor");

                // 开始缩放画布, 计算缩放点
                int d = canvasBuffer.mBitmapWidth / 2;
                float pre = detector.getPreviousSpan();
                float cur = detector.getCurrentSpan();
                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();
                if (d <= 0 || pre <= 0 || cur <= 0 || focusX <= 0 || focusY <= 0 || focusX >= canvasBuffer.mBitmapWidth || focusY >= canvasBuffer.mBitmapHeight) {
                    CommonLog.d(TAG + " onScaleBegin ignore, d:" + d + ", pre:" + pre + ", cur:" + cur + ", focusX:" + focusX + ", focusY:" + focusY + ", width:" + canvasBuffer.mBitmapWidth + ", height:" + canvasBuffer.mBitmapHeight);
                    return false;
                }

                mTextureView.setPivotX(focusX);
                mTextureView.setPivotY(focusY);
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                CommonLog.d(TAG + " onScaleEnd scaleFactor");
            }

        }

        private class RenderGestureListener implements GestureDetector.OnGestureListener {

            private static final String TAG = "Render$RenderGestureListener";

            @Override
            public boolean onDown(MotionEvent e) {
                CommonLog.d(TAG + " onDown " + e);
                return true;
            }

            @Override
            public void onShowPress(MotionEvent e) {
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                CommonLog.d(TAG + " onSingleTapUp " + e);
                // 换算坐标 将 root 上的坐标映射到 texture view 上

                float x = e.getX();
                float y = e.getY();
                float tx = mTextureView.getTranslationX();
                float ty = mTextureView.getTranslationY();
                float scale = mTextureView.getScaleX();
                int left = mTextureView.getLeft();
                int top = mTextureView.getTop();
                int right = mTextureView.getRight();
                int bottom = mTextureView.getBottom();
                float textureX = mTextureView.getX();
                float textureY = mTextureView.getY();
                int width = mTextureView.getWidth();
                int height = mTextureView.getHeight();

                CommonLog.d(TAG + " (" + x + ", " + y + ") mTextureView info tx:" + tx + ", ty:" + ty + ", scale:" + scale + ", ltrb[" + left + ", " + top + ", " + right + ", " + bottom + "], xy[" + textureX + ", " + textureY + "], wh[" + width + ", " + height + "]");

                mRender.enqueueAction(new PointAction(e.getX(), e.getY(), Color.RED, 30));
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                CommonLog.d(TAG + " onScroll e1 " + e1 + ", e2 " + e2 + ", distanceX:" + distanceX + ", distanceY:" + distanceY);

                if (e2.getPointerCount() > 1) {
                    // 多指移动画布
                    mTextureView.setTranslationX(mTextureView.getTranslationX() - distanceX);
                    mTextureView.setTranslationY(mTextureView.getTranslationY() - distanceY);
                }

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
                    canvasBuffer.draw(canvas, mPaint);

                    // 绘画测试内容
                    drawTest(canvas);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (canvas != null) {
                        mTextureView.unlockCanvasAndPost(canvas);
                    }
                }
            }

        }

        /**
         * test method
         */
        private void drawTest(Canvas canvas) {
            // test code
            mPaint.setColor(Color.BLACK);
            mPaint.setTextSize(30);
            canvas.drawText("time:" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date()), 50, 200, mPaint);
            mPaint.setColor(Color.DKGRAY);
            mPaint.setTextSize(50);
            canvas.drawText("test draw", 50, 400, mPaint);
        }

        public void postInvalidate() {
            mTaskQueue.enqueue(new Draw());
        }

        public void enqueueAction(final Action action) {
            mTaskQueue.enqueue(new Runnable() {
                @Override
                public void run() {
                    CanvasBuffer canvasBuffer = mCanvasBuffer;
                    if (!isAvailable()) {
                        CommonLog.d(TAG + " available is false, ignore action " + action);
                        return;
                    }
                    canvasBuffer.addAction(action);
                }
            });
            postInvalidate();
        }

        private abstract class Renderable {
            public abstract void onDraw(@NonNull Canvas canvas, @NonNull Paint paint);
        }

        /**
         * 帧图像
         */
        private class Frame extends Renderable {
            final int actionIndex; // 该帧对应的 action index, 第一帧从 0 开始
            final Bitmap bitmap; // 从起始到该 action index (包含) 所有动作绘画完成之后的图像

            private Frame(int actionIndex, Bitmap bitmap) {
                this.actionIndex = actionIndex;
                this.bitmap = bitmap;
            }

            @Override
            public void onDraw(@NonNull Canvas canvas, @NonNull Paint paint) {
                canvas.drawBitmap(bitmap, 0, 0, paint);
            }
        }

        /**
         * 绘画动作(单步)
         */
        private class Action extends Renderable {

            private String TAG = "Render$Action#" + getClass().getSimpleName();

            @Override
            public void onDraw(@NonNull Canvas canvas, @NonNull Paint paint) {
                CommonLog.d(TAG + " onDraw");
            }
        }

        private class PointAction extends Action {

            private final float mX;
            private final float mY;
            private final int mColor;
            private final int mSize;

            private PointAction(float x, float y, int color, int size) {
                mX = x;
                mY = y;
                mColor = color;
                mSize = size;
            }

            @Override
            public void onDraw(@NonNull Canvas canvas, @NonNull Paint paint) {
                super.onDraw(canvas, paint);
                paint.setColor(mColor);
                paint.setStrokeWidth(mSize);
                canvas.drawPoint(mX, mY, paint);
            }
        }

        public boolean onTouchEvent(MotionEvent event) {
            if (!isAvailable()) {
                return false;
            }

            mScaleGestureDetector.onTouchEvent(event);
            if (!mScaleGestureDetector.isInProgress()) {
                mGestureDetectorCompat.onTouchEvent(event);
            }
            return true;
        }

    }

}
