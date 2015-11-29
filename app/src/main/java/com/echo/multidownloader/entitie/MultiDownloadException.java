package com.echo.multidownloader.entitie;

/**
 * Created by cmcc on 11/29/15.
 */
public class MultiDownloadException {

    private String exceptionMessage;

    public MultiDownloadException(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }
}
