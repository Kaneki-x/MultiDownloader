package com.echo.multidownloader;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.echo.multidownloader.config.MultiDownloaderConfiguration;
import com.echo.multidownloader.entities.FileInfo;
import com.echo.multidownloader.services.MultiMainService;
import com.echo.multidownloader.task.DownloadTask;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by Lion on 2015/11/20 0020.
 */
public class MultiDownloader {

    private static volatile MultiDownloader instance = null;

    private int maxThreadNums;
    private String downPath;
    private Context mContext;
    private ExecutorThread executorThread;

    private Queue<FileInfo> fileInfoQueue;
    private Map<String, DownloadTask> mTasks;

    private MultiDownloader() {
        fileInfoQueue = new LinkedBlockingDeque<FileInfo>();
        mTasks = new LinkedHashMap<String, DownloadTask>();
    }

    private void startExecutorService(FileInfo fileInfo, String action) {
        Intent intent = new Intent(mContext, MultiMainService.class);
        intent.setAction(action);
        intent.putExtra("fileInfo", fileInfo);
        mContext.startService(intent);
    }

    private FileInfo isDownloadTaskExist(String url) {
        if(mTasks.containsKey(url)) {
            Log.d("bobo", "task exit");
            return mTasks.get(url).getFileInfo();
        } else {
            Iterator<FileInfo> it = fileInfoQueue.iterator();
            while(it.hasNext()) {
                FileInfo fileInfo = it.next();
                if(fileInfo.getUrl().equals(url)) {
                    Log.d("bobo", "queue exit");
                    return fileInfo;
                }
            }
            return null;
        }
    }

    public void init(MultiDownloaderConfiguration configuration) {
        this.maxThreadNums = configuration.getMaxThreadNums();
        this.downPath = configuration.getDownPath();
        this.mContext = configuration.getContext();
        this.executorThread = new ExecutorThread();
        this.executorThread.start();
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

    public int getMaxThreadNums() {
        return maxThreadNums;
    }

    public String getDownPath() {
        return downPath;
    }

    public Map<String, DownloadTask> getExecutorTask() {
        return mTasks;
    }

    public void addDownloadTaskIntoExecutorService(String fileName, String url) {
        if(isDownloadTaskExist(url) == null) {
            fileInfoQueue.offer(new FileInfo(url, fileName, 0, 0));
            Log.d("bobo", "1");
        }
    }

    public boolean pauseDownloadTaskFromExecutorService(String url) {
        FileInfo fileInfo = isDownloadTaskExist(url);
        if(fileInfo == null) {
            return false;
        } else {
            if(fileInfoQueue.contains(fileInfo))
                fileInfoQueue.remove(fileInfo);
            else
                startExecutorService(fileInfo, MultiMainService.ACTION_PAUSE);
            return true;
        }
    }

    public boolean stopDownloadTaskFromExecutorService(String url) {
        FileInfo fileInfo = isDownloadTaskExist(url);
        if(fileInfo == null) {
            return false;
        } else {
            if(fileInfoQueue.contains(fileInfo))
                fileInfoQueue.remove(fileInfo);
            else
                startExecutorService(fileInfo, MultiMainService.ACTION_STOP);
            return true;
        }
    }

    private class ExecutorThread extends Thread {

        @Override
        public void run() {
            while (true) {
                try {
                    while (mTasks.size() < maxThreadNums) {
                        FileInfo fileInfo = fileInfoQueue.poll();
                        if(fileInfo != null)
                            startExecutorService(fileInfo, MultiMainService.ACTION_START);
                        else
                            break;
                    }
                    Thread.sleep(500);
                } catch (Exception e) {

                }
            }
        }
    }
}
