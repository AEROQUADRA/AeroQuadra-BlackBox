package com.example.aa_usk_8;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MoveActivity extends AppCompatActivity {

    private static final String TAG = "MoveActivity";
    private TextView txtMovementStatus;
    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move);

        txtMovementStatus = findViewById(R.id.txtMovementStatus);

        // Send FORWARD command
        sendCommand("FORWARD");

        // Set duration to move forward (in milliseconds)
        int moveDuration = getMoveDuration();

        // Schedule STOP command and transition to next activity
        new Handler().postDelayed(() -> {
            sendCommand("STOP");
            startRotationActivity();
        }, moveDuration);
    }

    private int getMoveDuration() {
        // Retrieve move duration from SharedPreferences or use default value
        return getSharedPreferences("settings", MODE_PRIVATE).getInt("moveDuration", 3000); // Default to 3000ms
    }

    private void sendCommand(String cmd) {
        new Thread(() -> {
            String command = "http://192.168.4.1/" + cmd;
            Log.d(TAG, command);
            Request request = new Request.Builder().url(command).build();
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    final String myResponse = response.body().string();
                    runOnUiThread(() -> {
                        txtMovementStatus.setText("Command sent: " + cmd + "\nResponse: " + myResponse);
                        Toast.makeText(MoveActivity.this, "Command sent: " + cmd, Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> {
                        txtMovementStatus.setText("Error: " + response.code());
                        Toast.makeText(MoveActivity.this, "Failed to send command: " + cmd, Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    txtMovementStatus.setText("IOException: " + e.getMessage());
                    Toast.makeText(MoveActivity.this, "Failed to send command: " + cmd, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void startRotationActivity() {
        Intent intent = new Intent(this, RotationActivity.class);
        startActivity(intent);
        finish(); // Finish MoveActivity so it's not kept in the back stack
    }
}
