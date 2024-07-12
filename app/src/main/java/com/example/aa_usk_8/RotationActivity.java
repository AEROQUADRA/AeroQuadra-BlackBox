package com.example.aa_usk_8;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RotationActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private TextView realTimeHeadingTextView, constantHeadingTextView, markerInfoTextView;

    private long lastDetectionTime = 0;
    private static final long MIN_DETECT_INTERVAL_MS = 1000 / 30;
    private float initialAzimuth = -1; // Set initial value to -1 to indicate it hasn't been set
    private boolean rotationComplete = false;

    private int detectedMarkerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rotation);

        realTimeHeadingTextView = findViewById(R.id.headingTextView);
        constantHeadingTextView = findViewById(R.id.constantHeadingTextView);
        markerInfoTextView = findViewById(R.id.markerInfoTextView);

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

            if (initialAzimuth == -1) {
                initialAzimuth = azimuth;
                final String constantHeading = "Constant Heading: " + Math.round(initialAzimuth) + "°";
                runOnUiThread(() -> constantHeadingTextView.setText(constantHeading));
            }

            float requiredHeading = calculateRequiredHeading(detectedMarkerId);

            if (Math.abs(azimuth - requiredHeading) <= 5 && !rotationComplete) {
                rotationComplete = true;
                Toast.makeText(this, "Heading aligned. Moving to DetectArucoActivity.", Toast.LENGTH_SHORT).show();
                startDetectArucoActivity();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private float calculateRequiredHeading(int markerId) {
        float constantHeading = initialAzimuth;
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
}
