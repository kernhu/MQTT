package org.cion.mqtt;

import android.app.Application;

import com.tencent.bugly.crashreport.CrashReport;

import org.cion.mqtt.contants.BuglyConfig;

/**
 * @author: kern
 * @date: 2021/12/1
 * @Description: java类作用描述
 */
public class AppBase extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        initBugly();
    }

    private void initBugly() {
        CrashReport.initCrashReport(getApplicationContext(), BuglyConfig.APP_ID, false);
    }
}
