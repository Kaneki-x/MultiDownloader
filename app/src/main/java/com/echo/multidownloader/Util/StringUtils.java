package com.echo.multidownloader.Util;

/**
 * Created by cmcc on 11/29/15.
 */
public class StringUtils {

    public static String getDownloadSpeed(int len, long time) {
        float sec = time / 1000;
        float speed = len / 1024 / sec;
        if(speed >  1024) {
            return new String(speed/1024 + "m/s");
        } else {
            return new String(speed + "k/s");
        }
    }
}
