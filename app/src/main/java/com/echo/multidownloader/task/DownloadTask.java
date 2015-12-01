package com.echo.multidownloader.task;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.echo.multidownloader.MultiDownloader;
import com.echo.multidownloader.db.ThreadDAO;
import com.echo.multidownloader.db.ThreadDAOImpl;
import com.echo.multidownloader.entitie.FileInfo;
import com.echo.multidownloader.entitie.MultiDownloadException;
import com.echo.multidownloader.entitie.ThreadInfo;
import com.echo.multidownloader.listener.MultiDownloadListener;
import com.echo.multidownloader.util.StringUtils;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class DownloadTask {

    private static final String TAG = "DownloadTask";

    private static final int TASK_LOADING = 0;
    private static final int TASK_SUCCESS = 1;
    private static final int TASK_FAIL = 2;

    private FileInfo mFileInfo = null;
    private ThreadDAO mDao = null;
    private Handler mHandler;

    private long mFinised = 0;
    private int mThreadCount = 1;
    private String speed;

    private List<MultiCallback> multiCallbackList = null;

    public boolean isPause = false;

    /**
     * @param mContext
     * @param mFileInfo
     */
    public DownloadTask(Context mContext, FileInfo mFileInfo) {
        this.mFileInfo = mFileInfo;
        this.mThreadCount = 3;
        mDao = new ThreadDAOImpl(mContext);
    }

    public FileInfo getFileInfo() {
        return mFileInfo;
    }

    public void setFileInfo(FileInfo mFileInfo) {
        this.mFileInfo = mFileInfo;
    }

    public void downLoad() {
        initHandler();
        List<ThreadInfo> threads = mDao.getThreads(mFileInfo.getUrl());
        ThreadInfo threadInfo = null;

        if (0 == threads.size()) {
            long len = mFileInfo.getLength() / mThreadCount;
            for (int i = 0; i < mThreadCount; i++) {
                threadInfo = new ThreadInfo(i, mFileInfo.getUrl(),
                        len * i, (i + 1) * len - 1);

                if (mThreadCount - 1 == i) {
                    threadInfo.setEnd(mFileInfo.getLength());
                }

                threads.add(threadInfo);
                mDao.insertThread(threadInfo);
            }
        }

        multiCallbackList = new ArrayList<>();
        for (ThreadInfo info : threads) {
            MultiCallback multiCallback = new MultiCallback(info);
            multiCallbackList.add(multiCallback);
        }
        downloadByOkHttp();
    }

    private void initHandler() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case TASK_SUCCESS:
                        Log.d(TAG, mFileInfo.getUrl()+"---->Download Success");
                        MultiDownloader.getInstance().getExecutorTask().remove(MultiDownloader.getInstance().getDownloadTaskFromQueue(mFileInfo.getUrl()));
                        MultiDownloader.getInstance().getMultiDownloadListenerHashMap().get(mFileInfo.getUrl()).onSuccess();
                        MultiDownloader.getInstance().getMultiDownloadListenerHashMap().remove(mFileInfo.getUrl());
                        break;
                    case TASK_LOADING:
                        MultiDownloadListener listener = MultiDownloader.getInstance().getMultiDownloadListenerHashMap().get(mFileInfo.getUrl());
                        if(listener != null)
                            listener.onLoading(mFinised, mFileInfo.getLength(), speed);
                        break;
                    case TASK_FAIL:
                        Log.d(TAG, mFileInfo.getUrl()+"---->Download Fail");
                        MultiDownloader.getInstance().getExecutorTask().remove(MultiDownloader.getInstance().getDownloadTaskFromQueue(mFileInfo.getUrl()));
                        listener = MultiDownloader.getInstance().getMultiDownloadListenerHashMap().get(mFileInfo.getUrl());
                        if(listener != null) {
                            MultiDownloader.getInstance().getMultiDownloadListenerHashMap().get(mFileInfo.getUrl()).onFail((MultiDownloadException) msg.obj);
                            MultiDownloader.getInstance().getMultiDownloadListenerHashMap().remove(mFileInfo.getUrl());
                        }
                        break;
                }
            }
        };
    }

    private void downloadByOkHttp() {
        for(MultiCallback callback : multiCallbackList) {
            //创建一个Request
            long start = callback.mThreadInfo.getStart() + callback.mThreadInfo.getFinished();
            long end = callback.mThreadInfo.getEnd();
            final Request request = new Request.Builder()
                    .url(callback.mThreadInfo.getUrl())
                    .header("Range", "bytes=" + start + "-" + end)
                    .get()
                    .build();
            //new call
            Call call = MultiDownloader.getInstance().getOkHttpClient().newCall(request);
            //请求加入调度
            call.enqueue(callback);
        }
    }

    private class MultiCallback implements Callback {

        private ThreadInfo mThreadInfo = null;
        public boolean isFinished = false;

        public MultiCallback(ThreadInfo mThreadInfo) {
            this.mThreadInfo = mThreadInfo;
        }

        @Override
        public void onFailure(Request request, IOException e) {
            mHandler.obtainMessage(TASK_FAIL, new MultiDownloadException(0, e)).sendToTarget();
        }

        @Override
        public void onResponse(Response response) {
            RandomAccessFile raf = null;
            InputStream inputStream = null;
            try {
                if(response.code() != 206)
                    throw new Exception("The file server return error");
                long start = mThreadInfo.getStart() + mThreadInfo.getFinished();
                File file = new File(MultiDownloader.getInstance().getDownPath(),
                        mFileInfo.getFileName());
                raf = new RandomAccessFile(file, "rwd");
                raf.seek(start);
                mFinised += mThreadInfo.getFinished();

                inputStream = response.body().byteStream();
                byte buf[] = new byte[1024 << 2];
                int len = -1;
                long pb_time = System.currentTimeMillis();
                long sp_time = pb_time;
                speed = "0k/s";
                int sec_total = 0;
                while ((len = inputStream.read(buf)) != -1) {
                    raf.write(buf, 0, len);
                    mFinised += len;
                    mThreadInfo.setFinished(mThreadInfo.getFinished() + len);
                    if (System.currentTimeMillis() - pb_time > 500) {
                        pb_time = System.currentTimeMillis();
                        mHandler.obtainMessage(TASK_LOADING).sendToTarget();
                    }
                    if (System.currentTimeMillis() - sp_time > 1300) {
                        speed = StringUtils.getDownloadSpeed(sec_total, System.currentTimeMillis() - sp_time);
                        sp_time = System.currentTimeMillis();
                        sec_total = 0;
                    } else
                        sec_total += len;

                    if (isPause) {
                        mDao.updateThread(mThreadInfo.getUrl(),
                                mThreadInfo.getId(),
                                mThreadInfo.getFinished());
                        return;
                    }
                }
                isFinished = true;
                checkAllCallbackFinished();
            } catch (Exception e) {
                mHandler.obtainMessage(TASK_FAIL, new MultiDownloadException(StringUtils.getCurrentPercent(mFinised, mFileInfo.getLength()), e)).sendToTarget();
                mDao.updateThread(mThreadInfo.getUrl(), mThreadInfo.getId(), mThreadInfo.getFinished());
                e.printStackTrace();
            } finally {
                try {
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

    private synchronized void checkAllCallbackFinished() {
        boolean allFinished = true;

        for (MultiCallback callback : multiCallbackList) {
            if (!callback.isFinished) {
                allFinished = false;
                break;
            }
        }

        if (allFinished) {
            mDao.deleteThread(mFileInfo.getUrl());
            mHandler.obtainMessage(TASK_SUCCESS).sendToTarget();
        }
    }
}
