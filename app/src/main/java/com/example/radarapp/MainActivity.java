package com.example.radarapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;

public abstract class MainActivity extends AppCompatActivity {
    int saveDatabufPointer = 0;
    int[] saveDataIPos;
    int[] saveDataINeg;
    int[] saveDataQPos;
    int[] saveDataQNeg;
    int[] saveDataECG;
    int SAVE_DATABUF_SIZE;
    int LIST_DATABUF_SIZE;

    int[] ipos, ineg, qpos, qneg, idiff, qdiff, ecg;
    Handler uiUpdateHandler;

    abstract void updateConnMethodTextView();
    abstract void showToast(String text, int duration);
    abstract void saveDataToFile();


}
