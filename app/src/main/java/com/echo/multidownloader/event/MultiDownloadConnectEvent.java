package com.echo.multidownloader.event;

/**
 * Created by Lion on 2015/11/24 0024.
 */
public class MultiDownloadConnectEvent {

    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAIL = 1;
    public static final int TYPE_LOADING = 2;

    private int type;
    private String url;
    private long current_percent = 0;
    private long total_percent = 0;

    public MultiDownloadConnectEvent(String url, int type) {
        this.url = url;
        this.type = type;
    }

    public long getCurrent_percent() {
        return current_percent;
    }

    public long getTotal_percent() {
        return total_percent;
    }

    public void setCurrent_percent(long current_percent) {
        this.current_percent = current_percent;
    }

    public void setTotal_percent(long total_percent) {
        this.total_percent = total_percent;
    }

    public String getUrl() {
        return url;
    }

    public int getType() {
        return type;
    }
}
