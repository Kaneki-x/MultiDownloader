package com.echo.multidownloader.util;

import java.text.DecimalFormat;

/**
 * Created by cmcc on 11/29/15.
 */
public class StringUtils {

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
}
