package com.example.aa_usk_8;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RotationActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private TextView realTimeHeadingTextView, constantHeadingTextView, markerInfoTextView;

    private long lastDetectionTime = 0;
    private static final long MIN_DETECT_INTERVAL_MS = 1000 / 30; // Minimum interval between detections (in milliseconds)
    private float initialAzimuth = 0; // Store the initial azimuth value
    private boolean moveActivityStarted = false; // Flag to track if MoveActivity has been started

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rotation);

        realTimeHeadingTextView = findViewById(R.id.headingTextView);
        constantHeadingTextView = findViewById(R.id.constantHeadingTextView);
        markerInfoTextView = findViewById(R.id.markerInfoTextView);

        // Initialize constant heading with a default value
        constantHeadingTextView.setText("Constant Heading: " + Math.round(initialAzimuth) + "°");

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }
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

            // Limit detection frequency to MIN_DETECT_INTERVAL_MS
            if (currentTime - lastDetectionTime < MIN_DETECT_INTERVAL_MS) {
                return; // Return without processing
            }

            lastDetectionTime = currentTime;

            float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

            float[] orientation = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientation);

            // Convert radians to degrees
            float azimuth = (float) Math.toDegrees(orientation[0]);
            azimuth = (azimuth + 360) % 360;

            // Update real-time heading
            final String realTimeHeading = "Real-Time Heading: " + Math.round(azimuth) + "°";
            runOnUiThread(() -> realTimeHeadingTextView.setText(realTimeHeading));

            // Update constant heading if it hasn't been updated yet
            if (initialAzimuth == 0) {
                initialAzimuth = azimuth;
                final String constantHeading = "Constant Heading: " + Math.round(initialAzimuth) + "°";
                runOnUiThread(() -> constantHeadingTextView.setText(constantHeading));
            }

            // Display detected marker info
            String detectedMarkerId = getIntent().getStringExtra("detectedMarkerId");
            if (detectedMarkerId != null) {
                int markerId = Integer.parseInt(detectedMarkerId); // Convert to integer

                // Calculate the required heading to align with the detected marker direction
                float requiredHeading = calculateRequiredHeading(markerId);

                // Check if current heading is within an acceptable range of the required heading
                if (Math.abs(azimuth - requiredHeading) <= 5 && !moveActivityStarted) {
                    startDetectArucoActivity();
                }

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
                        direction = ""; // If markerId is not 1-8, ignore it
                        break;
                }

                if (!direction.isEmpty()) {
                    markerInfoTextView.setText("Detected Marker ID: " + detectedMarkerId + "\nDirection: " + direction);
                } else {
                    markerInfoTextView.setText("Detected Marker ID: " + detectedMarkerId);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used in this example
    }

    private float calculateRequiredHeading(int markerId) {
        // Calculate the required heading based on the constant heading and the marker ID direction
        float constantHeading = initialAzimuth;
        switch (markerId) {
            case 1:
                return (constantHeading + 90) % 360; // East
            case 2:
                return (constantHeading + 135) % 360; // South East
            case 3:
                return (constantHeading + 180) % 360; // South
            case 4:
                return (constantHeading + 225) % 360; // South West
            case 5:
                return (constantHeading + 270) % 360; // West
            case 6:
                return (constantHeading + 315) % 360; // North West
            case 7:
                return constantHeading; // North
            case 8:
                return (constantHeading + 45) % 360; // North East
            default:
                return constantHeading; // Default to constant heading
        }
    }

    private void startDetectArucoActivity() {
        // Start DetectArucoActivity and prevent multiple starts
        moveActivityStarted = true;
        Intent intent = new Intent(this, DetectArucoActivity.class);
        startActivity(intent);
        finish(); // Finish RotationActivity so it's not kept in the back stack
    }

    public void restartMainActivity(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Finish RotationActivity so it's not kept in the back stack
    }
}
