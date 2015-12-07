package com.echo.multidownloader.util;

import android.content.Context;

import java.text.DecimalFormat;

/**
 * Created by cmcc on 11/29/15.
 */
public class StringUtils {

    private final static long _1MB = 1 * 1024 *1024;
    private final static long _5MB = 5 * 1024 *1024;
    private final static long _10MB = 10 * 1024 *1024;
    private final static long _50MB = 50 * 1024 *1024;
    private final static long _100MB = 100 * 1024 *1024;
    private final static long _500MB = 500 * 1024 *1024;

    public static String getDownloadSpeed(int len, long time) {
        String str = "0k/s";
        if(time == 0)
            return str;
        float sec = time / 1000;
        float speed = len / 1024 / sec;

        if(speed > 1000) {
            str = new DecimalFormat("0.00").format(speed/1024) + "m/s";
            return str;
        } else {
            str = ((int)speed) + "k/s";
            return str;
        }
    }

    public static int getCurrentPercent(long current, long total) {
        if(total == 0)
            return 0;
        else
            return (int)(100 * current / total);
    }

    public static int caculateThreadCount(Context context, long size) {
        if(NetUtils.isWifiConnected(context)) {
            if(size <= _50MB)
                return 1;
            if(size > _50MB && size < _100MB)
                return 3;
            if(size >= _100MB)
                return 5;
        } else if(NetUtils.isMobileConnected(context)) {
            if(size <= _1MB)
                return 1;
            if(size > _5MB && size < _10MB)
                return 3;
            if(size >= _10MB)
                return 5;
        }
        return 1;
    }
}
