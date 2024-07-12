package com.example.aa_usk_8;

import android.content.Intent;
import android.content.SharedPreferences;
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

    private TextView countdownTextView, statusTextView;
    private OkHttpClient client = new OkHttpClient();
    private int moveDuration;
    private int detectedMarkerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move);

        countdownTextView = findViewById(R.id.countdownTextView);
        statusTextView = findViewById(R.id.statusTextView);

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        moveDuration = prefs.getInt("moveDuration", 3000);

        detectedMarkerId = getIntent().getIntExtra("detectedMarkerId", -1);

        if (detectedMarkerId == -1) {
            Toast.makeText(this, "No marker detected. Returning to DetectArucoActivity.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, DetectArucoActivity.class));
            finish();
            return;
        }

        startMove();
    }

    private void startMove() {
        if (detectedMarkerId == 0) {
            sendCommand("STOP");
            startProgramEndsActivity();
            return;
        }

        sendCommand("FORWARD");
        new CountDownTimer(moveDuration, 100) {

            public void onTick(long millisUntilFinished) {
                countdownTextView.setText("Moving: " + millisUntilFinished / 1000.0 + " seconds remaining");
            }

            public void onFinish() {
                sendCommand("STOP");
                startRotationActivity();
            }
        }.start();
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
}
