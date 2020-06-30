package com.example.radarapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    EditText editTextUsername;
    EditText editTextPassword;
    Switch switchIsDoctor;
    Button buttonDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        switchIsDoctor = findViewById(R.id.doctor_switch);
        buttonDone = findViewById(R.id.button_done);

        setButton();
    }

    private void setButton() {

        buttonDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = editTextUsername.getText().toString();
                String password = editTextPassword.getText().toString();
                Boolean isDoctor = switchIsDoctor.isChecked();

                SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.app_data), MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(getString(R.string.app_data_username), username);
                editor.putString(getString(R.string.app_data_pwd), password);
                editor.putBoolean(getString(R.string.app_data_user_is_doctor), isDoctor);
                editor.commit();

                Intent intent;
                if(isDoctor) {
                    intent = new Intent(getApplicationContext(), MainActivityDoctor.class);
                } else {
                    intent = new Intent(getApplicationContext(), MainActivityClient.class);
                }

                startActivity(intent);

                //prevent the user from coming back to this activity
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        editAppStatus();
    }

    private void editAppStatus() {
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.app_data), MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(getString(R.string.app_data_first_lunch), false);
        editor.commit();
    }
}