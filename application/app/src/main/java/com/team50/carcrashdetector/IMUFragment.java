package com.team50.carcrashdetector;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.team50.carcrashdetector.imuInference.CrashIMUClassifier;

public class IMUFragment extends Fragment implements CrashIMUClassifier.DetectorListener {
    CrashIMUClassifier crashIMUClassifier;
    TextView textViewIMUStatus;

    public IMUFragment() {
        // Required empty public constructor
    }

    public static IMUFragment newInstance() { return new IMUFragment(); }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_imu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d("IMUFragment", "View Created!");

        textViewIMUStatus = view.findViewById(R.id.textViewIMUStatus);
        // Assign classifier:
        crashIMUClassifier = new CrashIMUClassifier();
        // Initialize classifier:
        crashIMUClassifier.initialize(getContext(),150, 10);
        // Initialize classifier:
        crashIMUClassifier.setDetectorListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d("IMUFragment", "Paused!");

        // Stop classifier:
        crashIMUClassifier.stopInferencing();
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d("IMUFragment", "Resumed.");

        // Resume classifier:
        crashIMUClassifier.startInferencing();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // For data collection.
        crashIMUClassifier.saveCSV();

        Log.d("IMUFragment", "Destroyed!");
    }

    @Override
    public void onStop() {
        super.onStop();
        // For data collection.
        crashIMUClassifier.saveCSV();

        Log.d("IMUFragment", "Stopped!");
    }

    /* This function is invoked, when the classifier concludes its inferencing. */
    @Override
    public void onResults(boolean isCrash) {
        MainActivity mainActivity = (MainActivity) getActivity();

        if (mainActivity != null) {
            mainActivity.runOnUiThread(() -> {
                if (isCrash) {
                    textViewIMUStatus.setText(getResources().getText(R.string.imu_detected));
                    textViewIMUStatus.setTextColor(getResources().getColor(R.color.warning_red, getActivity().getTheme()));

                    // Stop classifier:
                    if (crashIMUClassifier != null)
                        crashIMUClassifier.stopInferencing();

                    if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                        Log.d("IMUFragment", "send call_micOn msg to MainActivity");
                        // Invoke MicOn() method of MainActivity.
                        Bundle res = new Bundle();
                        res.putInt("call_micOn", 1);
                        getParentFragmentManager().setFragmentResult("imu", res);
                    }
                }
            });
        }
    }

    public void startInferencing() {
        textViewIMUStatus.setText(getResources().getText(R.string.imu_safe));
        if (getActivity() != null)
            textViewIMUStatus.setTextColor(getResources().getColor(R.color.safe_green, getActivity().getTheme()));
        this.crashIMUClassifier.startInferencing();
    }

    public void stopInferencing() {
        this.crashIMUClassifier.stopInferencing();
    }
}