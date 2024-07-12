package com.example.aa_usk_8;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MoveActivity extends AppCompatActivity {

    private TextView txtMoveInfo;
    private int moveDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move);

        txtMoveInfo = findViewById(R.id.txtMoveInfo);

        // Get move duration from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("robotConfig", MODE_PRIVATE);
        moveDuration = prefs.getInt("moveDuration", 2000); // Default to 2000 ms

        int detectedMarkerId = getIntent().getIntExtra("detectedMarkerId", -1);
        double distanceToMarker = getIntent().getDoubleExtra("distanceToMarker", 0);

        txtMoveInfo.setText("Moving to Marker ID: " + detectedMarkerId + "\nDistance: " + distanceToMarker + " cm");

        new Handler().postDelayed(() -> {
            Intent intent;
            if (detectedMarkerId == 0) {
                intent = new Intent(MoveActivity.this, ProgramEndsActivity.class);
            } else {
                intent = new Intent(MoveActivity.this, RotationActivity.class);
                intent.putExtra("detectedMarkerId", detectedMarkerId);
            }
            startActivity(intent);
            finish();
        }, moveDuration);
    }
}
