package com.pdf.ai;

import android.app.Application;
import android.content.Intent;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            Intent intent = new Intent(this, DebugActivity.class);
            intent.putExtra(DebugActivity.EXTRA_LOGS, ex.toString());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            System.exit(1);
        });
    }
}
