package com.team50.carcrashdetector.imuInference;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.opencsv.CSVWriter;
import com.team50.carcrashdetector.ml.Imumodel;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class CrashIMUClassifier {
    Imumodel classifier;
    SensorManager recorder;

    private DetectorListener detectorListener;
    private TimerTask task;
    private Sensor accSensor, gyrSensor;
    private AccSensorEventListener accListener;
    private GyrSensorEventListener gyrListener;

    private float accThreshold;
    private float gyrThreshold;

    /* Initialize the classifier with given thresholds. */
    public void initialize(Context context, float accThreshold, float gyrThreshold) {
        try {
            this.accThreshold = accThreshold;
            this.gyrThreshold = gyrThreshold;

            classifier = Imumodel.newInstance(context);
            imuInitialize(context);
            startRecording();
            startInferencing();
        } catch (IOException e) {
            Log.d("CrashIMUClassifier", "Load failed");
        }
    }

    protected File mFileDir;

    private void imuInitialize(Context context) {
        this.recorder = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.accSensor = recorder.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.accListener = new AccSensorEventListener();
        this.gyrSensor = recorder.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        this.gyrListener = new GyrSensorEventListener();

        mFileDir = context.getExternalFilesDir(null);
    }

    /* Define own SensorEventListener for the accelerometer. */
    /* It contains own sensor window. (Containing 30 consecutive values.) */
    static class AccSensorEventListener implements SensorEventListener {
        float[][] window = new float[30][3];

        public AccSensorEventListener() {
            float[] initializer = {0.0f, 0.0f, 0.0f};
            Arrays.fill(this.window, initializer);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float accX = event.values[0];
            float accY = event.values[1];
            float accZ = event.values[2];
            float[] window_item = {accX, accY, accZ};

            window = Arrays.copyOf(Arrays.copyOfRange(window, 1, 30), 30) ;
            window[29] = window_item;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {}
    }

    /* Define own SensorEventListener for the gyroscope. */
    /* It contains own sensor window. (Containing 30 consecutive values.) */
    static class GyrSensorEventListener implements SensorEventListener {
        float[][] window = new float[30][3];

        public GyrSensorEventListener() {
            float[] initializer = {0.0f, 0.0f, 0.0f};
            Arrays.fill(this.window, initializer);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float gyrX = event.values[0];
            float gyrY = event.values[1];
            float gyrZ = event.values[2];
            float[] window_item = {gyrX, gyrY, gyrZ};

            window = Arrays.copyOf(Arrays.copyOfRange(window, 1, 30), 30);
            window[29] = window_item;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {}
    }

    /* Begin to receive data from sensors. */
    private void startRecording() {
        Log.d("CrashIMUClassifier", "Records begins");
        recorder.registerListener(accListener, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        recorder.registerListener(gyrListener, gyrSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /* Stop to receive data from sensors. */
    private void stopRecording() {
        Log.d("CrashIMUClassifier", "Recording ends");
        recorder.unregisterListener(accListener);
        recorder.unregisterListener(gyrListener);
    }

    /* For the given window, calculate the average magnitude. */
    private float calculateAverage(float[][] window) {
        float sum = 0;

        for (float[] floats : window) {
            sum = sum + (float) (Math.pow(floats[0], 2) + Math.pow(floats[1], 2) + Math.pow(floats[2], 2));
        }

        return (sum / window.length);
    }

    /* Save collected data in csv format. */
    public void saveCSV() {
        if (mCSVWriter != null) {
            try {
                mCSVWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private CSVWriter mCSVWriter;

    public boolean inference() {
        float accCur = calculateAverage(Arrays.copyOfRange(this.accListener.window, 29, 30));
        float gyrCur = calculateAverage(Arrays.copyOfRange(this.gyrListener.window, 29, 30));

        // For data collection.
        /*
        if (mCSVWriter == null && mFileDir != null) {
            String fileName = "test.csv";

            try {
                Log.e("CrashIMUClassifier", mFileDir.toString());
                mCSVWriter = new CSVWriter(new FileWriter(new File(mFileDir, fileName), true));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mCSVWriter != null) {
            String[] csvData = {String.format("%d", System.currentTimeMillis()) , String.format("%f", this.accListener.window[32][0]), String.format("%f", this.accListener.window[32][1]), String.format("%f", this.accListener.window[32][2]),
                    String.format("%f", this.gyrListener.window[32][0]), String.format("%f", this.gyrListener.window[32][1]), String.format("%f", this.gyrListener.window[32][2])};
            mCSVWriter.writeNext(csvData);

            Log.d("CrashIMUClassifier", "Collecting...");
        }
         */

        // Log.d("CrashIMUClassifier", String.format("%f", accCur) + ", " + String.format("%f", gyrCur));

        /* If current values from sensors exceed the threshold, do inference. */
        if (accCur >= accThreshold && gyrCur >= gyrThreshold) {
            TensorBuffer input = TensorBuffer.createFixedSize(new int[]{1, 30, 6}, DataType.FLOAT32);
            float[][] concat = new float[30][6];
            float[] flattenedConcat = new float[30 * 6];

            for (int i = 0; i < 30; i++) {
                System.arraycopy(this.accListener.window[i], 0, concat[i], 0, 3);
                System.arraycopy(this.gyrListener.window[i], 0, concat[i], 3, 3);
            }

            for (int i = 0; i < 30; i++) {
                System.arraycopy(concat[i], 0, flattenedConcat, 6 * i, 6);
            }

            input.loadArray(flattenedConcat);
            TensorBuffer outputs = classifier.process(input).getOutputFeature0AsTensorBuffer();
            float[] scores = outputs.getFloatArray();

            // Binary classification: 0 - Not crash ~ 1 - Crash
            return scores[0] > 0.5;
        }
        return false;
    }

    /* Begin inferencing in a periodic manner. */
    public void startInferencing() {
        if (task == null) {
            Timer timer = new Timer();
            task = new TimerTask() {
                @Override
                public void run() {
                    detectorListener.onResults(inference());
                }
            };

            timer.scheduleAtFixedRate(task, 0, 33L);
        }
    }

    /* Stop inferencing. */
    public void stopInferencing() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public interface DetectorListener {
        void onResults(boolean isCrash);
    }

    public void setDetectorListener(DetectorListener listener) {
        this.detectorListener = listener;
    }
}
