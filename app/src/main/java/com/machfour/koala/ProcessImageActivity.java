package com.machfour.koala;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.io.File;

public class ProcessImageActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGE_CROP_RECTF = "EXTRA_IMAGE_CROP_RECTF";

    private static final String TAG = "ProcessImageActivity";
    private static final String STATE_PROCESSING_DONE = "processing_done";
    private static final String STATE_PROCESSING_RESULT = "processing_result";

    static {
        System.loadLibrary("reference");
        System.loadLibrary("native-lib");
    }

    RectF cropRect;
    Uri imageUri;

    TableLayout processingTable;
    ImageView processingImage;

    Bitmap image;
    File tessDataDir;
    File tessConfigFile;

    boolean processingDone;
    String processingResult;

    private void initViews() {
        processingImage = findViewById(R.id.processingImage);
        processingTable = findViewById(R.id.processingTable);
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

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // if key is not present, don't change current value
        processingDone = savedInstanceState.getBoolean(STATE_PROCESSING_DONE, processingDone);
        processingResult = savedInstanceState.getString(STATE_PROCESSING_RESULT, processingResult);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_PROCESSING_DONE, processingDone);
        outState.putString(STATE_PROCESSING_RESULT, processingResult);
    }

    @Override
    protected void onStart() {
        super.onStart();
        image = loadCroppedImage();

        if (processingResult != null) {
            showProcessingResult(processingResult);
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
        if (processingDone) {
            showProcessingResult(processingResult);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_process_image, menu);
        return true;
    }

    public void onStartProcessing(MenuItem i) {
        if (!processingDone) {
            doProcessImage(image);
            Toast.makeText(this, "doProcessImage() finished", Toast.LENGTH_SHORT).show();
            showProcessingResult(processingResult);
        }
    }

    public void showProcessingResult(String tableString) {
        Table extracted = Table.parseFromString(tableString);
        populateTable(extracted);
    }

    private Bitmap loadCroppedImage() {
        int rotation = Utils.getImageRotation(getContentResolver(), imageUri);
        Bitmap b = Utils.loadImageSelection(getContentResolver(), imageUri, cropRect);
        b = Utils.rotateBitmap(b, rotation);
        if (b == null) {
            Log.w(TAG, "could not load image with Uri: " + imageUri);
        }
        processingImage.setImageBitmap(b);
        return b;
    }

    public void doProcessImage(Bitmap b) {
        Mat toProcess = new Mat();
        org.opencv.android.Utils.bitmapToMat(b, toProcess, true);

        // TODO be able to return error code
        processingResult = doExtractTable(toProcess.getNativeObjAddr(), tessDataDir.toString(), tessConfigFile.toString());
        Log.d(TAG, "doProcessImage() finished");
        processingDone = true;
    }

    //public native Table doExtractTable(long matAddr);
    public native String doExtractTable(long matAddr, String tessdataPath, String tessConfigFile);


    private void populateTable(Table t) {

         //* Converting dp to pixels in code
        int padding_in_dp = 6;  // 6 dps
        final float scale = getResources().getDisplayMetrics().density;
        int tablePadding = (int) (padding_in_dp * scale + 0.5f);

        for (int i = 0; i < t.getRows(); ++i) {
            TableRow row = new TableRow(this);
            TableRow.LayoutParams rowParams = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            rowParams.setMargins(tablePadding, tablePadding, tablePadding, tablePadding);
            row.setLayoutParams(rowParams);

            TableLayout.LayoutParams params = new TableLayout.LayoutParams();
            params.setMargins(tablePadding, tablePadding, tablePadding, tablePadding);
            //row.setPadding(tablePadding, tablePadding, tablePadding, tablePadding);
            row.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
            row.setDividerDrawable(Utils.getDrawable(getResources(), R.drawable.dark_divider_shape));
            for (int j = 0; j < t.getCols(); ++j) {
                String cellText = " " + t.getText(i, j).trim() + " ";
                TextView v = new TextView(this);
                v.setText(cellText);
                row.addView(v, j);
            }
            processingTable.addView(row, params);
        /*
        View newRowView = getLayoutInflater().inflate(R.layout.food_details_table_row, tableToAddTo, false);
        // even though the IDs are the same in each loop iteration, since the inflated views
        // are not immediately attached to view tree, the findViewById will still work
        TableRow newRow = newRowView.findViewById(R.id.table_row);
        TextView keyText = newRowView.findViewById(R.id.keyText);
        TextView valueText = newRowView.findViewById(R.id.valueText);
        */
    }


}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process_image);
        initViews();

        imageUri = getIntent().getData();
        cropRect = getIntent().getParcelableExtra(EXTRA_IMAGE_CROP_RECTF);
        if (cropRect == null || cropRect.isEmpty()) {
            // 'null crop' - gets whole image
            cropRect = new RectF(0.0f, 0.0f, 1.0f, 1.0f);
        }
        image = null;
        processingDone = false;
        tessDataDir = Utils.getTessDataDir(this);
        tessConfigFile = Utils.getTessConfigFile(this);
        Log.d(TAG, "tessDataDir: " + tessDataDir.getAbsolutePath());
        Log.d(TAG, "tessConfigFile: " + tessConfigFile.getAbsolutePath());


    }
}
