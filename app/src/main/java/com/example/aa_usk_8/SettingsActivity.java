package com.example.aa_usk_8;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "RobotSettings";
    private static final String KEY_WHEEL_RPM = "wheelRPM";
    private static final String PREFS_SCALING = "scalingFactorData";
    private static final String KEY_SCALING_FACTOR = "scalingFactor";

    // New Keys for Rotate, Move Power, and Heading Adjustment Factor
    private static final String KEY_ROTATE_LEFT_POWER = "rotateLeftPower";
    private static final String KEY_ROTATE_RIGHT_POWER = "rotateRightPower";
    private static final String KEY_MOVE_LEFT_POWER = "moveLeftPower";
    private static final String KEY_MOVE_RIGHT_POWER = "moveRightPower";
    private static final String KEY_HEADING_ADJUSTMENT_FACTOR = "headingAdjustmentFactor";

    private EditText editTextWheelRPM, editTextScalingFactor;
    private EditText editTextRotateLeftPower, editTextRotateRightPower, editTextMoveLeftPower, editTextMoveRightPower;
    private EditText editTextHeadingAdjustmentFactor;
    private Button btnSaveAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize fields
        editTextWheelRPM = findViewById(R.id.editTextWheelRPM);
        editTextScalingFactor = findViewById(R.id.editTextScalingFactor);
        editTextRotateLeftPower = findViewById(R.id.editTextRotateLeftPower);
        editTextRotateRightPower = findViewById(R.id.editTextRotateRightPower);
        editTextMoveLeftPower = findViewById(R.id.editTextMoveLeftPower);
        editTextMoveRightPower = findViewById(R.id.editTextMoveRightPower);
        editTextHeadingAdjustmentFactor = findViewById(R.id.editTextHeadingAdjustmentFactor);
        btnSaveAll = findViewById(R.id.btnSaveAll);

        // Load saved values for all settings
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedRPM = prefs.getInt(KEY_WHEEL_RPM, 125);
        float savedScalingFactor = prefs.getFloat(KEY_SCALING_FACTOR, 0.62f);
        int rotateLeftPower = prefs.getInt(KEY_ROTATE_LEFT_POWER, 75);
        int rotateRightPower = prefs.getInt(KEY_ROTATE_RIGHT_POWER, 75);
        int moveLeftPower = prefs.getInt(KEY_MOVE_LEFT_POWER, 75);
        int moveRightPower = prefs.getInt(KEY_MOVE_RIGHT_POWER, 75);
        float headingAdjustmentFactor = prefs.getFloat(KEY_HEADING_ADJUSTMENT_FACTOR, 1.0f);

        // Set saved values to input fields
        editTextWheelRPM.setText(String.valueOf(savedRPM));
        editTextScalingFactor.setText(String.valueOf(savedScalingFactor));
        editTextRotateLeftPower.setText(String.valueOf(rotateLeftPower));
        editTextRotateRightPower.setText(String.valueOf(rotateRightPower));
        editTextMoveLeftPower.setText(String.valueOf(moveLeftPower));
        editTextMoveRightPower.setText(String.valueOf(moveRightPower));
        editTextHeadingAdjustmentFactor.setText(String.valueOf(headingAdjustmentFactor));

        // Save all settings when Save All button is clicked
        btnSaveAll.setOnClickListener(v -> saveAllSettings());
    }

    private void saveAllSettings() {
        // Get all input values
        String rpmString = editTextWheelRPM.getText().toString();
        String scalingFactorString = editTextScalingFactor.getText().toString();
        String rotateLeftString = editTextRotateLeftPower.getText().toString();
        String rotateRightString = editTextRotateRightPower.getText().toString();
        String moveLeftString = editTextMoveLeftPower.getText().toString();
        String moveRightString = editTextMoveRightPower.getText().toString();
        String headingAdjustmentFactorString = editTextHeadingAdjustmentFactor.getText().toString();

        // Validate inputs
        if (rpmString.isEmpty() || scalingFactorString.isEmpty() ||
                rotateLeftString.isEmpty() || rotateRightString.isEmpty() ||
                moveLeftString.isEmpty() || moveRightString.isEmpty() ||
                headingAdjustmentFactorString.isEmpty()) {
            Toast.makeText(this, "Please enter valid values for all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert strings to appropriate types
        int rpm = Integer.parseInt(rpmString);
        float scalingFactor = Float.parseFloat(scalingFactorString);
        int rotateLeftPower = Integer.parseInt(rotateLeftString);
        int rotateRightPower = Integer.parseInt(rotateRightString);
        int moveLeftPower = Integer.parseInt(moveLeftString);
        int moveRightPower = Integer.parseInt(moveRightString);
        float headingAdjustmentFactor = Float.parseFloat(headingAdjustmentFactorString);

        // Save all settings to SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_WHEEL_RPM, rpm);
        editor.putFloat(KEY_SCALING_FACTOR, scalingFactor);
        editor.putInt(KEY_ROTATE_LEFT_POWER, rotateLeftPower);
        editor.putInt(KEY_ROTATE_RIGHT_POWER, rotateRightPower);
        editor.putInt(KEY_MOVE_LEFT_POWER, moveLeftPower);
        editor.putInt(KEY_MOVE_RIGHT_POWER, moveRightPower);
        editor.putFloat(KEY_HEADING_ADJUSTMENT_FACTOR, headingAdjustmentFactor);
        editor.apply();

        Toast.makeText(this, "All settings saved successfully", Toast.LENGTH_SHORT).show();
    }
}
