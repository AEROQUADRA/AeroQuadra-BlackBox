package com.example.aa_usk_8;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
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
    private TextView realTimeHeadingTextView, constantHeadingTextView, markerInfoTextView, statusTextView, headingDifferenceTextView;
    private OkHttpClient client = new OkHttpClient();

    private long lastDetectionTime = 0;
    private static final long MIN_DETECT_INTERVAL_MS = 1000 / 30;
    private boolean rotationComplete = false;
    private boolean rotatingRight = false;
    private boolean rotatingLeft = false; // New flags for locking direction

    private int detectedMarkerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rotation);

        // Initialize TextViews
        realTimeHeadingTextView = findViewById(R.id.headingTextView);
        constantHeadingTextView = findViewById(R.id.constantHeadingTextView);
        markerInfoTextView = findViewById(R.id.markerInfoTextView);
        statusTextView = findViewById(R.id.statusTextView);
        headingDifferenceTextView = findViewById(R.id.headingDifferenceTextView);

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
                return; // Ensure that we don't detect too frequently
            }

            lastDetectionTime = currentTime;

            // Get the rotation matrix and orientation from the sensor event
            float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

            float[] orientation = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientation);

            // Convert azimuth to degrees (current real-time heading)
            float azimuth = (float) Math.toDegrees(orientation[0]);
            azimuth = (azimuth + 360) % 360; // Normalize azimuth to 0-360 degrees

            // Update the real-time heading display
            final String realTimeHeading = "Real-Time Heading: " + Math.round(azimuth) + "°";
            runOnUiThread(() -> realTimeHeadingTextView.setText(realTimeHeading));

            // Retrieve the constant heading from preferences
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            float constantHeading = prefs.getFloat(KEY_CONSTANT_HEADING, -1);

            if (constantHeading == -1) {
                // If constant heading is not set, set it to the current azimuth
                constantHeading = azimuth;
                prefs.edit().putFloat(KEY_CONSTANT_HEADING, constantHeading).apply();
                final String constantHeadingStr = "Constant Heading: " + Math.round(constantHeading) + "°";
                runOnUiThread(() -> constantHeadingTextView.setText(constantHeadingStr));
            } else {
                final String constantHeadingStr = "Constant Heading: " + Math.round(constantHeading) + "°";
                runOnUiThread(() -> constantHeadingTextView.setText(constantHeadingStr));
            }

            // Calculate the required heading based on the detected marker ID
            float requiredHeading = calculateRequiredHeading(constantHeading, detectedMarkerId);

            // Calculate the heading difference
            float headingDifference = requiredHeading - azimuth;

            // Normalize the heading difference to [-180, 180] degrees
            if (headingDifference > 180) {
                headingDifference -= 360;
            } else if (headingDifference < -180) {
                headingDifference += 360;
            }

            // Update the Heading Difference TextView
            final String headingDifferenceStr = "Heading Difference: " + Math.round(headingDifference) + "°";
            runOnUiThread(() -> headingDifferenceTextView.setText(headingDifferenceStr));

            // Align the robot within a tolerance of 5 degrees
            if (Math.abs(headingDifference) <= 5 && !rotationComplete) {
                // If aligned, stop the rotation
                rotationComplete = true;
                sendCommand("STOP");
                Toast.makeText(this, "Heading aligned with marker.", Toast.LENGTH_SHORT).show();

                // Reset direction flags
                rotatingRight = false;
                rotatingLeft = false;

                // Proceed to the next step or activity
                proceedToNextStep();
            } else if (!rotationComplete) {
                // Determine and lock the direction (only choose once)
                if (!rotatingRight && !rotatingLeft) {
                    if (headingDifference > 0) {
                        rotatingRight = true;
                        rotatingLeft = false;
                    } else {
                        rotatingLeft = true;
                        rotatingRight = false;
                    }
                }

                // Continue rotating in the chosen direction
                if (rotatingRight) {
                    sendCommand("RIGHT");
                } else if (rotatingLeft) {
                    sendCommand("LEFT");
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No action needed for accuracy changes
    }

    private float calculateRequiredHeading(float constantHeading, int markerId) {
        switch (markerId) {
            case 1: // East
                return (constantHeading + 90) % 360;
            case 2: // South-East
                return (constantHeading + 135) % 360;
            case 3: // South
                return (constantHeading + 180) % 360;
            case 4: // South-West
                return (constantHeading + 225) % 360;
            case 5: // West
                return (constantHeading + 270) % 360;
            case 6: // North-West
                return (constantHeading + 315) % 360;
            case 7: // North
                return constantHeading % 360; // This is the constant heading itself
            case 8: // North-East
                return (constantHeading + 45) % 360;
            default:
                return constantHeading; // Default to constant heading if marker ID is unknown
        }
    }

    private void proceedToNextStep() {
        // Move to the next activity or restart the detection process
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
