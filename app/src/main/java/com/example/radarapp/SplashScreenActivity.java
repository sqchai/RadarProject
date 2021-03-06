package com.example.radarapp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import gr.net.maroulis.library.EasySplashScreen;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EasySplashScreen easySplashScreen = new EasySplashScreen(SplashScreenActivity.this)
                .withFullScreen()
                .withSplashTimeOut(2000)
                .withLogo(R.drawable.ic_hr)
                .withAfterLogoText(getString(R.string.app_splash_after_logo))
                .withBackgroundColor(Color.WHITE);

        Boolean isFirstLaunch = checkAppStatus();
        if (isFirstLaunch) {
            easySplashScreen.withTargetActivity(WelcomeActivity.class);
        } else {
            SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.app_data), MODE_PRIVATE);
            Boolean isDoctor = sharedPreferences.getBoolean(getString(R.string.app_data_user_is_doctor), true);
            if (isDoctor) {
                easySplashScreen.withTargetActivity(MainActivityDoctor.class);
            } else {
                easySplashScreen.withTargetActivity(MainActivityClient.class);
            }
        }

        setContentView(easySplashScreen.create());
    }

    private Boolean checkAppStatus() {
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.app_data), MODE_PRIVATE);
        return sharedPreferences.getBoolean(getString(R.string.app_data_first_launch), true);
    }
}