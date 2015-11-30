package com.echo.multidownloader.entitie;

/**
 * Created by cmcc on 11/29/15.
 */
public class MultiDownloadException {

    private int current_percent;
    private Exception exception;

    public MultiDownloadException(int current_percent, Exception exception) {
        this.current_percent = current_percent;
        this.exception = exception;
    }

    public int getPercent() {
        return current_percent;
    }

    public Exception getException() {
        return exception;
    }
}
