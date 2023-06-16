package com.team50.carcrashdetector;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentContainerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    FragmentContainerView imuContainerView, micContainerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imuContainerView = findViewById(R.id.IMUContainerView);
        micContainerView = findViewById(R.id.MicContainerView);

        getSupportFragmentManager().setFragmentResultListener("imu", this, (requestKey, result) -> {
            Log.d("MainActivity", "receive call_micOn msg from IMUFragment");
            int request = result.getInt("call_micOn", -1);
            Log.d("MainActivity", "received value: " + String.format("%d", request));
            if (request == 1) {
                micOn();
            }
        });

        getSupportFragmentManager().setFragmentResultListener("mic", this, (requestKey, result) -> {
            Log.d("MainActivity", "receive call_eme msg from MicFragment");
            int request = result.getInt("call_eme", -1);
            Log.d("MainActivity", "received value: " + String.format("%d", request));

            if (request == 1) {
                callEme();
                return;
            }

            Log.d("MainActivity", "receive call_imuOn msg from MicFragment");
            request = result.getInt("call_imuOn", -1);
            Log.d("MainActivity", "received value: " + String.format("%d", request));

            if (request == 1) {
                imuOn();
            }
        });

        initialization();
    }

    /* Begin two fragments. */
    /* Then, disable microphone sensing. */
    /* This is called at MainActivity's onCreate() callback method. */
    protected void initialization() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        } else {
            MicFragment micFragment = (MicFragment) getSupportFragmentManager().findFragmentById(R.id.MicContainerView);

            if (micFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .remove(micFragment)
                        .commit();
            }
            micContainerView.setVisibility(View.INVISIBLE);
        }
    }

    /* Begin MicFragment. */
    /* This is called by IMUFragment, through FragmentResult. */
    protected void micOn() {
        Log.d("MainActivity", "minOn() invoked");
        IMUFragment imuFragment = (IMUFragment) getSupportFragmentManager().findFragmentById(R.id.IMUContainerView);

        if (imuFragment != null)
            imuFragment.stopInferencing();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.MicContainerView, MicFragment.class, null)
                .commit();

        micContainerView.setVisibility(View.VISIBLE);
    }

    /* Begin IMUFragment. */
    /* This is called by MicFragment, through FragmentResult. */
    protected void imuOn() {
        MicFragment micFragment = (MicFragment) getSupportFragmentManager().findFragmentById(R.id.MicContainerView);
        IMUFragment imuFragment = (IMUFragment) getSupportFragmentManager().findFragmentById(R.id.IMUContainerView);

        if (imuFragment != null)
            imuFragment.startInferencing();

        if (micFragment != null)
            getSupportFragmentManager().beginTransaction()
                    .remove(micFragment)
                    .commit();

        micContainerView.setVisibility(View.INVISIBLE);
    }

    /* Call 911. Here, you can set your own emergency number. */
    /* This is called by MicFragment, through FragmentResult. */
    protected void callEme() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, 1);
        } else {
            /* Replace EMERGENCY_NUMBER_HERE */
            Intent callingIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:EMERGENCY_NUMBER_HERE"));
            startActivity(callingIntent);
        }
    }
}