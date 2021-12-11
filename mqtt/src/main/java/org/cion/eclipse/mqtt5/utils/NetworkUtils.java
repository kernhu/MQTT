package org.cion.eclipse.mqtt5.utils;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * @author: kern
 * @date: 2021/11/25
 * @Description: java类作用描述
 */
public class NetworkUtils {

    /**
     * 判断网络是否连接
     */
    public static boolean isConnecting(Application application) {
        ConnectivityManager connectivityManager = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            String name = info.getTypeName();
            Log.i("NetworkUtils", "当前网络名称：" + name);
            return true;
        } else {
            return false;
        }
    }
}
