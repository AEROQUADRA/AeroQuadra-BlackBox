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

    private EditText editTextWheelRPM;
    private Button btnSaveRPM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        editTextWheelRPM = findViewById(R.id.editTextWheelRPM);
        btnSaveRPM = findViewById(R.id.btnSaveRPM);

        // Load the saved RPM value
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedRPM = prefs.getInt(KEY_WHEEL_RPM, 0);
        editTextWheelRPM.setText(String.valueOf(savedRPM));

        btnSaveRPM.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveWheelRPM();
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
        finish();
    }
}
