package com.example.aa_usk_8;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CommandsActivity extends AppCompatActivity {

    Button btnForward, btnBackward, btnLeft, btnRight;
    TextView txtResult, txtCommandStatus;

    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_commands);

        btnForward = findViewById(R.id.btnForward);
        btnBackward = findViewById(R.id.btnBackward);
        btnLeft = findViewById(R.id.btnLeft);
        btnRight = findViewById(R.id.btnRight);
        txtResult = findViewById(R.id.txtResult);
        txtCommandStatus = findViewById(R.id.txtCommandStatus);

        btnForward.setOnClickListener(view -> sendCommand("FORWARD"));
        btnBackward.setOnClickListener(view -> sendCommand("BACKWARD"));
        btnLeft.setOnClickListener(view -> sendCommand("LEFT"));
        btnRight.setOnClickListener(view -> sendCommand("RIGHT"));
    }

    private void sendCommand(String cmd) {
        new Thread(() -> {
            String command = "http://192.168.4.1/" + cmd;
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
