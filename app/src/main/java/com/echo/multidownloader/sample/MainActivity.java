package com.echo.multidownloader.sample;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import com.echo.multidownloader.MultiDownloader;
import com.echo.multidownloader.R;
import com.echo.multidownloader.config.MultiDownloaderConfiguration;
import com.echo.multidownloader.entities.FileInfo;
import com.echo.multidownloader.event.MultiDownloadConnectEvent;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

public class MainActivity extends Activity {

    private ListView mListView = null;
    private List<FileInfo> mFileInfoList = null;
    private FileListAdapter mAdapter = null;

    private String[] urls = {"http://dlsw.baidu.com/sw-search-sp/2015_11_14_11/bind1/18099/rj_wb3210.exe",
            "http://dlsw.baidu.com/sw-search-sp/soft/24/13406/XiuXiu_V4.0.1.2002_BdSetup.1437647987.exe",
            "http://dlsw.baidu.com/sw-search-sp/soft/3a/12350/QQ_7.8.16379.0_setup.1446522220.exe",
            "http://dlsw.baidu.com/sw-search-sp/soft/b1/38200/WeChat_1.5.0.33_setup.1446175356.exe",
            "http://dlsw.baidu.com/sw-search-sp/2015_11_24_13/bind1/11383/rj_nz0378.exe"};
    private String[] names = {"rj_wb3210.exe",
            "XiuXiu_V4.0.1.2002_BdSetup.1437647987.exe",
            "QQ_7.8.16379.0_setup.1446522220.exe",
            "WeChat_1.5.0.33_setup.1446175356.exe",
            "rj_nz0378.exe"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("bobo", "on create");
        setContentView(R.layout.activity_main);
        EventBus.getDefault().register(this);

        MultiDownloader.getInstance().init(MultiDownloaderConfiguration.createDefault(this));

        mListView = (ListView) findViewById(R.id.lv_downLoad);
        mFileInfoList = new ArrayList<FileInfo>();

        for(int i = 0; i < urls.length; i++) {
            FileInfo fileInfo = null;
            fileInfo = new FileInfo(
                    urls[i],
                    names[i]);

            mFileInfoList.add(fileInfo);
        }

        mAdapter = new FileListAdapter(this, mFileInfoList);
        mListView.setAdapter(mAdapter);
    }

    public void onEventMainThread(MultiDownloadConnectEvent multiDownloadConnectEvent) {
        for (FileInfo fileInfo : mFileInfoList) {
            if(fileInfo.getUrl().equals(multiDownloadConnectEvent.getUrl())) {
                switch (multiDownloadConnectEvent.getType()) {
                    case MultiDownloadConnectEvent.TYPE_SUCCESS:
                        Toast.makeText(this, fileInfo.getFileName()+"-->success", Toast.LENGTH_SHORT).show();
                        fileInfo.setPercent(0);
                        mAdapter.notifyDataSetChanged();
                        break;
                    case MultiDownloadConnectEvent.TYPE_LOADING:
                        fileInfo.setPercent((int)(multiDownloadConnectEvent.getCurrent_percent() * 100 / multiDownloadConnectEvent.getTotal_percent()));
                        mAdapter.notifyDataSetChanged();
                        break;
                    case MultiDownloadConnectEvent.TYPE_FAIL:
                        Toast.makeText(this, fileInfo.getFileName()+"-->fail", Toast.LENGTH_SHORT).show();
                        fileInfo.setPercent(0);
                        mAdapter.notifyDataSetChanged();
                        break;
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
