package com.example.aa_usk_8;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RotationActivity extends AppCompatActivity implements SensorEventListener {

    private static final String PREFS_NAME = "RotationActivityPrefs";
    private static final String KEY_CONSTANT_HEADING = "constantHeading";
    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private TextView realTimeHeadingTextView, constantHeadingTextView, markerInfoTextView, statusTextView;
    private OkHttpClient client = new OkHttpClient();

    private long lastDetectionTime = 0;
    private static final long MIN_DETECT_INTERVAL_MS = 1000 / 30;
    private boolean rotationComplete = false;

    private int detectedMarkerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rotation);

        realTimeHeadingTextView = findViewById(R.id.headingTextView);
        constantHeadingTextView = findViewById(R.id.constantHeadingTextView);
        markerInfoTextView = findViewById(R.id.markerInfoTextView);
        statusTextView = findViewById(R.id.statusTextView);

        detectedMarkerId = getIntent().getIntExtra("detectedMarkerId", -1);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }

        if (detectedMarkerId == -1) {
            Toast.makeText(this, "No marker detected. Returning to DetectArucoActivity.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, DetectArucoActivity.class));
            finish();
            return;
        }

        displayMarkerInfo(detectedMarkerId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            Toast.makeText(this, "Rotation vector sensor not available.", Toast.LENGTH_SHORT).show();
            realTimeHeadingTextView.setText("Rotation vector sensor not available");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastDetectionTime < MIN_DETECT_INTERVAL_MS) {
                return;
            }

            lastDetectionTime = currentTime;

            float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

            float[] orientation = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientation);

            float azimuth = (float) Math.toDegrees(orientation[0]);
            azimuth = (azimuth + 360) % 360;

            final String realTimeHeading = "Real-Time Heading: " + Math.round(azimuth) + "°";
            runOnUiThread(() -> realTimeHeadingTextView.setText(realTimeHeading));

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            float constantHeading = prefs.getFloat(KEY_CONSTANT_HEADING, -1);

            if (constantHeading == -1) {
                // Constant heading is not set, set it now
                constantHeading = azimuth;
                prefs.edit().putFloat(KEY_CONSTANT_HEADING, constantHeading).apply();
                final String constantHeadingStr = "Constant Heading: " + Math.round(constantHeading) + "°";
                runOnUiThread(() -> constantHeadingTextView.setText(constantHeadingStr));
            } else {
                final String constantHeadingStr = "Constant Heading: " + Math.round(constantHeading) + "°";
                runOnUiThread(() -> constantHeadingTextView.setText(constantHeadingStr));
            }

            float requiredHeading = calculateRequiredHeading(constantHeading, detectedMarkerId);

            if (Math.abs(azimuth - requiredHeading) <= 5 && !rotationComplete) {
                rotationComplete = true;
                Toast.makeText(this, "Heading aligned. Moving to DetectArucoActivity.", Toast.LENGTH_SHORT).show();
                sendCommand("STOP"); // Send STOP command
                startDetectArucoActivity();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private float calculateRequiredHeading(float constantHeading, int markerId) {
        switch (markerId) {
            case 1:
                return (constantHeading + 90) % 360;
            case 2:
                return (constantHeading + 135) % 360;
            case 3:
                return (constantHeading + 180) % 360;
            case 4:
                return (constantHeading + 225) % 360;
            case 5:
                return (constantHeading + 270) % 360;
            case 6:
                return (constantHeading + 315) % 360;
            case 7:
                return constantHeading;
            case 8:
                return (constantHeading + 45) % 360;
            default:
                return constantHeading;
        }
    }

    private void startDetectArucoActivity() {
        Intent intent = new Intent(this, DetectArucoActivity.class);
        startActivity(intent);
        finish();
    }

    private void displayMarkerInfo(int markerId) {
        String direction;
        switch (markerId) {
            case 1:
                direction = "East";
                break;
            case 2:
                direction = "South East";
                break;
            case 3:
                direction = "South";
                break;
            case 4:
                direction = "South West";
                break;
            case 5:
                direction = "West";
                break;
            case 6:
                direction = "North West";
                break;
            case 7:
                direction = "North";
                break;
            case 8:
                direction = "North East";
                break;
            default:
                direction = "Unknown";
                break;
        }
        markerInfoTextView.setText("Detected Marker ID: " + markerId + "\nDirection: " + direction);
    }

    private void sendCommand(String cmd) {
        new Thread(() -> {
            String command = "http://192.168.4.1/" + cmd;
            Log.d("Command", command);
            Request request = new Request.Builder().url(command).build();
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    runOnUiThread(() -> statusTextView.setText("Command sent: " + cmd));
                } else {
                    runOnUiThread(() -> statusTextView.setText("Failed to send command: " + cmd));
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> statusTextView.setText("IOException: " + e.getMessage()));
            }
        }).start();
    }
}
