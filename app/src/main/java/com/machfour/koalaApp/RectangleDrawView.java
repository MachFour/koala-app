package com.machfour.koalaApp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.Locale;

// adapted from FingerPaint demo

public class RectangleDrawView extends View {
    private static final String TAG = "RectangleDrawView";
    private static final float TOUCH_TOLERANCE = 4;

    //private Bitmap blankBitmap;
    private final Paint mPaint;
    private final Rect currentRect;

    private Bitmap imageBitmap;
    private int imageWidth;
    private int imageHeight;
    private int imageRotation;
    private final Rect imageBounds;
    private final Rect viewBounds;

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
        viewBounds = new Rect(0, 0, getWidth(), getHeight());
        imageBounds = new Rect(0, 0, getWidth(), getHeight());
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


    private void updateImageBounds() {
        if (imageWidth == 0 || imageHeight == 0 || viewBounds.width() == 0 || viewBounds.height() == 0) {
            return;
        }
        double imgAspectRatio = imageWidth / (double) imageHeight;
        double viewAspectRatio = viewBounds.width()/viewBounds.height();
        if (viewAspectRatio > imgAspectRatio) {
            // view height is limiting factor. Scale image to have top and bottom filling the view
            imageBounds.top = viewBounds.top;
            imageBounds.bottom = viewBounds.bottom;
            double scaledImageHalfWidth = (viewBounds.height()*imgAspectRatio)/2.0;
            imageBounds.left = (int) (viewBounds.exactCenterX() - scaledImageHalfWidth);
            imageBounds.right = (int) (viewBounds.exactCenterX() + scaledImageHalfWidth);
        } else {
            // view width is limiting factor. Scale image to have left and right filling the view
            imageBounds.left = viewBounds.left;
            imageBounds.right = viewBounds.right;
            double scaledImageHalfHeight = (viewBounds.width()/imgAspectRatio)/2.0;
            imageBounds.top = (int) (viewBounds.exactCenterY() - scaledImageHalfHeight);
            imageBounds.bottom = (int) (viewBounds.exactCenterY() + scaledImageHalfHeight);
        }
        Log.d(TAG, String.format("new image bounds: l = %d, t = %d, r = %d, b = %d",
                imageBounds.left, imageBounds.top, imageBounds.right, imageBounds.bottom));
    }

    // stop it warning about nullability contract violations
    //@SuppressWarnings("ConstantConditions")
    public void setImageBitmap(Bitmap b, int rotation) {
        this.imageBitmap = Utils.rotateBitmap(b, rotation);
        this.imageWidth = imageBitmap.getWidth();
        this.imageHeight = imageBitmap.getHeight();
        this.imageRotation = rotation;

        // make bounds have the same aspect ratio as the bitmap
        updateImageBounds();
        invalidate();
    }

    public void setImageBitmap(Bitmap b) {
        setImageBitmap(b, 0);
    }

    @NonNull
    // returns currentRect with coordinates converted to proportions,
    // i.e. scaled by the width and height of current bounds
    // assumes bounds.top = bounds.left = 0
    RectF getScaledRect() {
        // TODO should this be viewBounds or imageBounds?
        float scaleWidth = imageBounds.width();
        float scaleHeight = imageBounds.height();
        if (scaleWidth > 0 && scaleHeight > 0) {
            float top = (currentRect.top - imageBounds.top)/scaleHeight;
            float bottom = (currentRect.bottom - imageBounds.top)/ scaleHeight;
            float left = (currentRect.left - imageBounds.left)/ scaleWidth;
            float right = (currentRect.right - imageBounds.left)/ scaleWidth;
            RectF scaledRect = new RectF(left, top, right, bottom);
            RectF rotatedScaledRect;
            if (imageRotation != 0) {
                rotatedScaledRect = new RectF();
                // have to 'unrotate' the rect to get it to work
                Matrix m = new Matrix();
                m.setRotate(-1*imageRotation, 0.5f, 0.5f);
                m.mapRect(rotatedScaledRect, scaledRect);
            } else {
                rotatedScaledRect = scaledRect;
            }
            Log.d(TAG, String.format("getScaledRect(): currentRect: %s. scaledRect: %s. rotatedScaledRect: %s",
                    currentRect.toString(), scaledRect.toString(), rotatedScaledRect.toString()));

            return rotatedScaledRect;
        } else {
            Log.w(TAG, "getScaledRect(): returning empty rect because of zero scale factor");
            return new RectF(0, 0, 0, 0);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //blankBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        viewBounds.right = w;
        viewBounds.bottom = h;
        Log.d(TAG, String.format("new view bounds: l = %d, t = %d, r = %d, b = %d", 0, 0, w, h));
        updateImageBounds();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // reset to nothing, so that we don't draw previous rectangles
        canvas.drawColor(0x00000000);
        if (imageBitmap != null) {
            //canvas.drawBitmap(imageBitmap, imageBounds, viewBounds, null);
            canvas.drawBitmap(imageBitmap, null, imageBounds, null);
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
        if (!currentRect.setIntersect(currentRect, imageBounds)) {
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
