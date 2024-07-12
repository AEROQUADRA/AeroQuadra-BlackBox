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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;

    Button btnStartDetect, btnCommands, btnCalibrate, btnDetectAruco;
    TextView txtRES, txtSSID, txtIP, txtCalibrationStatus;
    EditText edtMoveDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStartDetect = findViewById(R.id.btnStartDetect);
        btnCommands = findViewById(R.id.btnCommands);
        btnCalibrate = findViewById(R.id.btnCalibrate);
        btnDetectAruco = findViewById(R.id.btnDetectAruco);
        txtRES = findViewById(R.id.txtRES);
        txtSSID = findViewById(R.id.txtSSID);
        txtIP = findViewById(R.id.txtIP);
        txtCalibrationStatus = findViewById(R.id.txtCalibrationStatus);
        edtMoveDuration = findViewById(R.id.edtMoveDuration);

        // Check and display calibration status
        checkCalibrationStatus();

        // Request necessary permissions
        if (checkAndRequestPermissions()) {
            startNetworkService();
        }

        btnStartDetect.setOnClickListener(v -> {
//            Intent intent = new Intent(MainActivity.this, DetectActivity.class);
//            startActivity(intent);
        });

        btnCommands.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CommandsActivity.class);
            startActivity(intent);
        });

        btnCalibrate.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraCalibrationActivity.class);
            startActivity(intent);
        });

        btnDetectAruco.setOnClickListener(v -> {
            // Save the move duration to SharedPreferences
            String durationStr = edtMoveDuration.getText().toString();
            if (!durationStr.isEmpty()) {
                SharedPreferences prefs = getSharedPreferences("robotConfig", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("moveDuration", Integer.parseInt(durationStr));
                editor.apply();

                Intent intent = new Intent(MainActivity.this, DetectArucoActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Please enter a move duration", Toast.LENGTH_LONG).show();
            }
        });

        // Load the saved move duration
        SharedPreferences prefs = getSharedPreferences("robotConfig", MODE_PRIVATE);
        int savedDuration = prefs.getInt("moveDuration", 2000); // Default to 2000 ms
        edtMoveDuration.setText(String.valueOf(savedDuration));
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
