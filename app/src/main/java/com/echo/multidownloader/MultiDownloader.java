package com.echo.multidownloader;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.echo.multidownloader.config.MultiDownloaderConfiguration;
import com.echo.multidownloader.entitie.FileInfo;
import com.echo.multidownloader.entitie.MultiDownloadException;
import com.echo.multidownloader.listener.MultiDownloadListener;
import com.echo.multidownloader.service.MultiMainService;
import com.echo.multidownloader.task.DownloadTask;
import com.echo.multidownloader.util.NetUtils;
import com.squareup.okhttp.OkHttpClient;

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by Lion on 2015/11/20 0020.
 */
public class MultiDownloader {

    private final static String TAG = "MultiDownloader";

    private static volatile MultiDownloader instance = null;

    private String downPath;
    private Context mContext;

    private BlockingQueue<DownloadTask> downloadTaskBlockingQueue;
    private BlockingQueue<FileInfo> fileInfoBlockingQueue;
    private ConcurrentHashMap<String, MultiDownloadListener> multiDownloadListenerHashMap;

    private OkHttpClient mOkHttpClient;

    private Handler mHandler;

    private MultiDownloader() {
        //创建okHttpClient对象
        mOkHttpClient = new OkHttpClient();
        mOkHttpClient.setReadTimeout(2, TimeUnit.MINUTES);
        mOkHttpClient.setConnectTimeout(20, TimeUnit.SECONDS);
        fileInfoBlockingQueue = new LinkedBlockingQueue<>();
        multiDownloadListenerHashMap = new ConcurrentHashMap<>();
        mHandler = new Handler();
    }

    private void startExecutorService(FileInfo fileInfo, String action) {
        Intent intent = new Intent(mContext, MultiMainService.class);
        intent.setAction(action);
        intent.putExtra("fileInfo", fileInfo);
        mContext.startService(intent);
    }

    private FileInfo isDownloadTaskExist(String url) {
        Iterator<FileInfo> fileInfoIterator = fileInfoBlockingQueue.iterator();
        while(fileInfoIterator.hasNext()) {
            FileInfo fileInfo = fileInfoIterator.next();
            if(fileInfo.getUrl().equals(url)) {
                return fileInfo;
            }
        }
        Iterator<DownloadTask> downloadTaskIterator = downloadTaskBlockingQueue.iterator();
        while(downloadTaskIterator.hasNext()) {
            DownloadTask downloadTask = downloadTaskIterator.next();
            if(downloadTask.getFileInfo().getUrl().equals(url)) {
                return downloadTask.getFileInfo();
            }
        }
        return null;
    }

    public void init(MultiDownloaderConfiguration configuration) {
        int maxThreadNums = configuration.getMaxThreadNums();
        this.downloadTaskBlockingQueue = new LinkedBlockingQueue<>(maxThreadNums);
        this.downPath = configuration.getDownPath();
        this.mContext = configuration.getContext();
        ExecutorThread executorThread = new ExecutorThread();
        executorThread.start();
    }

    public static MultiDownloader getInstance() {
        if (instance == null) {
            synchronized (MultiDownloader.class) {
                if (instance == null) {
                    instance = new MultiDownloader();
                }
            }
        }
        return instance;
    }

    public String getDownPath() {
        return downPath;
    }

    public BlockingQueue<DownloadTask> getExecutorTask() {
        return downloadTaskBlockingQueue;
    }

    public ConcurrentHashMap<String, MultiDownloadListener> getMultiDownloadListenerHashMap() { return multiDownloadListenerHashMap; }

    public OkHttpClient getOkHttpClient() { return mOkHttpClient; }

    public DownloadTask getDownloadTaskFromQueue(String url) {
        Iterator<DownloadTask> downloadTaskIterator = downloadTaskBlockingQueue.iterator();
        while(downloadTaskIterator.hasNext()) {
            DownloadTask downloadTask = downloadTaskIterator.next();
            if(downloadTask.getFileInfo().getUrl().equals(url)) {
                return downloadTask;
            }
        }
        return null;
    }

    public void addDownloadTaskIntoExecutorService(String fileName, String url, MultiDownloadListener multiDownloadListener) {
        if(isDownloadTaskExist(url) == null) {
            Log.d(TAG, url+"---->Add To Ready Queue");
            fileInfoBlockingQueue.add(new FileInfo(url, fileName));
            multiDownloadListenerHashMap.put(url, multiDownloadListener);
        }
    }

    public void pauseDownloadTaskFromExecutorService(String url) {
        FileInfo fileInfo = isDownloadTaskExist(url);
        if(fileInfo != null) {
            if(fileInfoBlockingQueue.contains(fileInfo)) {
                Log.d(TAG, fileInfo.getUrl()+"---->Pause True Remove From Ready Queue");
                fileInfoBlockingQueue.remove(fileInfo);
                multiDownloadListenerHashMap.remove(url);
            } else
                startExecutorService(fileInfo, MultiMainService.ACTION_PAUSE);
        }
    }

    public void stopDownloadTaskFromExecutorService(String url) {
        FileInfo fileInfo = isDownloadTaskExist(url);
        if(fileInfo == null) {
            if(fileInfoBlockingQueue.contains(fileInfo)) {
                Log.d(TAG, fileInfo.getUrl()+"---->Stop True Remove From Ready Queue");
                fileInfoBlockingQueue.remove(fileInfo);
                multiDownloadListenerHashMap.remove(url);
            } else
                startExecutorService(fileInfo, MultiMainService.ACTION_STOP);
        }
    }

    private class ExecutorThread extends Thread {

        @Override
        public void run() {
            while (true) {
                try {
                    Log.d(TAG, "ExecutorThread Get From Ready Queue");
                    final FileInfo fileInfo = fileInfoBlockingQueue.take();
                    if(NetUtils.isNetworkConnected(mContext)) {
                        DownloadTask task = new DownloadTask(mContext, fileInfo);
                        Log.d(TAG, "ExecutorThread Put In Task Queue");
                        downloadTaskBlockingQueue.put(task);
                        Log.d(TAG, fileInfo.getUrl()+"---->Start Service");
                        startExecutorService(fileInfo, MultiMainService.ACTION_START);
                    } else {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                multiDownloadListenerHashMap.get(fileInfo.getUrl()).onFail(new MultiDownloadException(0, new Exception("Network connect error")));
                                multiDownloadListenerHashMap.remove(fileInfo.getUrl());
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.d(TAG, e.toString());
                }
            }
        }
    }
}
