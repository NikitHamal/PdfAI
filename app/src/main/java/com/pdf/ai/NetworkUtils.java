package com.pdf.ai;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

public class NetworkUtils {
    
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;
        
        ConnectivityManager connectivityManager = (ConnectivityManager) 
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && 
                   (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
    }
    
    public static boolean isWifiConnected(Context context) {
        if (context == null) return false;
        
        ConnectivityManager connectivityManager = (ConnectivityManager) 
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) return false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            NetworkInfo wifiNetwork = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return wifiNetwork != null && wifiNetwork.isConnected();
        }
    }
    
    public static String getConnectionType(Context context) {
        if (!isNetworkAvailable(context)) {
            return "No Connection";
        }
        
        if (isWifiConnected(context)) {
            return "WiFi";
        } else {
            return "Mobile Data";
        }
    }
    
    public static boolean shouldWarnAboutDataUsage(Context context) {
        return isNetworkAvailable(context) && !isWifiConnected(context);
    }
}