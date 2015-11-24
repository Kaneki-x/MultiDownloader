package com.echo.multidownloader.task;

import android.content.Context;
import android.util.Log;

import com.echo.multidownloader.MultiDownloader;
import com.echo.multidownloader.db.ThreadDAO;
import com.echo.multidownloader.db.ThreadDAOImpl;
import com.echo.multidownloader.entities.FileInfo;
import com.echo.multidownloader.entities.ThreadInfo;

import org.apache.http.HttpStatus;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;


public class DownloadTask {

    private static final int TASK_UPDATE = 0;
    private static final int TASK_FINISH = 1;
    private static final int TASK_ERROR = 2;

    private Context mContext = null;
    private FileInfo mFileInfo = null;
    private ThreadDAO mDao = null;
    private int mFinised = 0;
    private int mThreadCount = 1;
    private List<DownloadThread> mDownloadThreadList = null;

    public boolean isPause = false;

    /**
     *@param mContext
     *@param mFileInfo
     */
    public DownloadTask(Context mContext, FileInfo mFileInfo, int count) {
        this.mContext = mContext;
        this.mFileInfo = mFileInfo;
        this.mThreadCount = count;
        mDao = new ThreadDAOImpl(this.mContext);
    }

    public FileInfo getFileInfo() {
        return mFileInfo;
    }

    public void downLoad() {
        List<ThreadInfo> threads = mDao.getThreads(mFileInfo.getUrl());
        ThreadInfo threadInfo = null;

        if (0 == threads.size()) {
            long len = mFileInfo.getLength() / mThreadCount;
            for (int i = 0; i < mThreadCount; i++) {
                threadInfo = new ThreadInfo(i, mFileInfo.getUrl(),
                        len * i, (i + 1) * len - 1, 0);

                if (mThreadCount - 1 == i) {
                    threadInfo.setEnd(mFileInfo.getLength());
                }

                threads.add(threadInfo);
                mDao.insertThread(threadInfo);
            }
        }

        mDownloadThreadList = new ArrayList<DownloadThread>();
        for (ThreadInfo info : threads) {
            DownloadThread thread = new DownloadThread(info);
            thread.start();
            mDownloadThreadList.add(thread);
        }
    }

    private class DownloadThread extends Thread {
        private ThreadInfo mThreadInfo = null;
        public boolean isFinished = false;

        /**
         *@param mInfo
         */
        public DownloadThread(ThreadInfo mInfo) {
            this.mThreadInfo = mInfo;
        }

        /**
         * @see Thread#run()
         */
        @Override
        public void run() {
            HttpURLConnection connection = null;
            RandomAccessFile raf = null;
            InputStream inputStream = null;

            try {
                URL url = new URL(mThreadInfo.getUrl());
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setRequestMethod("GET");
                long start = mThreadInfo.getStart() + mThreadInfo.getFinished();
                connection.setRequestProperty("Range",
                        "bytes=" + start + "-" + mThreadInfo.getEnd());
                File file = new File(MultiDownloader.getInstance().getDownPath(),
                        mFileInfo.getFileName());
                raf = new RandomAccessFile(file, "rwd");
                raf.seek(start);
                mFinised += mThreadInfo.getFinished();
                Log.i("mFinised", mThreadInfo.getId() + "finished = " + mThreadInfo.getFinished());
                if (connection.getResponseCode() == HttpStatus.SC_PARTIAL_CONTENT) {
                    inputStream = connection.getInputStream();
                    byte buf[] = new byte[1024 << 2];
                    int len = -1;
                    long time = System.currentTimeMillis();
                    while ((len = inputStream.read(buf)) != -1) {
                        raf.write(buf, 0, len);
                        mFinised += len;
                        mThreadInfo.setFinished(mThreadInfo.getFinished() + len);
                        if (System.currentTimeMillis() - time > 1000) {
                            time = System.currentTimeMillis();
                            MultiDownloadConnectEvent multiDownloadConnectEvent = new MultiDownloadConnectEvent(mFileInfo.getUrl(), MultiDownloadConnectEvent.TYPE_LOADING);
                            multiDownloadConnectEvent.setCurrent_percent(mFinised);
                            multiDownloadConnectEvent.setTotal_percent(mFileInfo.getLength());
                            EventBus.getDefault().post(multiDownloadConnectEvent);
                        }

                        if (isPause) {
                            mDao.updateThread(mThreadInfo.getUrl(),
                                    mThreadInfo.getId(),
                                    mThreadInfo.getFinished());

                            Log.i("mThreadInfo", mThreadInfo.getId() + "finished = " + mThreadInfo.getFinished());

                            return;
                        }
                    }

                    isFinished = true;
                    checkAllThreadFinished();
                }
            } catch (Exception e) {
                MultiDownloader.getInstance().getExecutorTask().remove(mFileInfo.getUrl());
                EventBus.getDefault().post(new MultiDownloadConnectEvent(mFileInfo.getUrl(), MultiDownloadConnectEvent.TYPE_FAIL));
                e.printStackTrace();
            } finally {
                try {
                    if (connection != null)
                        connection.disconnect();
                    if (raf != null)
                        raf.close();
                    if (inputStream != null)
                        inputStream.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    private synchronized void checkAllThreadFinished() {
        boolean allFinished = true;

        for (DownloadThread thread : mDownloadThreadList) {
            if (!thread.isFinished) {
                allFinished = false;
                break;
            }
        }

        if (allFinished) {
            mDao.deleteThread(mFileInfo.getUrl());
            MultiDownloader.getInstance().getExecutorTask().remove(mFileInfo.getUrl());
            EventBus.getDefault().post(new MultiDownloadConnectEvent(mFileInfo.getUrl(), MultiDownloadConnectEvent.TYPE_SUCCESS));
        }
    }
}
