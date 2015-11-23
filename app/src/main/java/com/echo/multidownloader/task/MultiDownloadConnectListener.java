package com.echo.multidownloader.task;

/**
 * Created by Lion on 2015/11/23 0023.
 */
public interface MultiDownloadConnectListener {

    public void onSuccess();

    public void onProgress(long currentLength, long totalLength);

    public void onFail();
}
