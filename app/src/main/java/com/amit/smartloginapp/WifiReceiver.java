package com.amit.smartloginapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class WifiReceiver extends BroadcastReceiver {

    public WifiReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent wifiStateIntent = new Intent("wifiStateChange");
        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMan.getActiveNetworkInfo();
        if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            wifiStateIntent.putExtra("isOn", true);
        } else {
            wifiStateIntent.putExtra("isOn", false);
        }
        context.sendBroadcast(wifiStateIntent);
    }
}
