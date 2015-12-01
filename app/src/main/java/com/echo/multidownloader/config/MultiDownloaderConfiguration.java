package com.echo.multidownloader.config;

import android.content.Context;
import android.os.Environment;

/**
 * Created by Lion on 2015/11/20 0020.
 */
public class MultiDownloaderConfiguration {

    private final int maxThreadNums;
    private final String downPath;
    private final Context context;

    private MultiDownloaderConfiguration(Builder builder) {

        this.downPath = builder.downPath;
        this.maxThreadNums = builder.maxThreadNums;
        this.context = builder.context;
    }

    public String getDownPath() {
        return downPath;
    }

    public int getMaxThreadNums() {
        return maxThreadNums;
    }

    public Context getContext() {
        return context;
    }

    public static MultiDownloaderConfiguration createDefault(Context context) {
        return new Builder(context).build();
    }

    public static class Builder {

        private final Context context;

        private String downPath;
        private int maxThreadNums;

        public Builder(Context context) {
            this.context = context.getApplicationContext();
        }

        private Builder setDownloadPath(String downloadPath) {
            this.downPath = downloadPath;
            return this;
        }

        private Builder setMaxThreadNums(int maxThreadNums) {
            this.maxThreadNums = maxThreadNums;
            return this;
        }

        private void initEmptyFieldsWithDefaultValues() {
            if(maxThreadNums <= 0)
                maxThreadNums = 3;
            if(downPath == null)
                downPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/downloads/";
        }

        public MultiDownloaderConfiguration build() {
            initEmptyFieldsWithDefaultValues();
            return new MultiDownloaderConfiguration(this);
        }

    }

}
