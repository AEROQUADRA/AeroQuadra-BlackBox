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

    // New Keys for Rotate and Move Power
    private static final String KEY_ROTATE_LEFT_POWER = "rotateLeftPower";
    private static final String KEY_ROTATE_RIGHT_POWER = "rotateRightPower";
    private static final String KEY_MOVE_LEFT_POWER = "moveLeftPower";
    private static final String KEY_MOVE_RIGHT_POWER = "moveRightPower";

    private EditText editTextWheelRPM, editTextScalingFactor;
    private EditText editTextRotateLeftPower, editTextRotateRightPower, editTextMoveLeftPower, editTextMoveRightPower;
    private Button btnSaveRPM, btnSaveScalingFactor, btnSavePowers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize existing fields
        editTextWheelRPM = findViewById(R.id.editTextWheelRPM);
        editTextScalingFactor = findViewById(R.id.editTextScalingFactor);
        btnSaveRPM = findViewById(R.id.btnSaveRPM);
        btnSaveScalingFactor = findViewById(R.id.btnSaveScalingFactor);

        // Initialize new fields
        editTextRotateLeftPower = findViewById(R.id.editTextRotateLeftPower);
        editTextRotateRightPower = findViewById(R.id.editTextRotateRightPower);
        editTextMoveLeftPower = findViewById(R.id.editTextMoveLeftPower);
        editTextMoveRightPower = findViewById(R.id.editTextMoveRightPower);
        btnSavePowers = findViewById(R.id.btnSavePowers);

        // Load saved values for all settings
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedRPM = prefs.getInt(KEY_WHEEL_RPM, 125);
        float savedScalingFactor = prefs.getFloat(KEY_SCALING_FACTOR, 0.62f);
        int rotateLeftPower = prefs.getInt(KEY_ROTATE_LEFT_POWER, 75);
        int rotateRightPower = prefs.getInt(KEY_ROTATE_RIGHT_POWER, 75);
        int moveLeftPower = prefs.getInt(KEY_MOVE_LEFT_POWER, 75);
        int moveRightPower = prefs.getInt(KEY_MOVE_RIGHT_POWER, 75);

        // Set saved values to input fields
        editTextWheelRPM.setText(String.valueOf(savedRPM));
        editTextScalingFactor.setText(String.valueOf(savedScalingFactor));
        editTextRotateLeftPower.setText(String.valueOf(rotateLeftPower));
        editTextRotateRightPower.setText(String.valueOf(rotateRightPower));
        editTextMoveLeftPower.setText(String.valueOf(moveLeftPower));
        editTextMoveRightPower.setText(String.valueOf(moveRightPower));

        // Save Wheel RPM
        btnSaveRPM.setOnClickListener(v -> saveWheelRPM());

        // Save Scaling Factor
        btnSaveScalingFactor.setOnClickListener(v -> saveScalingFactor());

        // Save Powers for Rotate and Move
        btnSavePowers.setOnClickListener(v -> savePowers());
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

    private void savePowers() {
        String rotateLeftString = editTextRotateLeftPower.getText().toString();
        String rotateRightString = editTextRotateRightPower.getText().toString();
        String moveLeftString = editTextMoveLeftPower.getText().toString();
        String moveRightString = editTextMoveRightPower.getText().toString();

        if (rotateLeftString.isEmpty() || rotateRightString.isEmpty() || moveLeftString.isEmpty() || moveRightString.isEmpty()) {
            Toast.makeText(this, "Please enter valid values for all powers", Toast.LENGTH_SHORT).show();
            return;
        }

        int rotateLeftPower = Integer.parseInt(rotateLeftString);
        int rotateRightPower = Integer.parseInt(rotateRightString);
        int moveLeftPower = Integer.parseInt(moveLeftString);
        int moveRightPower = Integer.parseInt(moveRightString);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_ROTATE_LEFT_POWER, rotateLeftPower);
        editor.putInt(KEY_ROTATE_RIGHT_POWER, rotateRightPower);
        editor.putInt(KEY_MOVE_LEFT_POWER, moveLeftPower);
        editor.putInt(KEY_MOVE_RIGHT_POWER, moveRightPower);
        editor.apply();

        Toast.makeText(this, "Powers saved", Toast.LENGTH_SHORT).show();
    }
}
