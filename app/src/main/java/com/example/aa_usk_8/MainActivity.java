package com.example.aa_usk_8;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.text.InputType;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;

    Button btnCommands, btnCalibrate, btnDetectAruco, btnSetMoveDuration;
    TextView txtRES, txtSSID, txtIP, txtCalibrationStatus, txtMoveDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCommands = findViewById(R.id.btnCommands);
        btnCalibrate = findViewById(R.id.btnCalibrate);
        btnDetectAruco = findViewById(R.id.btnDetectAruco);
        btnSetMoveDuration = findViewById(R.id.btnSetMoveDuration);
        txtRES = findViewById(R.id.txtRES);
        txtSSID = findViewById(R.id.txtSSID);
        txtIP = findViewById(R.id.txtIP);
        txtCalibrationStatus = findViewById(R.id.txtCalibrationStatus);
        txtMoveDuration = findViewById(R.id.txtMoveDuration);

        // Check and display calibration status
        checkCalibrationStatus();

        // Display current move duration
        displayMoveDuration();

        // Request necessary permissions
        if (checkAndRequestPermissions()) {
            startNetworkService();
        }

        btnCommands.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CommandsActivity.class);
            startActivity(intent);
        });

        btnCalibrate.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraCalibrationActivity.class);
            startActivity(intent);
        });

        btnDetectAruco.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DetectArucoActivity.class);
            startActivity(intent);
        });

        btnSetMoveDuration.setOnClickListener(v -> showMoveDurationDialog());
    }

    private void checkCalibrationStatus() {
        SharedPreferences prefs = getSharedPreferences("cameraCalibration", MODE_PRIVATE);
        boolean isCalibrated = prefs.getBoolean("isCalibrated", false);
        if (isCalibrated) {
            txtCalibrationStatus.setText("Camera is calibrated.");
        } else {
            txtCalibrationStatus.setText("Camera is not calibrated.");
        }
    }

    private void displayMoveDuration() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        int moveDuration = prefs.getInt("moveDuration", 3000); // Default to 3000ms
        txtMoveDuration.setText("Move Duration: " + moveDuration + " ms");
    }

    private boolean checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_NETWORK_STATE)
                        != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SETTINGS)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_NETWORK_STATE,
                            Manifest.permission.WRITE_SETTINGS
                    },
                    PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private void startNetworkService() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        NetworkRequest request = builder.build();
        connManager.requestNetwork(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    connManager.bindProcessToNetwork(network);
                }
                updateWifiInfo();
            }
        });
    }

    private void updateWifiInfo() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)
                == PackageManager.PERMISSION_GRANTED) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID();
            int ipAddress = wifiInfo.getIpAddress();
            String ip = String.format("%d.%d.%d.%d",
                    (ipAddress & 0xff),
                    (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff),
                    (ipAddress >> 24 & 0xff));

            // Update UI on the main thread
            runOnUiThread(() -> {
                txtSSID.setText("SSID: " + ssid);
                txtIP.setText("IP: " + ip);
            });
        }
    }

    private void showMoveDurationDialog() {
        Log.d("MainActivity", "showMoveDurationDialog called");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Move Duration (ms)");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            try {
                int moveDuration = Integer.parseInt(input.getText().toString());
                SharedPreferences.Editor editor = getSharedPreferences("settings", MODE_PRIVATE).edit();
                editor.putInt("moveDuration", moveDuration);
                editor.apply();
                Toast.makeText(this, "Move duration set to " + moveDuration + " ms", Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "Move duration set to " + moveDuration + " ms");
                displayMoveDuration(); // Update displayed move duration
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid input. Please enter a number.", Toast.LENGTH_SHORT).show();
                Log.d("MainActivity", "Invalid input for move duration");
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
            Log.d("MainActivity", "Move duration dialog canceled");
        });

        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startNetworkService();
            } else {
                Toast.makeText(this, "Permissions not granted. App may not function properly.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
