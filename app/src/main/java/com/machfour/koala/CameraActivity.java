package com.machfour.koala;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

/* source:
 * https://github.com/googlesamples/android-Camera2Basic/
 * -> /Application/src/main/java/com/example/android/camera2basic/CameraActivity.java
 */

public class CameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera2BasicFragment.newInstance())
                    .commit();
        }
    }

}
