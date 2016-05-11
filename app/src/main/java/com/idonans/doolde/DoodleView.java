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
import android.view.MotionEvent;
import android.view.TextureView;
import android.widget.FrameLayout;

import com.idonans.acommon.lang.CommonLog;
import com.idonans.acommon.lang.TaskQueue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;

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
        addView(mRootView, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        mTextureView.setSurfaceTextureListener(new TextureListener());
    }

    private class RootView extends FrameLayout {

        private final String TAG = "DoodleView$RootView";

        public RootView(Context context) {
            super(context);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            CommonLog.d(TAG + " onTouchEvent " + event);
            if (mRender.onTouchEvent(event)) {
                mRender.postInvalidate();
            }
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
            mRender.setEnable(true);
            mRender.setSize(width, height);
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
            mRender.setSize(width, height);
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
            mRender.setEnable(false);
            return false;
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

    private class Render {

        private boolean mEnable;
        private int mWidth;
        private int mHeight;
        private final TaskQueue mTaskQueue = new TaskQueue(1);

        private GestureDetectorCompat mGestureDetectorCompat;
        private LinkedList<Frame> mFrames = new LinkedList<>();
        private ArrayList<Action> mActions = new ArrayList<>();

        private final Paint mPaint;

        private Render(Context context) {
            mGestureDetectorCompat = new GestureDetectorCompat(context, new RenderGestureListener());
            mGestureDetectorCompat.setIsLongpressEnabled(false);

            mPaint = new Paint();
        }

        private class RenderGestureListener extends GestureDetector.SimpleOnGestureListener {

            private static final String TAG = "Render$RenderGestureListener";

            @Override
            public boolean onDown(MotionEvent e) {
                CommonLog.d(TAG + " onDown " + e);
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                CommonLog.d(TAG + " onSingleTapUp " + e);
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                CommonLog.d(TAG + " onSingleTapConfirmed " + e);
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                CommonLog.d(TAG + " onDoubleTap " + e);
                return true;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                CommonLog.d(TAG + " onDoubleTapEvent " + e);
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                CommonLog.d(TAG + " onScroll e1 " + e1 + ", e2 " + e2 + ", distanceX:" + distanceX + ", distanceY:" + distanceY);

                if (e2.getPointerCount() > 1) {
                    // 多指移动画布
                    mTextureView.setTranslationX(mTextureView.getTranslationX() - distanceX);
                    mTextureView.setTranslationY(mTextureView.getTranslationY() - distanceY);
                    /*
                    // 多指缩放画布
                    float x1 = MotionEventCompat.getX(e2, 0);
                    float y1 = MotionEventCompat.getY(e2, 0);
                    float x2 = MotionEventCompat.getX(e2, 1);
                    float y2 = MotionEventCompat.getY(e2, 1);

                    double ds = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
                    float scaleX = mTextureView.getScaleX();
                    */

                }

                return true;
            }

        }

        private class Draw implements Runnable {

            private static final String TAG = "Render$Draw";

            @Override
            public void run() {
                if (!mEnable) {
                    CommonLog.d(TAG + " enable is false, ignore");
                    return;
                }

                if (mWidth <= 0 || mHeight <= 0) {
                    CommonLog.d(TAG + " width or height invalid, ignore width:" + mWidth + ", height:" + mHeight);
                }

                Canvas canvas = null;
                try {
                    canvas = mTextureView.lockCanvas();
                    if (canvas == null) {
                        CommonLog.d(TAG + " canvas is null, ignore");
                        return;
                    }

                    // clear canvas
                    draw(canvas);

                    // test code
                    mPaint.setColor(Color.BLACK);
                    mPaint.setTextSize(30);
                    canvas.drawText("time:" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date()), 50, 200, mPaint);
                    mPaint.setColor(Color.DKGRAY);
                    mPaint.setTextSize(50);
                    canvas.drawText("test draw", 50, 400, mPaint);

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

        private abstract class Renderable {
            public abstract void onDraw(@NonNull Canvas canvas, @NonNull Paint paint);
        }

        /**
         * 帧图像
         */
        private class Frame extends Renderable {
            int actionIndex; // 该帧对应的 action index, 第一帧从 0 开始
            Bitmap bitmap; // 从起始到该 action index (包含) 所有动作绘画完成之后的图像

            @Override
            public void onDraw(@NonNull Canvas canvas, @NonNull Paint paint) {
                canvas.drawBitmap(bitmap, 0, 0, paint);
            }
        }

        /**
         * 绘画动作(单步)
         */
        private class Action extends Renderable {
            @Override
            public void onDraw(@NonNull Canvas canvas, @NonNull Paint paint) {

            }
        }

        public void setEnable(boolean enable) {
            mEnable = enable;
        }

        public void setSize(int width, int height) {
            mWidth = width;
            mHeight = height;
        }

        public boolean onTouchEvent(MotionEvent event) {
            return mGestureDetectorCompat.onTouchEvent(event);
        }

        private void draw(Canvas canvas) {
            // 清空背景
            canvas.drawColor(Color.WHITE);

            // 从上一个关键帧开始绘画
            int actionIndex = -1;

            // 绘画上一个关键帧
            Frame frame = mFrames.peekLast();
            if (frame != null) {
                actionIndex = frame.actionIndex;
                frame.onDraw(canvas, mPaint);
            }

            // 绘画该关键帧之后的所有动作
            int size = mActions.size();
            for (int i = actionIndex + 1; i < size; i++) {
                mActions.get(i).onDraw(canvas, mPaint);
            }
        }

    }

}
