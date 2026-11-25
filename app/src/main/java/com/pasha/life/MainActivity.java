package com.pasha.life;

import android.app.Activity;
import android.app.AlarmManager;
import android.os.Bundle;
import android.os.Build;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.provider.Settings;
import android.net.Uri;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        WebView web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "Android");

        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
                "android.permission.POST_NOTIFICATIONS"
            ) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{
                    "android.permission.POST_NOTIFICATIONS"
                }, 1
            );
        } else {
            checkExactAlarm();
        }
    }

    private void checkExactAlarm() {
        if (Build.VERSION.SDK_INT >= 31) {
            AlarmManager am = getSystemService(
                AlarmManager.class
            );

            if (!am.canScheduleExactAlarms()) {
                Intent intent = new Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                );
                intent.setData(
                    Uri.parse("package:" + getPackageName())
                );
                startActivity(intent);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode,
        String[] permissions,
        int[] grantResults
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        );
        if (requestCode == 1) {
            checkExactAlarm();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Reminder.scheduleAll(this);
    }

    public class Bridge {
        @JavascriptInterface
        public void schedule(String json) {
            runOnUiThread(() ->
                Reminder.saveAndSchedule(
                    MainActivity.this, json
                )
            );
        }
    }
}
