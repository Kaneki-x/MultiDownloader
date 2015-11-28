package com.echo.multidownloader.task;

/**
 * Created by cmcc on 11/28/15.
 */
public interface MultiDownloadListener {

    void onSuccess();

    void onLoading(long current_length, long total_length);

    void onFail();
}
