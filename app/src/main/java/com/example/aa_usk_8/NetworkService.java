package com.example.aa_usk_8;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class NetworkService extends Service {

    private static final String TAG = "NetworkService";
    private static String connectedSSID = "N/A";
    private static String connectedIP = "N/A";

    @Override
    public void onCreate() {
        super.onCreate();

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        NetworkRequest request = builder.build();

        connManager.requestNetwork(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    connManager.bindProcessToNetwork(network);
                } else {
                    ConnectivityManager.setProcessDefaultNetwork(network);
                }
                updateWifiInfo();
            }

            @Override
            public void onLost(Network network) {
                Log.d(TAG, "Network disconnected: " + network.toString());
            }
        });
    }

    private void updateWifiInfo() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            connectedSSID = wifiInfo.getSSID();
            int ipAddress = wifiInfo.getIpAddress();
            connectedIP = String.format("%d.%d.%d.%d",
                    (ipAddress & 0xff),
                    (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff),
                    (ipAddress >> 24 & 0xff));
        }
    }

    public static String getConnectedSSID() {
        return connectedSSID;
    }

    public static String getConnectedIP() {
        return connectedIP;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
