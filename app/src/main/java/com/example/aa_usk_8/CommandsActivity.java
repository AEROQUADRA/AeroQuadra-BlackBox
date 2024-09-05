package com.example.aa_usk_8;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CommandsActivity extends AppCompatActivity {

    Button btnForward, btnBackward, btnLeft, btnRight, btnStop;
    TextView txtResult, txtCommandStatus;

    private OkHttpClient client = new OkHttpClient();

    // Keys for shared preferences
    private static final String PREFS_NAME = "RobotSettings";
    private static final String KEY_ROTATE_LEFT_POWER = "rotateLeftPower";
    private static final String KEY_ROTATE_RIGHT_POWER = "rotateRightPower";
    private static final String KEY_MOVE_LEFT_POWER = "moveLeftPower";
    private static final String KEY_MOVE_RIGHT_POWER = "moveRightPower";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_commands);

        // Initialize buttons and text views
        btnForward = findViewById(R.id.btnForward);
        btnBackward = findViewById(R.id.btnBackward);
        btnLeft = findViewById(R.id.btnLeft);
        btnRight = findViewById(R.id.btnRight);
        btnStop = findViewById(R.id.btnStop);
        txtResult = findViewById(R.id.txtResult);
        txtCommandStatus = findViewById(R.id.txtCommandStatus);

        // Set up button listeners
        btnForward.setOnClickListener(view -> sendCommand("FORWARD"));
        btnBackward.setOnClickListener(view -> sendCommand("BACKWARD"));
        btnLeft.setOnClickListener(view -> sendCommand("LEFT"));
        btnRight.setOnClickListener(view -> sendCommand("RIGHT"));
        btnStop.setOnClickListener(view -> sendCommand("STOP"));
    }

    private void sendCommand(String cmd) {
        new Thread(() -> {
            // Retrieve saved power values from SharedPreferences
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            int rotateLeftPower = prefs.getInt(KEY_ROTATE_LEFT_POWER, 0);
            int rotateRightPower = prefs.getInt(KEY_ROTATE_RIGHT_POWER, 0);
            int moveLeftPower = prefs.getInt(KEY_MOVE_LEFT_POWER, 0);
            int moveRightPower = prefs.getInt(KEY_MOVE_RIGHT_POWER, 0);

            // Initialize left and right motor speeds
            int leftSpeed = 0;
            int rightSpeed = 0;

            // Determine motor speeds based on the command
            switch (cmd) {
                case "FORWARD":
                    leftSpeed = moveLeftPower;
                    rightSpeed = moveRightPower;
                    break;
                case "BACKWARD":
                    leftSpeed = moveLeftPower;
                    rightSpeed = moveRightPower;
                    break;
                case "LEFT":
                    leftSpeed = rotateLeftPower;
                    rightSpeed = rotateRightPower;
                    break;
                case "RIGHT":
                    leftSpeed = rotateRightPower;
                    rightSpeed = rotateLeftPower;
                    break;
                case "STOP":
                    leftSpeed = 0;
                    rightSpeed = 0;
                    break;
            }

            // Construct the command URL
            String command = "http://192.168.4.1/" + cmd + "?leftSpeed=" + leftSpeed + "&rightSpeed=" + rightSpeed;
            Log.d("Command", command);
            Request request = new Request.Builder().url(command).build();
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    final String myResponse = response.body().string();
                    runOnUiThread(() -> {
                        txtResult.setText(myResponse);
                        txtCommandStatus.setText("Command sent: " + cmd);
                    });
                } else {
                    runOnUiThread(() -> {
                        txtResult.setText("Error: " + response.code());
                        txtCommandStatus.setText("Failed to send command: " + cmd);
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    txtResult.setText("IOException: " + e.getMessage());
                    txtCommandStatus.setText("Failed to send command: " + cmd);
                });
            }
        }).start();
    }
}
