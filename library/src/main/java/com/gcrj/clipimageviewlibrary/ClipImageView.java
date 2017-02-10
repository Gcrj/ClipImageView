package com.gcrj.clipimageviewlibrary;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * Created by zhangxin on 2017-1-16.
 */

public class ClipImageView extends View implements ScaleGestureDetector.OnScaleGestureListener {

    /**
     * 矩阵计算相关
     */
    private Matrix mMatrix;
    private float[] mMatrixValues;
    private RectF mCheckBorderRectF;

    /**
     * -----------------
     * ｜               ｜
     * ｜mRectBackStart ｜
     * ｜               ｜
     * ｜-------------- ｜
     * ｜               ｜
     * ｜   mRectClip   ｜
     * ｜               ｜
     * ｜-------------- ｜
     * ｜               ｜
     * ｜ mRectBackEnd  ｜
     * ｜               ｜
     * ----------------
     */
    private Rect mRectClip;//裁剪区
    private Rect mRectBackStart;//上（左）部阴影
    private Rect mRectBackEnd;//下（右）部阴影
    private Paint mPaintClip;
    private Paint mPaintBack;
    /**
     * 裁剪大小，单位px ({@link #clip()} 后mRectClip中的图片将缩放到mSize大小)
     */
    private int mSize = 150;

    private Bitmap mBitmap;

    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetector mGestureDetector;

    private float maxScale;
    /**
     * 最小初始化大小，当前缩放小于此值说明比裁剪框要小，才是松开触摸会恢复至minScale
     */
    private float minScale;
    /**
     * 初始化缩放大小，将传进来的bitmap等比例拉至屏幕大小（以短边为基准）
     */
    private float initScale;
    private float initX;
    private float initY;

    public ClipImageView(Context context) {
        super(context);
    }

    public ClipImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setClipSize(int size) {
        if (mSize == size) {
            return;
        }

        if (size < 0) {
            throw new IllegalArgumentException("Size can't less than 0");
        }

        mSize = size;
    }

    public void setImageResource(int resId) {
        setImageBitmap(BitmapFactory.decodeResource(getResources(), resId));
    }

    public void setImageBitmap(Bitmap bitmap) {
        if (mBitmap == bitmap) {
            return;
        }

        mBitmap = bitmap;
        post(new Runnable() {
            @Override
            public void run() {
                init();
            }
        });
    }

    private void init() {
        if (mMatrix == null) {
            mMatrix = new Matrix();
        }
        if (mMatrixValues == null) {
            mMatrixValues = new float[9];
        }
        if (mCheckBorderRectF == null) {
            mCheckBorderRectF = new RectF(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        }

        if (mPaintClip == null) {
            mPaintClip = new Paint();
            mPaintClip.setAntiAlias(true);
            mPaintClip.setStrokeWidth(3);
            mPaintClip.setColor(Color.WHITE);
            mPaintClip.setStyle(Paint.Style.STROKE);
        }
        if (mPaintBack == null) {
            mPaintBack = new Paint();
            mPaintBack.setColor(Color.parseColor("#3F000000"));
            mPaintBack.setStyle(Paint.Style.FILL);
        }

        if (mGestureDetector == null) {
            mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    if (getScale() >= maxScale) {
                        resetToInitScale();
                    } else {
                        mMatrix.postScale(2, 2, e.getX(), e.getY());
                    }
                    invalidate();
                    return true;
                }

                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                    mMatrix.postTranslate(-distanceX, -distanceY);
                    checkBorder();
                    invalidate();
                    return true;
                }
            });
        }
        if (mScaleGestureDetector == null) {
            mScaleGestureDetector = new ScaleGestureDetector(getContext(), this);
        }

        final int vWidth = getWidth();
        final int vHeight = getHeight();
        final int left;
        final int right;
        final int top;
        final int bottom;

        final int backStartRight;
        final int backStartBottom;
        final int backEndLeft;
        final int backEndTop;
        if (vWidth < vHeight) {
            left = 0;
            right = vWidth;
            top = (vHeight - vWidth) / 2;
            bottom = top + vWidth;

            backStartRight = vWidth;
            backStartBottom = top;
            backEndLeft = 0;
            backEndTop = bottom;
        } else {
            left = (vWidth - vHeight) / 2;
            right = left + vHeight;
            top = 0;
            bottom = vHeight;

            backStartRight = left;
            backStartBottom = vHeight;
            backEndLeft = right;
            backEndTop = 0;
        }
        mRectClip = new Rect(left, top, right, bottom);
        mRectBackStart = new Rect(0, 0, backStartRight, backStartBottom);
        mRectBackEnd = new Rect(backEndLeft, backEndTop, vWidth, vHeight);

        final int bWidth = mBitmap.getWidth();
        final int bHeight = mBitmap.getHeight();
        float scaleX = 1f * mRectClip.width() / bWidth;
        float scaleY = 1f * mRectClip.height() / bHeight;
        minScale = scaleX > scaleY ? scaleX : scaleY;

        //将图片等比例拉至屏幕大小（以短边为基准）
        scaleX = 1f * vWidth / bWidth;
        scaleY = 1f * vHeight / bHeight;
        initScale = scaleX > scaleY ? scaleX : scaleY;
        maxScale = initScale * 4;

        initX = vWidth / 2 - bWidth / 2;
        initY = vHeight / 2 - bHeight / 2;

        resetToInitScale();
        invalidate();
    }

    private void resetToInitScale() {
        mMatrix.reset();
        mMatrix.postTranslate(initX, initY);
        mMatrix.postScale(initScale, initScale, getWidth() / 2, getHeight() / 2);
    }

    /**
     * 边界检测
     */
    private void checkBorder() {
        //计算当前mMatrix下图片所在的实际位置和宽高
        mCheckBorderRectF.set(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        mMatrix.mapRect(mCheckBorderRectF);

        /**
         * 图片宽度比裁剪框宽度小时不做限制，因为这时候裁剪是不允许的，手松开时图片会恢复至minScale状态；
         * 图片宽度比裁剪框宽度大时，防止左右出框
         */
        if (mCheckBorderRectF.width() >= mRectClip.width()) {
            if (mCheckBorderRectF.left > mRectClip.left) {
                mMatrix.postTranslate(mRectClip.left - mCheckBorderRectF.left, 0);
            } else if (mCheckBorderRectF.right < mRectClip.right) {
                mMatrix.postTranslate(mRectClip.right - mCheckBorderRectF.right, 0);
            }
        }

        /**
         * 图片高度比裁剪框高度小时不做限制，因为这时候裁剪是不允许的，手松开时图片会恢复至minScale状态；
         * 图片高度比裁剪框高度大时，防止上下出框
         */
        if (mCheckBorderRectF.height() >= mRectClip.height()) {
            if (mCheckBorderRectF.top > mRectClip.top) {
                mMatrix.postTranslate(0, mRectClip.top - mCheckBorderRectF.top);
            } else if (mCheckBorderRectF.bottom < mRectClip.bottom) {
                mMatrix.postTranslate(0, mRectClip.bottom - mCheckBorderRectF.bottom);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap == null || mRectClip == null) {
            return;
        }

        canvas.drawBitmap(mBitmap, mMatrix, null);
        canvas.drawRect(mRectClip, mPaintClip);
        canvas.drawRect(mRectBackStart, mPaintBack);
        canvas.drawRect(mRectBackEnd, mPaintBack);
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scale = detector.getScaleFactor();
        if (scale == 1.0f || scale == 0f) {
            return true;
        }

        mMatrix.postScale(scale, scale, detector.getFocusX(), detector.getFocusY());
        checkBorder();
        invalidate();
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
    }

    public final float getScale() {
        mMatrix.getValues(mMatrixValues);
        return mMatrixValues[Matrix.MSCALE_X];
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleGestureDetector.onTouchEvent(event);
        if (!mScaleGestureDetector.isInProgress()) {
            mGestureDetector.onTouchEvent(event);
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getScale() < minScale) {
                    mMatrix.reset();
                    mMatrix.postScale(minScale, minScale, 0, 0);
                    mMatrix.postTranslate(mRectClip.left, mRectClip.top);
                    invalidate();
                }
        }

        return true;
    }

    /**
     * 获取当前裁剪框中的图片
     *
     * @return 裁剪框中的bitmap
     */
    public Bitmap clip() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.clipRect(mRectClip);
        draw(canvas);
        Bitmap clipBitmap = Bitmap.createBitmap(mSize, mSize, Bitmap.Config.ARGB_8888);
        Canvas clipCanvas = new Canvas(clipBitmap);
        clipCanvas.drawBitmap(bitmap, mRectClip, new Rect(0, 0, mSize, mSize), null);
        bitmap.recycle();
        return clipBitmap;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("bitmap", mBitmap);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bitmap bitmap = ((Bundle) state).getParcelable("bitmap");
            setImageBitmap(bitmap);
        }
        super.onRestoreInstanceState(state);
    }

}
