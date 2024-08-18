package com.example.aa_usk_8;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MoveActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "RobotSettings";
    private static final String KEY_WHEEL_RPM = "wheelRPM";

    private TextView countdownTextView, statusTextView, distanceTextView, debugTextView, detailsTextView;
    private OkHttpClient client = new OkHttpClient();
    private int detectedMarkerId;
    private double distanceToMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move);

        countdownTextView = findViewById(R.id.countdownTextView);
        statusTextView = findViewById(R.id.statusTextView);
        distanceTextView = findViewById(R.id.distanceTextView);
        debugTextView = findViewById(R.id.debugTextView);
        detailsTextView = findViewById(R.id.detailsTextView);

        detectedMarkerId = getIntent().getIntExtra("detectedMarkerId", -1);
        distanceToMarker = getIntent().getDoubleExtra("distanceToMarker", 0.0);

        if (detectedMarkerId == -1) {
            Toast.makeText(this, "No marker detected. Returning to DetectArucoActivity.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, DetectArucoActivity.class));
            finish();
            return;
        }

        if (detectedMarkerId == 0) {
            sendCommand("STOP");
            startProgramEndsActivity();
            return;
        }

        // Display the distance
        distanceTextView.setText(String.format("Distance to Marker: %.2f cm", distanceToMarker));

        int moveDuration = calculateMoveDuration(distanceToMarker);

        // Display the calculated move duration
        debugTextView.setText(String.format("Move Duration: %d ms", moveDuration));

        startMove(moveDuration);
    }

    private int calculateMoveDuration(double distance) {
        // Retrieve the wheel RPM from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int wheelRPM = prefs.getInt(KEY_WHEEL_RPM, 60); // Default RPM if not set

        // Wheel specifications
        double wheelDiameter = 43; // mm
        double wheelCircumference = Math.PI * wheelDiameter; // mm

        // Convert distance from cm to mm
        double distanceInMm = distance * 10;

        // Calculate the number of revolutions needed
        double wheelRevolutions = distanceInMm / wheelCircumference;

        // Calculate the time per revolution
        double wheelRevolutionTime = 60.0 / wheelRPM; // Time for one revolution in seconds

        // Log debug information
        Log.d("DEBUG", String.format("Distance in mm: %.2f", distanceInMm));
        Log.d("DEBUG", String.format("Wheel Circumference: %.2f", wheelCircumference));
        Log.d("DEBUG", String.format("Wheel Revolutions: %.2f", wheelRevolutions));
        Log.d("DEBUG", String.format("Wheel Revolution Time: %.2f seconds", wheelRevolutionTime));
        Log.d("DEBUG", String.format("Wheel RPM: %d", wheelRPM));

        // Display calculation details on the screen
        detailsTextView.setText(
                String.format("RPM: %d\nDiameter: %.2f mm\nCircumference: %.2f mm\nDistance in mm: %.2f\nRevolutions: %.2f\nTime per revolution: %.2f s",
                        wheelRPM, wheelDiameter, wheelCircumference, distanceInMm, wheelRevolutions, wheelRevolutionTime));

        // Calculate and return the total duration in milliseconds
        return (int) (wheelRevolutions * wheelRevolutionTime * 1000); // Convert to milliseconds
    }

    private void startMove(int duration) {
        if (isConnected()) {
            sendCommand("FORWARD");
            new CountDownTimer(duration, 100) {

                public void onTick(long millisUntilFinished) {
                    countdownTextView.setText("Moving: " + millisUntilFinished / 1000.0 + " seconds remaining");
                }

                public void onFinish() {
                    sendCommand("STOP");
                    startRotationActivity();
                }
            }.start();
        } else {
            statusTextView.setText("No network connection. Command not sent.");
        }
    }

    private void startProgramEndsActivity() {
        Intent intent = new Intent(this, ProgramEndsActivity.class);
        startActivity(intent);
        finish();
    }

    private void startRotationActivity() {
        Intent intent = new Intent(this, RotationActivity.class);
        intent.putExtra("detectedMarkerId", detectedMarkerId);
        startActivity(intent);
        finish();
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

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            Network network = cm.getActiveNetwork();
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        return false;
    }
}
