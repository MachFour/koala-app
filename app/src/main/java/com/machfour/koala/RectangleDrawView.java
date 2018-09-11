package com.machfour.koala;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.BinderThread;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.Locale;

// adapted from FingerPaint demo

public class RectangleDrawView extends View {
    private static final String TAG = "RectangleDrawView";
    private static final float TOUCH_TOLERANCE = 4;

    private Bitmap blankBitmap;
    private Canvas mCanvas;
    private final Paint mPaint;
    private final Rect currentRect;

    private final Rect bounds;
    private Bitmap imageBitmap;
    private float mX, mY;
    private int rectStartX, rectStartY;

    public RectangleDrawView(Context c) {
        this(c, null);
    }

    public RectangleDrawView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RectangleDrawView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mPaint = makePaint();
        currentRect = new Rect(0, 0, 0, 0);
        bounds = new Rect(0, 0, getWidth(), getHeight());
        imageBitmap = null;
    }

    private static Paint makePaint() {
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setDither(true);
        p.setColor(0xFFFF0000);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(10);
        return p;
    }

    public void setImageBitmap(Bitmap b) {
        this.imageBitmap = b;
        invalidate();
    }

    @NonNull
    // returns currentRect with coordinates converted to proportions,
    // i.e. scaled by the width and height of current bounds
    // assumes bounds.top = bounds.left = 0
    RectF getScaledRect() {
        float scaleWidth = bounds.width();
        float scaleHeight = bounds.height();
        if (scaleWidth > 0 && scaleHeight > 0) {
            float top = currentRect.top/scaleHeight;
            float bottom = currentRect.bottom / scaleHeight;
            float left = currentRect.left / scaleWidth;
            float right = currentRect.right / scaleWidth;
            return new RectF(left, top, right, bottom);
        } else {
            Log.w(TAG, "getScaledRect(): returning empty rect because of zero scale factor");
            return new RectF(0, 0, 0, 0);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        blankBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(blankBitmap);
        bounds.right = w;
        bounds.bottom = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // reset to nothing, so that we don't draw previous rectangles
        canvas.drawColor(0x00000000);
        if (imageBitmap != null) {
            canvas.drawBitmap(imageBitmap, null, bounds, null);
        }
        if (!currentRect.isEmpty()) {
            canvas.drawRect(currentRect, mPaint);
        }
    }

    private void touch_start(float x, float y) {
        // new rect set
        resetRect(currentRect);
        rectStartX = (int) x;
        rectStartY = (int) y;
    }
    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mX = x;
            mY = y;
            adjustRectEnd();
        }
    }
    private void touch_up() {
        adjustRectEnd();
        // stop drawing
        Log.d("RectangleDrawView", String.format(Locale.getDefault(), "touch up at %.1f, %.1f", mX, mY));
        mCanvas.drawRect(currentRect, mPaint);
    }

    private static void resetRect(Rect r) {
        r.top = 0;
        r.bottom = 0;
        r.left = 0;
        r.right = 0;
    }

    private void adjustRectEnd() {
        int rectEndX = (int) mX;
        int rectEndY = (int) mY;

        int top = Math.min(rectStartY, rectEndY);
        int left = Math.min(rectStartX, rectEndX);
        int bottom = Math.max(rectStartY, rectEndY);
        int right = Math.max(rectStartX, rectEndX);
        currentRect.top = top;
        currentRect.bottom = bottom;
        currentRect.left = left;
        currentRect.right = right;
        // try to set currentRect within the bounds of the image.
        // if it lies entirely outside the bounds of the image, reset it to empty
        if (!currentRect.setIntersect(currentRect, bounds)) {
            resetRect(currentRect);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        // invalidate() causes the canvas to be redrawn
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }
        return true;
    }
}
