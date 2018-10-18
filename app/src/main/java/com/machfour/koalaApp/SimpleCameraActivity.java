package com.machfour.koalaApp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.util.Locale;

// Activity to choose or take a photo (intent fulfilled by another app) and then display it

/* References:
 * 1. stackoverflow.com/questions/4455558/allow-user-to-select-camera-or-gallery-for-image/12347567
 */
public class SimpleCameraActivity extends AppCompatActivity {
    static final int REQUEST_IMAGE_PICK = 2;
    static final String TAG = "SimpleCameraActivity";
    static final String INSTANCE_STATE_CAPTURE_URI = "capture_uri";
    static final String INSTANCE_STATE_IMAGE_URI = "image_uri";

    static final String[] permissionsNeeded;
    static final String[] permissionNames;

    static {
        if (Build.VERSION.SDK_INT >= 16) {
            permissionsNeeded = new String[] {
                      Manifest.permission.CAMERA
                    , Manifest.permission.READ_EXTERNAL_STORAGE
                    //, Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            permissionNames = new String[] {
                    "Use camera"
                    , "Read External Storage"
                    //, "Write external Storage"
            };
        } else {
            permissionsNeeded = new String[] { Manifest.permission.CAMERA };
            permissionNames = new String[] { "Use camera"};
        }

    }

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
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                // If request is cancelled, the result arrays are empty.
                for (int i = 0; i < grantResults.length; ++i) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        // same order as in the ActivityCompat.requestPermissions call
                        String message = String.format(Locale.ENGLISH, "Permission %d (%s) not granted", i, permissionNames[i]);
                        Log.w(TAG, message);
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    }

                }
                break;
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
        // Permissions for Android 6+
        ActivityCompat.requestPermissions(this, permissionsNeeded, 1);

        findViewById(R.id.choosePictureButton).setOnClickListener(view -> dispatchGetImageIntent());
    }
}
