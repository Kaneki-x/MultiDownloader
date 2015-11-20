package com.echo.multidownloader;

import com.echo.multidownloader.config.MultiDownloaderConfiguration;

/**
 * Created by Lion on 2015/11/20 0020.
 */
public class MultiDownloader {

    private static volatile MultiDownloader instance = null;

    public static MultiDownloader getInstance() {
        if (instance == null) {
            synchronized (MultiDownloader.class) {
                if (instance == null) {
                    instance = new MultiDownloader();
                }
            }
        }
        return instance;
    }


    private void init(MultiDownloaderConfiguration configuration) {

    }
}
