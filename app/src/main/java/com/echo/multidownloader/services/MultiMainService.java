package com.echo.multidownloader.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.echo.multidownloader.MultiDownloader;
import com.echo.multidownloader.db.ThreadDAO;
import com.echo.multidownloader.db.ThreadDAOImpl;
import com.echo.multidownloader.entities.FileInfo;
import com.echo.multidownloader.task.DownloadTask;
import com.echo.multidownloader.event.MultiDownloadConnectEvent;

import org.apache.http.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import de.greenrobot.event.EventBus;

public class MultiMainService extends Service {

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_STOP = "ACTION_STOP";

    private final static int UNIT_CHECK_SUCCESS = 0;
    private final static int UNIT_CHECK_FAIL = 1;

    private final static String TAG = "MultiMainService";

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_START.equals(intent.getAction())) {
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
            Log.i(TAG, "Start:" + fileInfo.toString());
            DownloadTask task = new DownloadTask(MultiMainService.this, fileInfo, 3);
            // 把下载任务添加到集合中
            synchronized (this) {
                MultiDownloader.getInstance().getExecutorTask().put(fileInfo.getUrl(), task);
            }
            // 启动初始化线程
            new UnitThread(fileInfo).start();
        } else if (ACTION_PAUSE.equals(intent.getAction())) {
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
            Log.i(TAG, "Pause:" + fileInfo.toString());

            // 从集合中取出下载任务
            DownloadTask task = MultiDownloader.getInstance().getExecutorTask().get(fileInfo.getUrl());
            if (task != null) {
                task.isPause = true;
                synchronized (this) {
                    MultiDownloader.getInstance().getExecutorTask().remove(fileInfo.getUrl());
                }
            }
        } else if (ACTION_STOP.equals(intent.getAction())) {
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
            Log.i(TAG, "Stop:" + fileInfo.toString());

            // 从集合中取出下载任务
            DownloadTask task = MultiDownloader.getInstance().getExecutorTask().get(fileInfo.getUrl());
            if (task != null) {
                task.isPause = true;
                ThreadDAO threadDAO = new ThreadDAOImpl(this);
                threadDAO.deleteThread(fileInfo.getUrl());
                File file = new File(MultiDownloader.getInstance().getDownPath(),
                        fileInfo.getFileName());
                if(file.exists())
                    file.delete();
                synchronized (this) {
                    MultiDownloader.getInstance().getExecutorTask().remove(fileInfo.getUrl());
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case UNIT_CHECK_SUCCESS:
                    FileInfo fileInfo = (FileInfo) msg.obj;
                    // 启动下载任务
                    DownloadTask task = MultiDownloader.getInstance().getExecutorTask().get(fileInfo.getUrl());
                    task.downLoad();
                    break;
            }
        };
    };

    private class UnitThread extends Thread {
        private FileInfo mFileInfo = null;

        public UnitThread(FileInfo mFileInfo) {
            this.mFileInfo = mFileInfo;
        }

        /**
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            HttpURLConnection connection = null;
            RandomAccessFile raf = null;

            try {
                // 连接网络文件
                URL url = new URL(mFileInfo.getUrl());
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setRequestMethod("GET");
                long length = -1;

                if (connection.getResponseCode() == HttpStatus.SC_OK) {
                    // 获得文件的长度
                    length = connection.getContentLength();
                }

                if (length <= 0) {
                    return;
                }

                File dir = new File(MultiDownloader.getInstance().getDownPath());
                if (!dir.exists()) {
                    dir.mkdir();
                }

                // 在本地创建文件
                File file = new File(dir, mFileInfo.getFileName());
                raf = new RandomAccessFile(file, "rwd");
                // 设置文件长度
                raf.setLength(length);
                mFileInfo.setLength(length);
                mHandler.obtainMessage(UNIT_CHECK_SUCCESS, mFileInfo).sendToTarget();
            } catch (Exception e) {
                synchronized (this) {
                    MultiDownloader.getInstance().getExecutorTask().remove(mFileInfo.getUrl());
                }
                EventBus.getDefault().post(new MultiDownloadConnectEvent(mFileInfo.getUrl(), MultiDownloadConnectEvent.TYPE_FAIL));
                e.printStackTrace();
            } finally {
                if (connection != null)
                    connection.disconnect();
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
