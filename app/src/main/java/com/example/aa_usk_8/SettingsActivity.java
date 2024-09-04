package com.example.aa_usk_8;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "RobotSettings";
    private static final String KEY_WHEEL_RPM = "wheelRPM";
    private static final String PREFS_SCALING = "scalingFactorData";
    private static final String KEY_SCALING_FACTOR = "scalingFactor";

    private EditText editTextWheelRPM, editTextScalingFactor;
    private Button btnSaveRPM, btnSaveScalingFactor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        editTextWheelRPM = findViewById(R.id.editTextWheelRPM);
        editTextScalingFactor = findViewById(R.id.editTextScalingFactor); // New input for scaling factor
        btnSaveRPM = findViewById(R.id.btnSaveRPM);
        btnSaveScalingFactor = findViewById(R.id.btnSaveScalingFactor); // New button to save scaling factor

        // Load saved RPM value
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedRPM = prefs.getInt(KEY_WHEEL_RPM, 0);
        editTextWheelRPM.setText(String.valueOf(savedRPM));

        // Load saved scaling factor value
        SharedPreferences scalingPrefs = getSharedPreferences(PREFS_SCALING, MODE_PRIVATE);
        float savedScalingFactor = scalingPrefs.getFloat(KEY_SCALING_FACTOR, 1.0f); // Default scaling factor is 1.0
        editTextScalingFactor.setText(String.valueOf(savedScalingFactor));

        // Save Wheel RPM
        btnSaveRPM.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveWheelRPM();
            }
        });

        // Save Scaling Factor
        btnSaveScalingFactor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveScalingFactor();
            }
        });
    }

    private void saveWheelRPM() {
        String rpmString = editTextWheelRPM.getText().toString();
        if (rpmString.isEmpty()) {
            Toast.makeText(this, "Please enter a valid RPM", Toast.LENGTH_SHORT).show();
            return;
        }

        int rpm = Integer.parseInt(rpmString);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_WHEEL_RPM, rpm);
        editor.apply();

        Toast.makeText(this, "Wheel RPM saved", Toast.LENGTH_SHORT).show();
    }

    private void saveScalingFactor() {
        String scalingFactorString = editTextScalingFactor.getText().toString();
        if (scalingFactorString.isEmpty()) {
            Toast.makeText(this, "Please enter a valid scaling factor", Toast.LENGTH_SHORT).show();
            return;
        }

        float scalingFactor = Float.parseFloat(scalingFactorString);

        SharedPreferences scalingPrefs = getSharedPreferences(PREFS_SCALING, MODE_PRIVATE);
        SharedPreferences.Editor editor = scalingPrefs.edit();
        editor.putFloat(KEY_SCALING_FACTOR, scalingFactor);
        editor.putBoolean("isScalingFactorCalculated", true); // Mark scaling factor as set
        editor.apply();

        Toast.makeText(this, "Scaling factor saved", Toast.LENGTH_SHORT).show();
    }
}
