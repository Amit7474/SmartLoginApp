package com.amit.smartloginapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MotionEventCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import me.turkergoksu.lib.PercentageView;

public class MainActivity extends AppCompatActivity implements SensorListener {

    private MaterialButton loginBtn;
    private TextInputEditText loginpswd;
    private RelativeLayout mRLmain;
    private PercentageView mPercentageView;
    private final String PASSWORD = "123456789";
    private SensorManager sensorMgr;
    private static final int SHAKE_THRESHOLD = 10000;
    private static final int SWIPE_THRESHOLD = 5000;
    private static final int SECRET_ACTIONS_THRESHOLD = 3;
    private long lastUpdate;
    private boolean isFirstTouch = true, swipeChallengeFinished = false, shakeChallengeFinished = false, wifiOnChallengeFinished = false;
    private float last_x, last_y, last_z;
    private int[] start = new int[2];
    private int totalSwipeDistance, secretActions = 0, progressPercentage = 0;
    private BroadcastReceiver mWifiReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorMgr.registerListener(this,
                SensorManager.SENSOR_ACCELEROMETER,
                SensorManager.SENSOR_DELAY_GAME);
        loginBtn = findViewById(R.id.login_BTN);
        loginpswd = findViewById(R.id.login_TXT);
        mRLmain = findViewById(R.id.RL_main);
        mPercentageView = findViewById(R.id.percentageView);
        loginBtn.setOnClickListener(loginBtnListrener);
        loginpswd.addTextChangedListener(passwordListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mWifiReceiver = registerWifiReceiver();
        registerReceiver(mWifiReceiver, new IntentFilter("wifiStateChange"));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mWifiReceiver);
    }

    /**
     *
     */
    private void logIn() {
        MyVibrator.vibrate(getApplicationContext());
        secretActions++;
        if (secretActions == SECRET_ACTIONS_THRESHOLD && progressPercentage == 100) {
            loginBtn.setEnabled(true);
        } else {
            Snackbar.make(mRLmain, "Keep Going! you have more " + (SECRET_ACTIONS_THRESHOLD - secretActions) + " actions to complete!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    /**
     *
     * @return
     */
    private BroadcastReceiver registerWifiReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean state = intent.getBooleanExtra("isOn", false);
                if (state) {
                    if (!wifiOnChallengeFinished) {
                        wifiOnChallengeFinished = true;
                        progressPercentage += 30;
                        mPercentageView.setPercentage(progressPercentage);
                        logIn();
                    }
                } else {
                    Log.d("login", "Don't have Wifi Connection");
                }
            }
        };
    }

    /**
     *
     */
    View.OnClickListener loginBtnListrener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String userPassword = loginpswd.getText().toString();
            if (userPassword.equalsIgnoreCase(PASSWORD)) {
                Intent intent = new Intent(MainActivity.this, AfterLogin.class);
                startActivity(intent);
            } else {
                Snackbar.make(mRLmain, "Wrong Password", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }

        }
    };

    /**
     *
     */
    TextWatcher passwordListener = new TextWatcher() {
        private String lastInput = "";

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (lastInput.length() < s.length()) {
                progressPercentage += 1;
            } else {
                progressPercentage -= 1;
                if (lastInput.equalsIgnoreCase(PASSWORD)) {
                    progressPercentage -= 1;
                }
            }
            lastInput = s.toString();
            mPercentageView.setPercentage(progressPercentage);
            if (s.toString().equalsIgnoreCase(PASSWORD)) {
                progressPercentage += 1;
                mPercentageView.setPercentage(progressPercentage);
            }
            if (progressPercentage == 100) {
                loginBtn.setEnabled(true);
            }

        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    /**
     *
     * @param sensor
     * @param values
     */
    @Override
    public void onSensorChanged(int sensor, float[] values) {
        if (shakeChallengeFinished) {
            return;
        }
        if (sensor == SensorManager.SENSOR_ACCELEROMETER) {
            long curTime = System.currentTimeMillis();
            // only allow one update every 100ms.
            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;
                float x = values[SensorManager.DATA_X];
                float y = values[SensorManager.DATA_Y];
                float z = values[SensorManager.DATA_Z];

                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    shakeChallengeFinished = true;
                    progressPercentage += 30;
                    mPercentageView.setPercentage(progressPercentage);
                    logIn();
                }
                last_x = x;
                last_y = y;
                last_z = z;
            }
        }
    }

    @Override
    public void onAccuracyChanged(int sensor, int accuracy) {

    }

    /**
     *
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (swipeChallengeFinished) {
            return true;
        }
        int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (isFirstTouch) {
                    start[0] = (int) event.getX();
                    start[1] = (int) event.getY();
                    Log.d("login:", "X: " + start[0] + ", Y:" + start[1]);
                    isFirstTouch = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                totalSwipeDistance += Math.abs(start[0] - (int) event.getX()) + Math.abs((int) event.getY() - start[1]);
                start[0] = (int) event.getX();
                start[1] = (int) event.getY();
                Log.d("login:", "total: " + totalSwipeDistance);
                if (totalSwipeDistance >= SWIPE_THRESHOLD) {
                    swipeChallengeFinished = true;
                    progressPercentage += 30;
                    mPercentageView.setPercentage(progressPercentage);
                    logIn();
                }
                break;
            case MotionEvent.ACTION_UP:
                isFirstTouch = true;
                start[0] = 0;
                start[1] = 0;
                totalSwipeDistance = 0;
                break;
        }


        return true;
    }
}