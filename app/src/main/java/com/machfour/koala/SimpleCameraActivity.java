package com.machfour.koala;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

import static com.machfour.koala.Config.FILE_PROVIDER_AUTHORITY;
import static com.machfour.koala.Config.IMAGE_CACHE_SUBDIR;

// Activity to choose or take a photo (intent fulfilled by another app) and then display it

/* References:
 * 1. stackoverflow.com/questions/4455558/allow-user-to-select-camera-or-gallery-for-image/12347567
 */
public class SimpleCameraActivity extends AppCompatActivity {
    static final int REQUEST_IMAGE_PICK = 2;
    static final String TAG = "SimpleCameraActivity";
    static final String INSTANCE_STATE_CAPTURE_URI = "capture_uri";
    static final String INSTANCE_STATE_IMAGE_URI = "image_uri";


    Uri captureURI;
    Uri imageURI;

    private void dispatchGetImageIntent() {
        // Intent to choose from filesystem
        Intent galleryIntent = Utils.makeGalleryIntent();

        // Intent to take a photo where to save camera image
        File captureFile = Utils.makeImageCaptureFile(this);
        captureURI = FileProvider.getUriForFile(this, Config.FILE_PROVIDER_AUTHORITY, captureFile);
        Parcelable[] captureIntents = Utils.makeImgCaptureIntents(getPackageManager(), captureURI);

        // choice of all intents
        String chooserTitle = "Select image source"; // TODO make @string resource
        Intent chooserIntent = Intent.createChooser(galleryIntent, chooserTitle)
                    .putExtra(Intent.EXTRA_INITIAL_INTENTS, captureIntents);

        startActivityForResult(chooserIntent, REQUEST_IMAGE_PICK);
    }

    private static boolean wasCameraCaptureIntent(Intent returnedData) {
        return returnedData == null || returnedData.getData() == null ||
                MediaStore.ACTION_IMAGE_CAPTURE.equals(returnedData.getAction());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case REQUEST_IMAGE_PICK:
                if (wasCameraCaptureIntent(data)) {
                    // capture intent
                    imageURI = captureURI;
                    if (imageURI == null) {
                        Log.e(TAG, "CaptureURI was null");
                    }
                } else {
                    // if the intent was to pick an image (rather than capture) then we need to get its URI
                    imageURI = data.getData();
                    if (imageURI == null) {
                        Log.e(TAG, "ACTION_PICK returned null data");
                    }
                }
                if (imageURI != null) {
                    Intent intent = new Intent(this, CropImageActivity.class);
                    intent.setData(imageURI);
                    startActivity(intent);
                } else {
                    Log.w(TAG, "Skipping setPic() with null URI");
                }
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (imageURI != null) {
            outState.putParcelable(INSTANCE_STATE_IMAGE_URI, imageURI);
        }
        if (captureURI != null) {
            outState.putParcelable(INSTANCE_STATE_CAPTURE_URI, captureURI);
        }

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        imageURI = savedInstanceState.getParcelable(INSTANCE_STATE_IMAGE_URI);
        captureURI = savedInstanceState.getParcelable(INSTANCE_STATE_CAPTURE_URI);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_camera);

        findViewById(R.id.choosePictureButton).setOnClickListener(view -> dispatchGetImageIntent());
    }
}
