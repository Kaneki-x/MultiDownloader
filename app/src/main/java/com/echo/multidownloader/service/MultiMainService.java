package com.echo.multidownloader.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.echo.multidownloader.MultiDownloader;
import com.echo.multidownloader.db.ThreadDAO;
import com.echo.multidownloader.db.ThreadDAOImpl;
import com.echo.multidownloader.entitie.FileInfo;
import com.echo.multidownloader.entitie.MultiDownloadException;
import com.echo.multidownloader.task.DownloadTask;
import com.echo.multidownloader.util.StringUtils;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class MultiMainService extends Service {

    private static final String TAG = "MultiMainService";

    private static final int TASK_CHECK_SUCCESS = 0;
    private static final int TASK_CHECK_FAIL = 1;

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_STOP = "ACTION_STOP";

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service Stop");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_START.equals(intent.getAction())) {
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
            Log.d(TAG, fileInfo.getUrl()+"---->Download Start");
            // 启动初始化线程
            //new UnitThread(fileInfo).start();
            checkFileLength(fileInfo);
        } else if (ACTION_PAUSE.equals(intent.getAction())) {
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
            Log.d(TAG, fileInfo.getUrl()+"---->Download Pause");

            // 从集合中取出下载任务
            DownloadTask task = MultiDownloader.getInstance().getDownloadTaskFromQueue(fileInfo.getUrl());
            if (task != null) {
                task.isPause = true;
                MultiDownloader.getInstance().getExecutorTask().remove(task);
                MultiDownloader.getInstance().getMultiDownloadListenerHashMap().remove(fileInfo.getUrl());
                stopService();
            }
        } else if (ACTION_STOP.equals(intent.getAction())) {
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
            Log.d(TAG, fileInfo.getUrl()+"---->Download Stop");

            // 从集合中取出下载任务
            DownloadTask task = MultiDownloader.getInstance().getDownloadTaskFromQueue(fileInfo.getUrl());
            if (task != null) {
                task.isPause = true;
                ThreadDAO threadDAO = new ThreadDAOImpl(this);
                threadDAO.deleteThread(fileInfo.getUrl());
                File file = new File(MultiDownloader.getInstance().getDownPath(),
                        fileInfo.getFileName());
                if(file.exists())
                    file.delete();
                MultiDownloader.getInstance().getExecutorTask().remove(task);
                MultiDownloader.getInstance().getMultiDownloadListenerHashMap().remove(fileInfo.getUrl());
                stopService();
            }
        }
        return START_REDELIVER_INTENT;
    }

    private void stopService() {
        if(MultiDownloader.getInstance().getExecutorTask().size() == 0)
            stopSelf();
    }

    private void checkFileLength(final FileInfo mFileInfo) {
        //创建一个Request
        final Request request = new Request.Builder()
                .url(mFileInfo.getUrl())
                .get()
                .build();
        //new call
        Call call = MultiDownloader.getInstance().getOkHttpClient().newCall(request);
        //请求加入调度
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                mHandler.obtainMessage(TASK_CHECK_FAIL, mFileInfo).sendToTarget();
            }

            @Override
            public void onResponse(Response response) {
                RandomAccessFile raf = null;
                long length;
                try {
                    File dir = new File(MultiDownloader.getInstance().getDownPath());
                    if (!dir.exists()) {
                        dir.mkdir();
                    }
                    length = response.body().contentLength();
                    if(length <= 0)
                        throw new IOException("File content length invaild");
                    // 在本地创建文件
                    File file = new File(dir, mFileInfo.getFileName());
                    raf = new RandomAccessFile(file, "rwd");
                    // 设置文件长度
                    raf.setLength(length);
                    mFileInfo.setLength(length);
                    mHandler.obtainMessage(TASK_CHECK_SUCCESS, mFileInfo).sendToTarget();
                } catch (IOException e) {
                    mHandler.obtainMessage(TASK_CHECK_FAIL, mFileInfo).sendToTarget();
                } finally {
                    if (raf != null) {
                        try {
                            raf.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    private final Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            FileInfo fileInfo = (FileInfo) msg.obj;
            if(msg.what == TASK_CHECK_SUCCESS) {
                Log.d(TAG, fileInfo.getUrl() + "---->Check File Length Success");
                DownloadTask task = MultiDownloader.getInstance().getDownloadTaskFromQueue(fileInfo.getUrl());
                task.setFileInfo(fileInfo);
                task.setThreadCount(StringUtils.caculateThreadCount(MultiMainService.this, fileInfo.getLength()));
                task.downLoad();
            } else {
                Log.d(TAG, fileInfo.getUrl() + "---->Check File Length Fail");
                MultiDownloader.getInstance().getExecutorTask().remove(MultiDownloader.getInstance().getDownloadTaskFromQueue(fileInfo.getUrl()));
                MultiDownloader.getInstance().getMultiDownloadListenerHashMap().get(fileInfo.getUrl()).onFail(new MultiDownloadException(0, new Exception("Check file length fail, Please check your internet connection and retry late")));
                MultiDownloader.getInstance().getMultiDownloadListenerHashMap().remove(fileInfo.getUrl());
                stopService();
            }
        }
    };
}
