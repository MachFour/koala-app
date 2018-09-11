package com.machfour.koala;

import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

public class ProcessImageActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGE_CROP_RECTF = "EXTRA_IMAGE_CROP_RECTF";

    private static final String TAG = "ProcessImageActivity";
    private static final String STATE_PROCESSING_DONE = "processing_done";

    static {
        System.loadLibrary("reference");
    }

    RectF cropRect;
    Uri imageUri;

    TextView processingText;
    ImageView processingImage;

    boolean processingDone;

    private void initViews() {
        processingImage = findViewById(R.id.processingImage);
        processingText = findViewById(R.id.processingText);
    }

    private BaseLoaderCallback _baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    // Load ndk built module, as specified in moduleName in build.gradle
                    // after opencv initialization
                    System.loadLibrary("native-lib");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
            }
        }
    };

    private Bitmap loadCroppedImage() {
        Bitmap b = Utils.loadImageSelection(getContentResolver(), imageUri, cropRect);
        if (b == null) {
            Log.w(TAG, "could not load image with Uri: " + imageUri);
        }
        processingImage.setImageBitmap(b);
        return b;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // if key is not present, don't change current value
        processingDone = savedInstanceState.getBoolean(STATE_PROCESSING_DONE, processingDone);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_PROCESSING_DONE, processingDone);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!processingDone) {
            Bitmap b = loadCroppedImage();
            //Table t = doProcessImage(b);
            doProcessImage(b);
            processingDone = true;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, _baseLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            _baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void doProcessImage(Bitmap b) {
        Mat toProcess = new Mat();
        org.opencv.android.Utils.bitmapToMat(b, toProcess, true);
        //Table t = doExtractTable(toProcess.getNativeObjAddr());
        doExtractTable(toProcess.getNativeObjAddr());
    }

    //public native Table doExtractTable(long matAddr);
    public native void doExtractTable(long matAddr);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process_image);

        initViews();

        imageUri = getIntent().getData();
        cropRect = getIntent().getParcelableExtra(EXTRA_IMAGE_CROP_RECTF);
        processingDone = false;


    }
}
