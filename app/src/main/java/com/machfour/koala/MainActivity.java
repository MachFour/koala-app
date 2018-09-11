package com.machfour.koala;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        */

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Snackbar s = Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG);
            s.setAction("Action", null);
            s.show();
        });
        Button b = findViewById(R.id.open_cv_button);
        b.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, OpenCVActivity.class);
            startActivity(intent);
        });

        Button b2 = findViewById(R.id.camera_button);
        b2.setOnClickListener(view -> {
             Intent intent = new Intent(MainActivity.this, CameraActivity.class);
             startActivity(intent);
        });

        Button b3 = findViewById(R.id.simple_camera_button);
        b3.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SimpleCameraActivity.class);
            startActivity(intent);
        });

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
