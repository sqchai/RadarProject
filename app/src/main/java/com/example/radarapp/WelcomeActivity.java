package com.example.radarapp;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        editAppStatus();
    }

    private void editAppStatus() {
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.app_data), MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(getString(R.string.app_data_first_lunch), false);
        editor.commit();
    }
}