package com.example.aa_usk_8;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MoveActivity extends AppCompatActivity {

    private EditText moveDurationEditText;
    private TextView moveDurationTextView;

    private SharedPreferences sharedPreferences;
    private static final String SHARED_PREFS_FILE = "MoveActivitySharedPrefs";
    private static final String MOVE_DURATION_KEY = "moveDuration";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move);

        moveDurationEditText = findViewById(R.id.moveDurationEditText);
        moveDurationTextView = findViewById(R.id.moveDurationTextView);

        sharedPreferences = getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        int savedDuration = sharedPreferences.getInt(MOVE_DURATION_KEY, 5000); // Default duration 5000 ms
        moveDurationEditText.setText(String.valueOf(savedDuration));
        moveDurationTextView.setText("Current Move Duration: " + savedDuration + " ms");
    }

    public void saveDuration(View view) {
        String durationStr = moveDurationEditText.getText().toString();
        if (!durationStr.isEmpty()) {
            int duration = Integer.parseInt(durationStr);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(MOVE_DURATION_KEY, duration);
            editor.apply();
            moveDurationTextView.setText("Current Move Duration: " + duration + " ms");
            Toast.makeText(this, "Move Duration Saved: " + duration + " ms", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please enter a valid duration", Toast.LENGTH_SHORT).show();
        }
    }

    public void restartMainActivity(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Finish ProgramEndsActivity so it's not kept in the back stack
    }
}
