package com.echo.multidownloader.listener;

import com.echo.multidownloader.entitie.MultiDownloadException;

/**
 * Created by cmcc on 11/28/15.
 */
public interface MultiDownloadListener {

    void onSuccess();

    void onLoading(long current_length, long total_length, String speed);

    void onFail(MultiDownloadException exception);
}
