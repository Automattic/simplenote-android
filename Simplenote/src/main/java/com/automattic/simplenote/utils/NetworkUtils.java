package com.automattic.simplenote.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import java.text.DecimalFormat;

public class NetworkUtils {
    private static final int TYPE_NONE = -1;
    private static final int TYPE_NULL = -2;

    /**
     * @return {@link String} formatting speed of network in bps (e.g. 12.34 Kbps)
     */
    private static String formatSpeedFromKbps(int speed) {
        if (speed <= 0) {
            return "0.00";
        }

        String[] units = new String[] { "Kbps", "Mbps", "Gbps", "Tbps" };
        int index = (int) (Math.log10(speed) / Math.log10(1024));
        return new DecimalFormat("#,##0.00").format(speed / Math.pow(1024, index)) + " " + units[index];
    }

    /**
     * @return {@link String} formatting speed of network in bps (e.g. 12.34 Mbps)
     */
    private static String formatSpeedFromMbps(int speed) {
        if (speed <= 0) {
            return "0.00";
        }

        String[] units = new String[] { "Mbps", "Gbps", "Tbps" };
        int index = (int) (Math.log10(speed) / Math.log10(1024));
        return new DecimalFormat("#,##0.00").format(speed / Math.pow(1024, index)) + " " + units[index];
    }

    /**
     * @return {@link NetworkInfo} on the active network connection
     */
    private static NetworkInfo getActiveNetworkInfo(Context context) {
        if (context == null) {
            return null;
        }

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm != null ? cm.getActiveNetworkInfo() : null;
    }

    /**
     * @return {@link String} type and speed of network (e.g. WIFI (123.45 Mbps))
     */
    public static String getNetworkInfo(Context context) {
        String type = getNetworkTypeString(context);
        String speed;

        switch (getNetworkType(context)) {
            case ConnectivityManager.TYPE_MOBILE:
                speed = getNetworkSpeed(context);
                break;
            case ConnectivityManager.TYPE_WIFI:
                speed = getNetworkSpeedWifi(context);
                break;
            case TYPE_NONE:
            case TYPE_NULL:
            default:
                speed = "?";
                break;
        }

        return type + " (" + speed + ")";
    }

    /**
     * @return {@link String} speed of network in Kbps (e.g. 12.34)
     */
    public static String getNetworkSpeed(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return "Could not get network speed";
        }

        NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());

        if (nc == null) {
            return "Could not get network speed";
        }

        return formatSpeedFromKbps(nc.getLinkDownstreamBandwidthKbps());
    }

    /**
     * @return {@link String} speed of network in Mbps (e.g. 12.34)
     */
    public static String getNetworkSpeedWifi(Context context) {
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wm == null) {
            return "Could not get network speed";
        }

        return formatSpeedFromMbps(wm.getConnectionInfo().getLinkSpeed());
    }

    /**
     * @return integer constant of the network type
     */
    public static int getNetworkType(Context context) {
        NetworkInfo info = getActiveNetworkInfo(context);

        if (info == null) {
            return TYPE_NULL;
        }

        if (!info.isConnected()) {
            return TYPE_NONE;
        }

        return info.getType();
    }

    /**
     * @return {@link String} representation of the network type
     */
    public static String getNetworkTypeString(Context context) {
        switch (getNetworkType(context)) {
            case ConnectivityManager.TYPE_MOBILE:
                return "MOBILE";
            case ConnectivityManager.TYPE_WIFI:
                return "WIFI";
            case TYPE_NONE:
                return "No network connection";
            case TYPE_NULL:
            default:
                return "Could not get network type";
        }
    }

    /**
     * @return true if a network connection is available; false otherwise
     */
    public static boolean isNetworkAvailable(Context context) {
        NetworkInfo info = getActiveNetworkInfo(context);
        return info != null && info.isConnected();
    }
}
