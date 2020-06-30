package com.example.radarapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivityDoctor extends AppCompatActivity {
    class UpdateDataTimerTask extends TimerTask {
        UpdateDataTimerTask() {
            // Get the display data buffer.
            ipos = iPosNegWaveView.getListData();
            ineg = iPosNegWaveView.getListData(false);
            qpos = qPosNegWaveView.getListData();
            qneg = qPosNegWaveView.getListData(false);
            idiff = iDiffWaveView.getListData();
            qdiff = qDiffWaveView.getListData();
            ecg = ecgWaveView.getListData();
        }

        @SuppressLint("DefaultLocale")
        private void wifiMethod() {
            String webContent;
            // webContent = DebugWebContentGen.debugWebGen();

            try {
                StringBuilder result = new StringBuilder();
                URLConnection connection = new URL(getURL()).openConnection();
                connection.setReadTimeout(resources.getInteger(R.integer.TIMEOUT));
                try (InputStreamReader input = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                    int ch;
                    while ((ch = input.read()) != -1) {
                        result.append((char) ch);
                    }
                }
                webContent = result.toString().trim();
            } catch (SocketTimeoutException e) {
                showToast("Connection Timeout! The system clear!", Toast.LENGTH_LONG);
                uiUpdateHandler.sendEmptyMessage(R.integer.MSG_SYSTEM_RESET);
                return;
            } catch (IOException e) {
                showToast("Exception occurred: " + e.getMessage(), Toast.LENGTH_LONG);
                uiUpdateHandler.sendEmptyMessage(R.integer.MSG_SYSTEM_RESET);
                return;
            }

            // At this point, the content has successfully been loaded.

            isFreqChanged = false;
            if ("No Data".equals(webContent.substring(0, 7))) {
                return;
            }
            int missingRecords = -1;
            for (int index = webContent.length() - 1; index > 0; index--) {
                if (webContent.charAt(index) == ' ') {
                    missingRecords = Integer.parseInt(webContent.substring(index + 1));
                    break;
                }
            }
            int numOfNewRecords = (webContent.length() - (missingRecords + "").length() - 8) / 25;

            if (missingRecords != 0) {
                showToast(String.format("Missing %d records!", missingRecords), Toast.LENGTH_SHORT);
            }

            // The buffer pointer and records pointer.
            int bufPointer, recordsPointer;

            // If the number of the new records are bigger than the list buffer size.
            if (numOfNewRecords >= LIST_DATABUF_SIZE) {
                AxesView.setListDatabufPointer(LIST_DATABUF_SIZE);
                bufPointer = 0;
                recordsPointer = numOfNewRecords - LIST_DATABUF_SIZE;
            } else {
                // Move the current records to leave room for the new records.
                // Execute the code if needed.
                if (AxesView.getListDatabufPointer() + numOfNewRecords > LIST_DATABUF_SIZE) {
                    int moveFromWhere = AxesView.getListDatabufPointer() + numOfNewRecords - LIST_DATABUF_SIZE;
                    for (int newPointer = 0, oldPointer = moveFromWhere; oldPointer < AxesView.getListDatabufPointer(); newPointer++, oldPointer++) {
                        ipos[newPointer] = ipos[oldPointer];
                        ineg[newPointer] = ineg[oldPointer];
                        qpos[newPointer] = qpos[oldPointer];
                        qneg[newPointer] = qneg[oldPointer];
                        idiff[newPointer] = idiff[oldPointer];
                        qdiff[newPointer] = qdiff[oldPointer];
                        ecg[newPointer] = ecg[oldPointer];
                    }
                    bufPointer = LIST_DATABUF_SIZE - numOfNewRecords;
                    recordsPointer = 0;
                    AxesView.setListDatabufPointer(LIST_DATABUF_SIZE);
                } else {
                    bufPointer = AxesView.getListDatabufPointer();
                    recordsPointer = 0;
                    AxesView.setListDatabufPointer(AxesView.getListDatabufPointer() + numOfNewRecords);
                }
            }

            // Records assignment.
            for (int bufPointer0 = bufPointer; recordsPointer < numOfNewRecords; bufPointer0++, recordsPointer++) {
                ipos[bufPointer0] = Integer.parseInt(webContent.substring(25 * recordsPointer, 25 * recordsPointer + 4));
                ineg[bufPointer0] = Integer.parseInt(webContent.substring(25 * recordsPointer + 5, 25 * recordsPointer + 5 + 4));
                qpos[bufPointer0] = Integer.parseInt(webContent.substring(25 * recordsPointer + 5 * 2, 25 * recordsPointer + 5 * 2 + 4));
                qneg[bufPointer0] = Integer.parseInt(webContent.substring(25 * recordsPointer + 5 * 3, 25 * recordsPointer + 5 * 3 + 4));
                ecg[bufPointer0] = Integer.parseInt(webContent.substring(25 * recordsPointer + 5 * 4, 25 * recordsPointer + 5 * 4 + 4));
                idiff[bufPointer0] = ipos[bufPointer0] - ineg[bufPointer0];
                qdiff[bufPointer0] = qpos[bufPointer0] - qneg[bufPointer0];
            }

            // If save buffers full, save it to file.
            if (saveDatabufPointer + numOfNewRecords > SAVE_DATABUF_SIZE) {
                saveDataToFile();
            }

            for (recordsPointer = 0; recordsPointer < numOfNewRecords; saveDatabufPointer++, recordsPointer++, bufPointer++) {
                saveDataIPos[saveDatabufPointer] = ipos[bufPointer];
                saveDataINeg[saveDatabufPointer] = ineg[bufPointer];
                saveDataQPos[saveDatabufPointer] = qpos[bufPointer];
                saveDataQNeg[saveDatabufPointer] = qneg[bufPointer];
                saveDataECG[saveDatabufPointer] = ecg[bufPointer];
            }
        }

        public void run() {
            if (isStart) {
                if (connectionToolbox.connectionMethod == ConnectionToolbox.CONNECTION_WIFI) {
                    wifiMethod();
                } else if (connectionToolbox.connectionMethod == ConnectionToolbox.CONNECTION_BLUETOOTH) {
                    if (isFreqChanged) {
                        connectionToolbox.sendMessage("CS" + csEditText.getText().toString());
                        connectionToolbox.sendMessage("R0" + Integer.parseInt(r0EditText.getText().toString().substring(2), 16));
                        connectionToolbox.sendMessage("R1" + Integer.parseInt(r1EditText.getText().toString().substring(2), 16));
                        connectionToolbox.sendMessage("R2" + Integer.parseInt(r2EditText.getText().toString().substring(2), 16));
                        connectionToolbox.sendMessage("R3" + Integer.parseInt(r3EditText.getText().toString().substring(2), 16));
                        connectionToolbox.sendMessage("R4" + Integer.parseInt(r4EditText.getText().toString().substring(2), 16));
                        connectionToolbox.sendMessage("R5" + Integer.parseInt(r5EditText.getText().toString().substring(2), 16));
                        isFreqChanged = false;
                    }
                }
                // Draw the figure.
                uiUpdateHandler.sendEmptyMessage(R.integer.MSG_UPDATE_WAVES);
            }
        }
    }

    UpdateDataTimerTask updateDataTimerTask;

    private class SeekBarTools {
        private SeekBar seekBar;

        private int min, value, max;

        SeekBarTools(SeekBar seekBar, int min, int value, int max) {
            this.seekBar = seekBar;
            this.min = min;
            this.value = value;
            this.max = max;
            setMax0(max - min);
            setProgress0(value - min);
        }

        int getMin() {
            return min;
        }

        int getMax() {
            return max;
        }

        int getValue() {
            return value;
        }

        void updataValue() {
            value = seekBar.getProgress() + min;
        }

        void setValue(int value) {
            if (value < min) {
                value = min;
            } else if (value > max) {
                value = max;
            }
            setProgress0(value - min);
        }

        void setMax(int max) {
            this.max = max;
            if (max <= min) {
                min = max - 10;
                value = min + 5;
            }
            if (value > max) {
                value = max;
            }
            setProgress0(value - min);
            setMax0(max - min);
        }

        void setMin(int min) {
            this.min = min;
            if (min >= max) {
                max = min + 10;
                value = min + 5;
            }
            if (value < min) {
                value = min;
            }
            setProgress0(value - min);
            setMax0(max - min);
        }

        private void setMax0(int max) {
            Message msg = new Message();
            Bundle bundle = new Bundle();
            bundle.putInt("max", max);
            msg.setData(bundle);
            msg.what = R.integer.MSG_SET_FREQSEEKBAR_MAX;
            uiUpdateHandler.sendMessage(msg);
        }

        private void setProgress0(int progress) {
            Message msg = new Message();
            Bundle bundle = new Bundle();
            bundle.putInt("progress", progress);
            msg.setData(bundle);
            msg.what = R.integer.MSG_SET_FREQSEEKBAR_PROGRESS;
            uiUpdateHandler.sendMessage(msg);
        }
    }

    public void showToast(String text, int duration) {
        runOnUiThread(() -> Toast.makeText(MainActivityDoctor.this, text, duration).show());
    }

    Handler uiUpdateHandler = new UIUpdateHandler(new WeakReference<>(this));

    static class UIUpdateHandler extends Handler {
        private WeakReference<MainActivityDoctor> activity;

        UIUpdateHandler(WeakReference<MainActivityDoctor> activity) {
            this.activity = activity;
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(@NonNull Message msg) {
            MainActivityDoctor activity0 = activity.get();
            Bundle data = msg.getData();
            switch (msg.what) {
                case R.integer.MSG_SYSTEM_RESET:
                    activity0.systemReset();
                    break;
                case R.integer.MSG_UPDATE_WAVES:
                    activity0.iDiffWaveView.invalidate();
                    activity0.iPosNegWaveView.invalidate();
                    activity0.qDiffWaveView.invalidate();
                    activity0.qPosNegWaveView.invalidate();
                    activity0.ecgWaveView.invalidate();
                    break;
                case R.integer.MSG_SET_FREQSEEKBAR_MAX:
                    activity0.freqSeekBar.setMax(data.getInt("max"));
                    break;
                case R.integer.MSG_SET_FREQSEEKBAR_PROGRESS:
                    activity0.freqSeekBar.setProgress(data.getInt("progress"));
                    break;
                case R.integer.MSG_ENABLE_WIDGETS:
                    activity0.enableOrDisableWidgets(data.getInt("enable_status"));
                    break;
                case R.integer.MSG_UPDATE_curFreqRangeTextView:
                    activity0.curFreqRangeTextView.setText(data.getString("text"));
                    break;
                case R.integer.MSG_UPDATE_curFreqTextView:
                    activity0.curFreqTextView.setText(data.getString("text"));
                    break;
                case R.integer.MSG_UPDATE_EditTexts:
                    activity0.csEditText.setText(data.getString("cs"));
                    activity0.r0EditText.setText(data.getString("r0"));
                    activity0.r1EditText.setText(data.getString("r1"));
                    activity0.r2EditText.setText(data.getString("r2"));
                    activity0.r3EditText.setText(data.getString("r3"));
                    activity0.r4EditText.setText(data.getString("r4"));
                    activity0.r5EditText.setText(data.getString("r5"));
                    break;
                case R.integer.MSG_START:
                    activity0.isStart = true;
                    activity0.startButton.setEnabled(false);
                    activity0.stopButton.setEnabled(true);
                    activity0.autoCalcButton.setEnabled(true);
                    break;
                case R.integer.MSG_STOP:
                    activity0.isStart = false;
                    activity0.startButton.setEnabled(true);
                    activity0.stopButton.setEnabled(false);
                    activity0.autoCalcButton.setEnabled(false);
                    break;
                case R.integer.MSG_AUTO_CALC:
                    activity0.leftButton.setEnabled(false);
                    activity0.rightButton.setEnabled(false);
                    activity0.setAsMinButton.setEnabled(false);
                    activity0.setAsFreqButton.setEnabled(false);
                    activity0.setAsMaxButton.setEnabled(false);
                    activity0.autoCalcButton.setEnabled(false);
                    activity0.freqSeekBar.setEnabled(false);

                    activity0.csEditText.setText("1");
                    activity0.r0EditText.setText("0x54");
                    activity0.r1EditText.setText("0x04");
                    activity0.r2EditText.setText("0x07");
                    activity0.r3EditText.setText("0x8E");
                    activity0.r4EditText.setText("0x00");
                    activity0.r5EditText.setText("0x00");
                    break;
                case R.integer.MSG_UPDATE_IP:
                    activity0.ipEditText.setText(data.getString("IP"));
                    break;
                default:
                    Log.e(LOGTAG.LOGTAG, "Unknown message code: " + msg.what);
            }
        }
    }

    private Resources resources;
    private PopupMenu powerSelPopupMenu;

    // Flag that if the timer started.
    boolean isStart = false;
    // The current frequency.
    private int currentFreq = 1800;
    // Flag that if registers reassigned needed.
    private boolean isFreqChanged;

    private ConnectionToolbox connectionToolbox = ConnectionToolbox.getInstance();

    // Variables with "save" prefix are the save variables.
    // Comments are same as above.
    int saveDatabufPointer = 0;
    int[] saveDataIPos;
    int[] saveDataINeg;
    int[] saveDataQPos;
    int[] saveDataQNeg;
    int[] saveDataECG;
    int SAVE_DATABUF_SIZE;
    int LIST_DATABUF_SIZE;

    int[] ipos, ineg, qpos, qneg, idiff, qdiff, ecg;

    private EditText ipEditText;
    EditText csEditText;
    EditText r0EditText;
    EditText r1EditText;
    EditText r2EditText;
    EditText r3EditText;
    EditText r4EditText;
    EditText r5EditText;

    private TextView curFreqTextView;
    private SeekBar freqSeekBar;
    private SeekBarTools freqSeekBarToolbox;
    TextView curFreqRangeTextView;

    private Button wifiMethodButton;
    private Button bleMethodButton;
    private TextView connMethodTextView;
    private LinearLayout wifiMethodLayout;
    private LinearLayout bleMethodLayout;

    private Button leftButton;
    private EditText intervalText;
    private Button rightButton;
    private Button setAsMinButton;
    private Button setAsFreqButton;
    private Button setAsMaxButton;

    private Button curPowerButton;
    private Button startButton;
    private Button stopButton;
    private Button resetButton;
    private Button autoCalcButton;

    private AxesView iDiffWaveView;
    private AxesView iPosNegWaveView;
    private AxesView qDiffWaveView;
    private AxesView qPosNegWaveView;
    private AxesView ecgWaveView;

    private EditText wifiSsidEditText;
    private EditText wifiPwdEditText;
    private Button wifiInfoSubmitButton;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, R.integer.MENUITEM_CHANGE_FREQUENCY, 0, "Change Frequency");
        menu.add(0, R.integer.MENUITEM_ABOUT_ME, 0, "About Me");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.integer.MENUITEM_CHANGE_FREQUENCY:
                showToast("MENUITEM_CHANGE_FREQUENCY", Toast.LENGTH_LONG);
                break;
            case R.integer.MENUITEM_ABOUT_ME:
                showToast("ABOUT_ME", Toast.LENGTH_LONG);
                break;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == R.integer.PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                showToast("Require file write permission to store data!", Toast.LENGTH_LONG);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_doctor);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        requestPermissions(new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH},
                R.integer.PERMISSION_REQUEST_CODE);

        resources = this.getApplicationContext().getResources();

        SAVE_DATABUF_SIZE = resources.getInteger(R.integer.SAVE_DATABUF_SIZE);
        LIST_DATABUF_SIZE = resources.getInteger(R.integer.LIST_DATABUF_SIZE);

        saveDataIPos = new int[SAVE_DATABUF_SIZE];
        saveDataINeg = new int[SAVE_DATABUF_SIZE];
        saveDataQPos = new int[SAVE_DATABUF_SIZE];
        saveDataQNeg = new int[SAVE_DATABUF_SIZE];
        saveDataECG = new int[SAVE_DATABUF_SIZE];

        ipEditText = findViewById(R.id.ipEditText);
        csEditText = findViewById(R.id.csEditText);
        r0EditText = findViewById(R.id.r0EditText);
        r1EditText = findViewById(R.id.r1EditText);
        r2EditText = findViewById(R.id.r2EditText);
        r3EditText = findViewById(R.id.r3EditText);
        r4EditText = findViewById(R.id.r4EditText);
        r5EditText = findViewById(R.id.r5EditText);

        curFreqTextView = findViewById(R.id.curFreqTextView);
        freqSeekBar = findViewById(R.id.freqSeekBar);
        freqSeekBarToolbox = new SeekBarTools(freqSeekBar, 1000, 1800, 2600);
        curFreqRangeTextView = findViewById(R.id.curFreqRangeTextView);

        connMethodTextView = findViewById(R.id.connMethodTextView);

        wifiMethodButton = findViewById(R.id.wifiMethodButton);
        bleMethodButton = findViewById(R.id.bleMethodButton);

        wifiMethodLayout = findViewById(R.id.wifiMethodLayout);
        bleMethodLayout = findViewById(R.id.bleMethodLayout);

        leftButton = findViewById(R.id.leftButton);
        intervalText = findViewById(R.id.intervalText);
        rightButton = findViewById(R.id.rightButton);
        setAsMinButton = findViewById(R.id.setAsMinButton);
        setAsFreqButton = findViewById(R.id.setAsFreqButton);
        setAsMaxButton = findViewById(R.id.setAsMaxButton);

        curPowerButton = findViewById(R.id.curPowerButton);

        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        resetButton = findViewById(R.id.resetButton);
        autoCalcButton = findViewById(R.id.autoCalcButton);

        iDiffWaveView = findViewById(R.id.iDiffWaveView);
        iPosNegWaveView = findViewById(R.id.iPosNegWaveView);
        qDiffWaveView = findViewById(R.id.qDiffWaveView);
        qPosNegWaveView = findViewById(R.id.qPosNegWaveView);
        ecgWaveView = findViewById(R.id.ecgWaveView);

        wifiSsidEditText = findViewById(R.id.wifiSsidEditText);
        wifiPwdEditText = findViewById(R.id.wifiPwdEditText);
        wifiInfoSubmitButton = findViewById(R.id.wifiInfoSubmitButton);

        DisplayMetrics outMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        int widthPixels = outMetrics.widthPixels;
        int waveViewsMinimumHeight = (int) (9.0 / 16.0 * widthPixels);

        iDiffWaveView.setMinimumHeight(waveViewsMinimumHeight);
        iPosNegWaveView.setMinimumHeight(waveViewsMinimumHeight);
        qDiffWaveView.setMinimumHeight(waveViewsMinimumHeight);
        qPosNegWaveView.setMinimumHeight(waveViewsMinimumHeight);
        ecgWaveView.setMinimumHeight(waveViewsMinimumHeight);

        stopButton.setEnabled(false);
        autoCalcButton.setEnabled(false);
        addListeners();

        updateDataTimerTask = new UpdateDataTimerTask();
        new Timer().schedule(updateDataTimerTask, 0, 250);

//        System.arraycopy(DebugArrays.qdiffDebug, 0, qdiff, 0, DebugArrays.qdiffDebug.length);
//        System.arraycopy(DebugArrays.idiffDebug, 0, idiff, 0, DebugArrays.idiffDebug.length);
//        System.arraycopy(DebugArrays.ecgDebug, 0, ecg, 0, DebugArrays.ecgDebug.length);

        connectionToolbox.mGattCallback = new MyBluetoothGattCallback(this);
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void addListeners() {
        wifiMethodButton.setOnClickListener(view -> {
            if (connectionToolbox.mBluetoothGatt != null) {
                connectionToolbox.mBluetoothGatt.disconnect();
            }
            connectionToolbox.connectionMethod = ConnectionToolbox.CONNECTION_WIFI;
            runOnUiThread(() -> {
                updateConnMethodTextView();
                wifiMethodLayout.setVisibility(View.VISIBLE);
                bleMethodLayout.setVisibility(View.GONE);
            });
        });

        bleMethodButton.setOnClickListener(view -> {
            if (connectionToolbox.mBluetoothGatt != null) {
                connectionToolbox.mBluetoothGatt.disconnect();
            }
            Intent intent = new Intent(MainActivityDoctor.this, BleConnection.class);
            startActivityForResult(intent, resources.getInteger(R.integer.CONNECT_BLE_REQUEST_CODE));
        });

        leftButton.setOnClickListener(view -> {
            String deltaFreqStr = intervalText.getText().toString();
            int deltaFreq, newFreq;

            // Check if the text is a valid number.
            if (!(deltaFreqStr.matches("[1-9][0-9]{0,8}") && (deltaFreq = Integer.parseInt(deltaFreqStr)) <= freqSeekBarToolbox.getMax() - freqSeekBarToolbox.getMin() && deltaFreq > 0)) {
                showToast("Invalid input!", Toast.LENGTH_SHORT);
                return;
            }

            // If the new frequency is smaller than the minimum frequency of the slider bar.
            if (freqSeekBarToolbox.getValue() - deltaFreq >= freqSeekBarToolbox.getMin()) {
                newFreq = freqSeekBarToolbox.getValue() - deltaFreq;
            } else {
                newFreq = freqSeekBarToolbox.getMin();
            }
            if (newFreq != currentFreq) {
                currentFreq = newFreq;
                new Thread() {
                    public void run() {
                        changeFreq();
                    }
                }.start();
            }
        });

        rightButton.setOnClickListener(view -> {
            String deltaFreqStr = intervalText.getText().toString();
            int deltaFreq, newFreq;

            // Check if the text is a valid number.
            if (!(deltaFreqStr.matches("[1-9][0-9]{0,8}") && (deltaFreq = Integer.parseInt(deltaFreqStr)) <= freqSeekBarToolbox.getMax() - freqSeekBarToolbox.getMin() && deltaFreq > 0)) {
                showToast("Invalid input!", Toast.LENGTH_SHORT);
                return;
            }

            // If the new frequency is bigger than the maximum frequency of the slider bar.
            if (freqSeekBarToolbox.getValue() + deltaFreq <= freqSeekBarToolbox.getMax()) {
                newFreq = freqSeekBarToolbox.getValue() + deltaFreq;
            } else {
                newFreq = freqSeekBarToolbox.getMax();
            }
            if (newFreq != currentFreq) {
                currentFreq = newFreq;
                new Thread() {
                    public void run() {
                        changeFreq();
                    }
                }.start();
            }
        });

        setAsFreqButton.setOnClickListener(view -> {
            String deltaFreqStr = intervalText.getText().toString();
            int newFreq;

            // Check if the text is a valid number.
            if (!(deltaFreqStr.matches("[1-9][0-9]{0,8}") && (newFreq = Integer.parseInt(deltaFreqStr)) < resources.getInteger(R.integer.MAX_FREQ) && newFreq > resources.getInteger(R.integer.MIN_FREQ))) {
                showToast("Invalid input!", Toast.LENGTH_SHORT);
                return;
            }

            if (newFreq != currentFreq) {
                currentFreq = newFreq;
                new Thread() {
                    public void run() {
                        changeFreq();
                    }
                }.start();
            }
        });

        setAsMinButton.setOnClickListener(view -> {
            String deltaFreqStr = intervalText.getText().toString();
            int minFreq;

            // Check if the text is a valid number.
            if (!(deltaFreqStr.matches("[1-9][0-9]{0,8}") && (minFreq = Integer.parseInt(deltaFreqStr)) <= freqSeekBarToolbox.getMax() - 10 && minFreq >= resources.getInteger(R.integer.MIN_FREQ))) {
                showToast("Invalid input!", Toast.LENGTH_SHORT);
                return;
            }

            freqSeekBarToolbox.setMin(minFreq);
            curFreqRangeTextView.setText(String.format("(Slider Range: %d ~ %d MHz)", freqSeekBarToolbox.getMin(), freqSeekBarToolbox.getMax()));
            if (minFreq > currentFreq) {
                currentFreq = minFreq;
                new Thread() {
                    public void run() {
                        changeFreq();
                    }
                }.start();
            }
        });

        setAsMaxButton.setOnClickListener(view -> {
            String deltaFreqStr = intervalText.getText().toString();
            int maxFreq;

            // Check if the text is a valid number.
            if (!(deltaFreqStr.matches("[1-9][0-9]{0,8}") && (maxFreq = Integer.parseInt(deltaFreqStr)) >= freqSeekBarToolbox.getMin() + 10 && maxFreq <= resources.getInteger(R.integer.MAX_FREQ))) {
                showToast("Invalid input!", Toast.LENGTH_SHORT);
                return;
            }

            freqSeekBarToolbox.setMax(maxFreq);
            curFreqRangeTextView.setText(String.format("(Slider Range: %d ~ %d MHz)", freqSeekBarToolbox.getMin(), freqSeekBarToolbox.getMax()));
            if (maxFreq < currentFreq) {
                currentFreq = maxFreq;
                new Thread() {
                    public void run() {
                        changeFreq();
                    }
                }.start();
            }
        });

        freqSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private int currentValue = 0;
            private boolean isTouch = false;

            @SuppressLint("DefaultLocale")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                freqSeekBarToolbox.updataValue();
                if (isTouch) {
                    curFreqTextView.setText(String.format("Freq: %d MHz (Release to Apply)", 1000 + progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                currentValue = freqSeekBarToolbox.getValue();
                isTouch = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isTouch = false;
                if (currentValue != freqSeekBarToolbox.getValue()) {
                    currentFreq = freqSeekBarToolbox.getValue();
                    new Thread() {
                        public void run() {
                            changeFreq();
                        }
                    }.start();
                }
            }
        });

        curPowerButton.setOnClickListener(view -> powerSelPopupMenu.show());
        powerSelPopupMenu = new PopupMenu(this, curPowerButton);
        getMenuInflater().inflate(R.menu.power_sel_popupmenu, powerSelPopupMenu.getMenu());
        powerSelPopupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.plus5PopupMenuItem:
                    curPowerButton.setText("Current Power: +5 dBm");
                    break;
                case R.id.plus2PopupMenuItem:
                    curPowerButton.setText("Current Power: +2 dBm");
                    break;
                case R.id.minus1PopupMenuItem:
                    curPowerButton.setText("Current Power: -1 dBm");
                    break;
                case R.id.minus4PopupMenuItem:
                    curPowerButton.setText("Current Power: -4 dBm");
                    break;
            }
            new Thread() {
                public void run() {
                    changeFreq();
                }
            }.start();
            return true;
        });

        startButton.setOnClickListener(view -> {
            if (ConnectionToolbox.getInstance().connectionMethod == ConnectionToolbox.CONNECTION_UNKNOWN) {
                showToast("Please connect to the device via Bluetooth or select to use Wi-Fi method first!", Toast.LENGTH_SHORT);
                return;
            }
            uiUpdateHandler.sendEmptyMessage(R.integer.MSG_START);
            new Thread() {
                public void run() {
                    changeFreq();
                }
            }.start();
        });

        stopButton.setOnClickListener(view -> uiUpdateHandler.sendEmptyMessage(R.integer.MSG_STOP));

        resetButton.setOnClickListener(view -> uiUpdateHandler.sendEmptyMessage(R.integer.MSG_SYSTEM_RESET));

        autoCalcButton.setOnClickListener(view -> {
            uiUpdateHandler.sendEmptyMessage(R.integer.MSG_AUTO_CALC);
            new Thread() {
                public void run() {
                    if (connectionToolbox.connectionMethod == ConnectionToolbox.CONNECTION_BLUETOOTH) {
                        connectionToolbox.sendMessage("CS1");
                        connectionToolbox.sendMessage("R084");
                        connectionToolbox.sendMessage("R14");
                        connectionToolbox.sendMessage("R27");
                        connectionToolbox.sendMessage("R3142");
                        connectionToolbox.sendMessage("R40");
                        connectionToolbox.sendMessage("R50");
                    } else if (connectionToolbox.connectionMethod == ConnectionToolbox.CONNECTION_WIFI) {
                        if (waitingForArgsTransmitting()) {
                            return;
                        }

                        isFreqChanged = true;

                        if (waitingForArgsTransmitting()) {
                            return;
                        }
                    }

                    Message msg_enableWidgets = new Message();
                    Bundle bundle_enableWidgets = new Bundle();
                    int enableStatus = 0b1111110;
                    if (isStart) {
                        enableStatus |= 0b1;
                    }
                    bundle_enableWidgets.putInt("enable_status", enableStatus);
                    msg_enableWidgets.setData(bundle_enableWidgets);
                    msg_enableWidgets.what = R.integer.MSG_ENABLE_WIDGETS;
                    uiUpdateHandler.sendMessage(msg_enableWidgets);

                    showToast("Auto Calc Finished!", Toast.LENGTH_SHORT);
                }
            }.start();
        });

        wifiInfoSubmitButton.setOnClickListener(view -> {
            String SSID = wifiSsidEditText.getText().toString();
            String PWD = wifiPwdEditText.getText().toString();
            connectionToolbox.sendMessage("ID" + SSID);
            connectionToolbox.sendMessage("PD" + PWD);
            showToast("Successfully sent the Wi-Fi info! Please wait at most 5 seconds to connect!", Toast.LENGTH_LONG);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            if (requestCode == resources.getInteger(R.integer.CONNECT_BLE_REQUEST_CODE)) {
                runOnUiThread(this::updateConnMethodTextView);
                if (connectionToolbox.connectionMethod == ConnectionToolbox.CONNECTION_BLUETOOTH) {
                    runOnUiThread(() -> {
                        wifiMethodLayout.setVisibility(View.GONE);
                        bleMethodLayout.setVisibility(View.VISIBLE);
                    });
                }
//                connectionToolbox.mGattCallback.init(this, getApplicationContext());
            }
        }
    }

    @SuppressLint("SetTextI18n")
    void updateConnMethodTextView() {
        switch (connectionToolbox.connectionMethod) {
            case ConnectionToolbox.CONNECTION_UNKNOWN:
                connMethodTextView.setText("Connection Method: Unknown");
                break;
            case ConnectionToolbox.CONNECTION_BLUETOOTH:
                connMethodTextView.setText("Connection Method: Bluetooth");
                break;
            case ConnectionToolbox.CONNECTION_WIFI:
                connMethodTextView.setText("Connection Method: Wi-Fi");
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveDataToFile();
        if (connectionToolbox.connectionMethod == ConnectionToolbox.CONNECTION_BLUETOOTH) {
            connectionToolbox.mBluetoothGatt.disconnect();
        }
    }

    // leftButton, rightButton,
    // setAsMinButton, setAsFreqButton, setAsMaxButton,
    // freqSeekBar, autoCalcButton
    // from left to right
    public void enableOrDisableWidgets(int enableStatus) {
        if ((enableStatus & (0b1 << 6)) != 0) {
            leftButton.setEnabled(true);
        }
        if ((enableStatus & (0b1 << 5)) != 0) {
            rightButton.setEnabled(true);
        }
        if ((enableStatus & (0b1 << 4)) != 0) {
            setAsMinButton.setEnabled(true);
        }
        if ((enableStatus & (0b1 << 3)) != 0) {
            setAsFreqButton.setEnabled(true);
        }
        if ((enableStatus & (0b1 << 2)) != 0) {
            setAsMaxButton.setEnabled(true);
        }
        if ((enableStatus & (0b1 << 1)) != 0) {
            freqSeekBar.setEnabled(true);
        }
        if ((enableStatus & 0b1) != 0) {
            autoCalcButton.setEnabled(true);
        }
    }

    @SuppressLint("SetTextI18n")
    private void systemReset() {
        if (saveDatabufPointer > 0) {
            saveDataToFile();
        }

        isStart = false;
        isFreqChanged = false;
        csEditText.setText("0");
        r0EditText.setText("0");
        r1EditText.setText("0");
        r2EditText.setText("0");
        r3EditText.setText("0");
        r4EditText.setText("0");
        r5EditText.setText("0");

        AxesView.setListDatabufPointer(0);
        iDiffWaveView.reset();
        iPosNegWaveView.reset();
        qDiffWaveView.reset();
        qPosNegWaveView.reset();
        ecgWaveView.reset();

        iDiffWaveView.invalidate();
        iPosNegWaveView.invalidate();
        qDiffWaveView.invalidate();
        qPosNegWaveView.invalidate();
        ecgWaveView.invalidate();

        saveDatabufPointer = 0;

        freqSeekBarToolbox.setMax(2600);
        freqSeekBarToolbox.setMin(1000);
        freqSeekBarToolbox.setValue(1800);
        curFreqRangeTextView.setText("(Slider Range: 1000 ~ 2600 MHz)");
        currentFreq = 1800;

        changeFreq();

        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        autoCalcButton.setEnabled(false);

        leftButton.setEnabled(true);
        rightButton.setEnabled(true);
        setAsMinButton.setEnabled(true);
        setAsFreqButton.setEnabled(true);
        setAsMaxButton.setEnabled(true);
        freqSeekBar.setEnabled(true);

        wifiMethodLayout.setVisibility(View.GONE);
        bleMethodLayout.setVisibility(View.GONE);

        if (connectionToolbox.mBluetoothGatt != null) {
            connectionToolbox.mBluetoothGatt.disconnect();
        }
        connectionToolbox.connectionMethod = ConnectionToolbox.CONNECTION_UNKNOWN;

        runOnUiThread(this::updateConnMethodTextView);
    }

    /**
     * Calculate the URL.
     *
     * @return The URL.
     */
    @SuppressLint("DefaultLocale")
    private String getURL() {
        if (isFreqChanged) {
            return String.format("http://%s/?CS=%s&R0=%d&R1=%d&R2=%d&R3=%d&R4=%d&R5=%d",
                    ipEditText.getText().toString(), csEditText.getText().toString(),
                    Integer.parseInt(r0EditText.getText().toString().substring(2), 16),
                    Integer.parseInt(r1EditText.getText().toString().substring(2), 16),
                    Integer.parseInt(r2EditText.getText().toString().substring(2), 16),
                    Integer.parseInt(r3EditText.getText().toString().substring(2), 16),
                    Integer.parseInt(r4EditText.getText().toString().substring(2), 16),
                    Integer.parseInt(r5EditText.getText().toString().substring(2), 16));
        } else {
            return String.format("http://%s/", ipEditText.getText().toString());
        }
    }

    /**
     * This function aims to block the current thread until last transmission
     * finished or time's up.
     *
     * @return Whether time's up or not (true for timeout).
     */
    private boolean waitingForArgsTransmitting() {
        int waitingCycles = 0;
        while (isFreqChanged) {
            waitingCycles++;
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                showToast("Exception occurred: " + e.getMessage(), Toast.LENGTH_LONG);
            }
            if (waitingCycles >= resources.getInteger(R.integer.TIMEOUT)) {
                showToast("Connection Timeout! The system reset!", Toast.LENGTH_LONG);
                uiUpdateHandler.sendEmptyMessage(R.integer.MSG_SYSTEM_RESET);
                return true;
            }
        }
        return false;
    }

    @SuppressLint("DefaultLocale")
    private static String makeup0(int bits, String value) {
        return String.format(String.format("%%0%dd", bits - value.length()), 0) + value;
    }

    private static int[] freqCalcResult = new int[2];

    private static void freqCalc(int targetFreq) {
        if (targetFreq >= 2200) {
            freqCalcResult[0] = (int) Math.floor(targetFreq / 26.0);
            freqCalcResult[1] = targetFreq % 26;
        } else {
            freqCalcResult[0] = (int) Math.floor(targetFreq / 13.0);
            freqCalcResult[1] = targetFreq % 13;
        }
    }

    /**
     * Apply the change of the frequency to MCU.
     */
    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void changeFreq() {
        if (currentFreq > freqSeekBarToolbox.getMax()) {
            freqSeekBarToolbox.setMax(currentFreq);
        } else if (currentFreq < freqSeekBarToolbox.getMin()) {
            freqSeekBarToolbox.setMin(currentFreq);
        }

        Message msg_update_curFreqRangeTextView = new Message();
        Bundle bundle_update_curFreqRangeTextView = new Bundle();
        bundle_update_curFreqRangeTextView.putString("text", String.format("(Slider Range: %d ~ %d MHz)", freqSeekBarToolbox.getMin(), freqSeekBarToolbox.getMax()));
        msg_update_curFreqRangeTextView.what = R.integer.MSG_UPDATE_curFreqRangeTextView;
        msg_update_curFreqRangeTextView.setData(bundle_update_curFreqRangeTextView);
        uiUpdateHandler.sendMessage(msg_update_curFreqRangeTextView);

        freqSeekBarToolbox.setValue(currentFreq);

        String curFreqTextViewCaption = String.format("Current Freq: %d MHz", currentFreq);

        if (!isStart) {
            Message msg_update_curFreqTextView = new Message();
            Bundle bundle_update_curFreqTextView = new Bundle();
            bundle_update_curFreqTextView.putString("text", curFreqTextViewCaption + " (Not Applied)");
            msg_update_curFreqTextView.what = R.integer.MSG_UPDATE_curFreqTextView;
            msg_update_curFreqTextView.setData(bundle_update_curFreqTextView);
            uiUpdateHandler.sendMessage(msg_update_curFreqTextView);
            return;
        }

        // Register0
        // The calculation will assume the divider = 1
        final int MODDenominator = 26;
        freqCalc(currentFreq);
        int intVal = freqCalcResult[0];
        int fracVal = freqCalcResult[1];
        final int numINT = 16;
        final int numFRAC = 12;
        final int numCTL = 3;
        final int numMOD = 12;

        if (intVal > 2e15 || intVal < 23) {
            showToast("Integer value is too big or small for normal operation. Please double check with Frequency calculation!", Toast.LENGTH_LONG);
            isFreqChanged = false;
            Message msg_update_curFreqTextView = new Message();
            Bundle bundle_update_curFreqTextView = new Bundle();
            bundle_update_curFreqTextView.putString("text", curFreqTextViewCaption + " (Not Applied)");
            msg_update_curFreqTextView.what = R.integer.MSG_UPDATE_curFreqTextView;
            msg_update_curFreqTextView.setData(bundle_update_curFreqTextView);
            uiUpdateHandler.sendMessage(msg_update_curFreqTextView);
            return;
        }

        if (fracVal > 2e11) {
            showToast("Fractional value is too big for register. Please double check with Frequency calculation!", Toast.LENGTH_LONG);
            isFreqChanged = false;
            Message msg_update_curFreqTextView = new Message();
            Bundle bundle_update_curFreqTextView = new Bundle();
            bundle_update_curFreqTextView.putString("text", curFreqTextViewCaption + " (Not Applied)");
            msg_update_curFreqTextView.what = R.integer.MSG_UPDATE_curFreqTextView;
            msg_update_curFreqTextView.setData(bundle_update_curFreqTextView);
            uiUpdateHandler.sendMessage(msg_update_curFreqTextView);
            return;
        }

        Message msg_update_curFreqTextView = new Message();
        Bundle bundle_update_curFreqTextView = new Bundle();
        bundle_update_curFreqTextView.putString("text", curFreqTextViewCaption);
        msg_update_curFreqTextView.what = R.integer.MSG_UPDATE_curFreqTextView;
        msg_update_curFreqTextView.setData(bundle_update_curFreqTextView);
        uiUpdateHandler.sendMessage(msg_update_curFreqTextView);

        // Generate Binary Char Vector for Register 0
        String INTBin = makeup0(numINT, Integer.toString(intVal, 2));
        String FRACBin = makeup0(numFRAC, Integer.toString(fracVal, 2));
        String CTRLBin0 = makeup0(numCTL, "0");
        String REG0 = makeup0(32, INTBin + FRACBin + CTRLBin0);

        // Redister 1
        // Prescalar: INT is the preset divide ratio of the binary 16-bit counter (23 to 65,535 for the 4/5 prescaler; 75 to 65,535 for the 8/9 prescaler). so 4/5 prescalar must be chosen for lower frequency that we want. '0'
        // Phase: Bits[DB26:DB15] control the phase word. The phase word must be less than the MOD value programmed in Register 1. The phase word is used to program the RF output phase from 0бу to 360бу with a resolution of 360бу/MOD (see the Phase Resync section). Disabled for now
        // numPHASE = 12;

        // Generate Binary Char Vector for Register 1
        String MODBin = makeup0(numMOD, Integer.toString(MODDenominator, 2));
        String CTLbi1 = makeup0(numCTL, "1");
        String REG1 = "00001000" + makeup0(24, MODBin + CTLbi1);

        // Register 4
        StringBuilder REG4 = new StringBuilder("0x00");
        if (currentFreq >= 2200) {
            REG4.append("8D"); // 10001101
        } else {
            REG4.append("9D"); // 10011101
        }
        REG4.append("04");
        switch (Integer.parseInt(curPowerButton.getText().subSequence(15, 17).toString())) {
            case 5:
                REG4.append("3C"); // 00111100
                break;
            case 2:
                REG4.append("34"); // 00110100
                break;
            case -1:
                REG4.append("2C"); // 00101100
                break;
            default: // -4
                REG4.append("24"); // 00100100
        }

        Message msg_enableWidgets = new Message();
        Bundle bundle_enableWidgets = new Bundle();
        int enableStatus = 0b0000000;
        bundle_enableWidgets.putInt("enable_status", enableStatus);
        msg_enableWidgets.setData(bundle_enableWidgets);
        msg_enableWidgets.what = R.integer.MSG_ENABLE_WIDGETS;
        uiUpdateHandler.sendMessage(msg_enableWidgets);

        Message msg_updateEditTexts = new Message();
        Bundle bundle_updateEditTexts = new Bundle();
        bundle_updateEditTexts.putString("cs", "0");
        bundle_updateEditTexts.putString("r0", String.format("0x%08X", Integer.parseInt(REG0, 2)));
        bundle_updateEditTexts.putString("r1", String.format("0x%08X", Integer.parseInt(REG1, 2)));
        bundle_updateEditTexts.putString("r2", "0x00005E42");
        bundle_updateEditTexts.putString("r3", "0x000004B3");
        bundle_updateEditTexts.putString("r4", REG4.toString());
        bundle_updateEditTexts.putString("r5", "0x00580005");
        msg_updateEditTexts.setData(bundle_updateEditTexts);
        msg_updateEditTexts.what = R.integer.MSG_UPDATE_EditTexts;
        uiUpdateHandler.sendMessage(msg_updateEditTexts);

        // For Debug
//        r0EditText.setText("0x00300010");
//        r1EditText.setText("0x08008069");
//        r4EditText.setText("0x008D043C");

        if (connectionToolbox.connectionMethod == ConnectionToolbox.CONNECTION_WIFI) {
            if (waitingForArgsTransmitting()) {
                return;
            }

            isFreqChanged = true;

            if (waitingForArgsTransmitting()) {
                return;
            }
        }

        Message msg_enableWidgets_1 = new Message();
        Bundle bundle_enableWidgets_1 = new Bundle();
        enableStatus = 0b1111100;
        if (isStart) {
            enableStatus |= 0b11;
        }
        bundle_enableWidgets_1.putInt("enable_status", enableStatus);
        msg_enableWidgets_1.setData(bundle_enableWidgets_1);
        msg_enableWidgets_1.what = R.integer.MSG_ENABLE_WIDGETS;
        uiUpdateHandler.sendMessage(msg_enableWidgets_1);
    }

    @SuppressLint({"DefaultLocale", "SimpleDateFormat"})
    void saveDataToFile() {
        if (saveDatabufPointer == 0) {
            return;
        }

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            showToast("Need file write permission to store the data files!", Toast.LENGTH_LONG);
            return;
        }

        File externalFilesDir = getApplicationContext().getExternalFilesDir(null);

        if (externalFilesDir == null) {
            showToast("Cannot save the data to the external files directory!", Toast.LENGTH_LONG);
            return;
        }

        File savedPath = new File(externalFilesDir.getAbsoluteFile(), "HeartRaderSavedData");

        if (savedPath.isFile()) {
            showToast(String.format("\"%s\" is a file! Please delete it and retry!", savedPath.toString()), Toast.LENGTH_LONG);
            return;
        }
        if (!savedPath.exists() && !savedPath.mkdir()) {
            showToast("Cannot make the save directory!", Toast.LENGTH_LONG);
            return;
        }

        File targetFile = new File(savedPath, "data_" + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date()) + ".csv");

        if (targetFile.isDirectory()) {
            showToast(String.format("\"%s\" is a directory! Please delete it and retry!", targetFile.toString()), Toast.LENGTH_LONG);
            return;
        }

        if (targetFile.exists()) {
            showToast(String.format("\"%s\" exists! Please delete it and retry!", targetFile.toString()), Toast.LENGTH_LONG);
            return;
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(targetFile))) {
            bw.write("\"I_Positive\",\"I_Negative\",\"Q_Positive\",\"Q_Negative\",\"ECG\"\n");
            for (int dataIndex = 0; dataIndex < saveDatabufPointer; dataIndex++) {
                bw.write(String.format("%d,%d,%d,%d,%d\n",
                        saveDataIPos[dataIndex],
                        saveDataINeg[dataIndex],
                        saveDataQPos[dataIndex],
                        saveDataQNeg[dataIndex],
                        saveDataECG[dataIndex]));
            }
        } catch (IOException e) {
            showToast("Exception occurred: " + e.getMessage(), Toast.LENGTH_LONG);
            return;
        }
        saveDatabufPointer = 0;
    }
}
