package com.pasha.life;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.content.pm.PackageManager;

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
        }
    }

    public class Bridge {
        @JavascriptInterface
        public void schedule(String json) {
            Reminder.saveAndSchedule(
                MainActivity.this, json
            );
        }
    }
}
