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
            Log.d(TAG, fileInfo.getUrl()+"---->Download Start");
            // 启动初始化线程
            new UnitThread(fileInfo).start();
        } else if (ACTION_PAUSE.equals(intent.getAction())) {
            FileInfo fileInfo = (FileInfo) intent.getSerializableExtra("fileInfo");
            Log.d(TAG, fileInfo.getUrl()+"---->Download Pause");

            // 从集合中取出下载任务
            DownloadTask task = MultiDownloader.getInstance().getDownloadTaskFromQueue(fileInfo.getUrl());
            if (task != null) {
                task.isPause = true;
                MultiDownloader.getInstance().getExecutorTask().remove(task);
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
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private final Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            FileInfo fileInfo = (FileInfo) msg.obj;
            Log.d(TAG, fileInfo.getUrl()+"---->Check Length Success");
            DownloadTask task = MultiDownloader.getInstance().getDownloadTaskFromQueue(fileInfo.getUrl());
            task.setFileInfo(fileInfo);
            task.downLoad();
        }
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
                mHandler.obtainMessage(0, mFileInfo).sendToTarget();
            } catch (Exception e) {
                Log.d(TAG, mFileInfo.getUrl()+"---->Check Length Fail");
                MultiDownloader.getInstance().getExecutorTask().remove(mFileInfo.getUrl());
                EventBus.getDefault().post(new MultiDownloadConnectEvent(mFileInfo.getUrl(), MultiDownloadConnectEvent.TYPE_FAIL));
                e.printStackTrace();
                System.gc();
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
