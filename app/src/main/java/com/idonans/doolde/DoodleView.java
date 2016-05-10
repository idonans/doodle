package com.idonans.doolde;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.TextureView;
import android.widget.FrameLayout;

import com.idonans.acommon.lang.CommonLog;
import com.idonans.acommon.lang.TaskQueue;

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
        mRender = new Render();

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
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_UP) {
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
            CommonLog.d(TAG + "onSurfaceTextureAvailable width:" + width + ", height:" + height);
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
            CommonLog.d(TAG + "onSurfaceTextureSizeChanged width:" + width + ", height:" + height);
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
            CommonLog.d(TAG + "onSurfaceTextureDestroyed");
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
            CommonLog.d(TAG + "onSurfaceTextureUpdated");
        }
    }

    private class Render implements Runnable {

        private boolean mEnable;
        private int mWidth;
        private int mHeight;
        private final TaskQueue mTaskQueue = new TaskQueue(1);

        private Bitmap mLastBitmap;

        private final Paint mPaint;

        private Render() {
            mPaint = new Paint();
            mPaint.setColor(Color.RED);
        }


        public void setEnable(boolean enable) {
            mEnable = enable;
        }

        public void setSize(int width, int height) {
            mWidth = width;
            mHeight = height;
        }

        public void postInvalidate() {
            mTaskQueue.enqueue(this);
        }

        @Override
        public void run() {
            if (!mEnable) {
                return;
            }

            if (mWidth <= 0 || mHeight <= 0) {
                return;
            }

            Canvas canvas = null;
            try {
                canvas = mTextureView.lockCanvas();
                if (canvas == null) {
                    return;
                }

                if (mLastBitmap != null) {
                    canvas.drawBitmap(mLastBitmap, -10, -10, null);
                }

                mPaint.setTextSize(System.currentTimeMillis() % 30 + 10);
                canvas.drawText("test doodle", 50, 50, mPaint);

                if (mLastBitmap == null) {
                    mLastBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
                }
                mLastBitmap = mTextureView.getBitmap(mLastBitmap);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (canvas != null) {
                    mTextureView.unlockCanvasAndPost(canvas);
                }
            }

        }

    }

}
