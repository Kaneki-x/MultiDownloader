package com.echo.multidownloader.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class MultiMainService extends Service {

    private final static String TAG = "MultiMainService";

    public void init(Context context) {

    }

    public void release() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }


}
