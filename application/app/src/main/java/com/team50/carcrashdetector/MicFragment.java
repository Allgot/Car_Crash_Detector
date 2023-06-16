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

import com.team50.carcrashdetector.micInference.CrashMicClassifier;

public class MicFragment extends Fragment implements CrashMicClassifier.DetectorListener {
    CrashMicClassifier crashMicClassifier;
    TextView textViewMicStatus;
    long startingTime;

    public MicFragment() {
        // Required empty public constructor
    }

    public static MicFragment newInstance() { return new MicFragment(); }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_mic, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d("MicFragment", "View created!");

        startingTime = System.currentTimeMillis();

        textViewMicStatus = view.findViewById(R.id.textViewMicStatus);
        // Assign classifier:
        crashMicClassifier = new CrashMicClassifier();
        // Initialize classifier:
        crashMicClassifier.initialize(getContext());
        // Initialize classifier:
        crashMicClassifier.setDetectorListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d("MicFragment", "Paused!");

        // Stop classifier:
        crashMicClassifier.stopInferencing();
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d("MicFragment", "Resumed.");

        // Resume classifier:
        crashMicClassifier.startInferencing();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d("MicFragment", "Destroyed!");
    }

    /* This function is invoked, when the classifier concludes its inferencing. */
    @Override
    public void onResults(boolean isCrash) {
        MainActivity mainActivity = (MainActivity) getActivity();

        if (mainActivity != null && getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            mainActivity.runOnUiThread(() -> {
                if (isCrash) {
                    textViewMicStatus.setText(getResources().getText(R.string.mic_detected));
                    textViewMicStatus.setTextColor(getResources().getColor(R.color.warning_red, getActivity().getTheme()));

                    if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                        // Call 911.
                        Bundle res = new Bundle();
                        res.putInt("call_eme", 1);
                        getParentFragmentManager().setFragmentResult("mic", res);
                    }
                } else if (System.currentTimeMillis() - startingTime > 10000) {
                    if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                        Log.d("MicFragment", "send call_imuOn msg to MainActivity");
                        // Invoke imuOn() method of MainActivity.
                        Bundle res = new Bundle();
                        res.putInt("call_imuOn", 1);
                        getParentFragmentManager().setFragmentResult("mic", res);
                    }
                }
            });
        }
    }
}