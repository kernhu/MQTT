package org.cion.eclipse.mqtt5.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.core.app.ActivityCompat;

import java.util.UUID;

/**
 * @author: kern
 * @date: 2021/11/25
 * @Description: java类作用描述
 */
public class DeviceUtils {

    /**
     * @param context
     * @return
     */
    public static String getUniqueId(Context context) {
        String sn = getSerialNumber(context);
        String androidId = getAndroid(context);
        if (!TextUtils.isEmpty(sn)) {
            return sn;
        } else if (!TextUtils.isEmpty(androidId)) {
            return androidId;
        } else {
            return UUID.randomUUID().toString();
        }
    }

    /**
     * @param context
     * @return
     */
    @SuppressLint("MissingPermission")
    public static String getSerialNumber(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ActivityCompat.checkSelfPermission(context.getApplicationContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                return "";
            }
            try {
                return Build.getSerial();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return Build.SERIAL;
    }

    /**
     * @param context
     * @return
     */
    public static String getAndroid(Context context) {
        return Settings.System.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

}
