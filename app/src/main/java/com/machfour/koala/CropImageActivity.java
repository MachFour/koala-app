package com.machfour.koala;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class CropImageActivity extends AppCompatActivity {
    private static final String TAG = "CropImageActivity";

    Uri imageUri;
    RectangleDrawView rectangleDrawView;

    private void loadScaledImage() {
        int imageW = rectangleDrawView.getWidth();
        int imageH = rectangleDrawView.getHeight();
        int orientation = Utils.getImageRotation(getContentResolver(), imageUri);
        Bitmap b = Utils.loadUnRotatedImageWithRotatedBounds(getContentResolver(), imageUri, imageW, imageH, orientation);
        if (b != null) {
            rectangleDrawView.setImageBitmap(b, orientation);
        } else {
            Log.w(TAG, "could not load image with Uri: " + imageUri);
            rectangleDrawView.setImageBitmap(null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        rectangleDrawView.post(this::loadScaledImage);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_crop_image, menu);
        return true;
    }

    public void onContinueButtonPressed(MenuItem i) {
        RectF userRect = rectangleDrawView.getScaledRect();
        // convert to image coordinates
        Intent processIntent = new Intent(this, ProcessImageActivity.class);
        processIntent.setData(imageUri);
        processIntent.putExtra(ProcessImageActivity.EXTRA_IMAGE_CROP_RECTF, userRect);
        startActivity(processIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_image);
        rectangleDrawView = findViewById(R.id.rectangleDraw);
        imageUri = getIntent().getData();
    }
}
