/*
 * @Title FileListAdapter.java
 * @Copyright Copyright 2010-2015 Yann Software Co,.Ltd All Rights Reserved.
 * @Description��
 * @author Yann
 * @date 2015-8-9 ����11:37:18
 * @version 1.0
 */
package com.echo.multidownloader.sample;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.echo.multidownloader.MultiDownloader;
import com.echo.multidownloader.R;
import com.echo.multidownloader.entitie.FileInfo;
import com.echo.multidownloader.listener.MultiDownloadListener;

import java.util.List;

public class FileListAdapter extends BaseAdapter {
	private Context mContext;
	private List<FileInfo> mList;
	
	public FileListAdapter(Context context, List<FileInfo> fileInfos) {
		this.mContext = context;
		this.mList = fileInfos;
	}
	
	/**
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount()
	{
		return mList.size();
	}

	/**
	 * @see android.widget.Adapter#getView(int, View, ViewGroup)
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder = null;
		final FileInfo fileInfo = mList.get(position);
		
		if (convertView != null) {
			viewHolder = (ViewHolder) convertView.getTag();
			if (!viewHolder.mFileName.getTag().equals(fileInfo.getUrl())) {
				convertView = null;
			}
		}
		
		if (null == convertView) {
			LayoutInflater inflater = LayoutInflater.from(mContext);
			convertView = inflater.inflate(R.layout.item, null);
			
			viewHolder = new ViewHolder(
					(TextView) convertView.findViewById(R.id.tv_fileName),
					(ProgressBar) convertView.findViewById(R.id.pb_progress),
					(Button) convertView.findViewById(R.id.btn_start),
					(Button) convertView.findViewById(R.id.btn_stop)
					);
			convertView.setTag(viewHolder);
			
			viewHolder.mFileName.setText(fileInfo.getFileName());
			viewHolder.mProgressBar.setMax(100);
			viewHolder.mStartBtn.setOnClickListener(new OnItemClickListener(viewHolder, fileInfo));
			viewHolder.mStopBtn.setOnClickListener(new OnItemClickListener(viewHolder, fileInfo));
			
			viewHolder.mFileName.setTag(fileInfo.getUrl());
		}
		
		viewHolder.mProgressBar.setProgress(fileInfo.getPercent());
		
		return convertView;
	}

    private class OnItemClickListener implements OnClickListener {

        ViewHolder viewHolder;
        FileInfo fileInfo;

        public OnItemClickListener(ViewHolder viewHolder, FileInfo fileInfo) {
            this.viewHolder = viewHolder;
            this.fileInfo = fileInfo;
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_start:
                    MultiDownloader.getInstance().addDownloadTaskIntoExecutorService(fileInfo.getFileName(), fileInfo.getUrl(), new MultiDownloadListener(){
						@Override
						public void onSuccess() {
							Toast.makeText(mContext, fileInfo.getFileName()+"下载成功", Toast.LENGTH_SHORT).show();
						}

						@Override
						public void onLoading(long current_length, long total_length) {
							viewHolder.mProgressBar.setProgress((int) (current_length * 100 / total_length));
						}

						@Override
						public void onFail() {
							Toast.makeText(mContext, fileInfo.getFileName()+"下载失败！", Toast.LENGTH_SHORT).show();
						}
					});
                    break;
                case R.id.btn_stop:
                    MultiDownloader.getInstance().pauseDownloadTaskFromExecutorService(fileInfo.getUrl());
                    break;
            }
        }
    }

	private static class ViewHolder {
		TextView mFileName;
		ProgressBar mProgressBar;
		Button mStartBtn;
		Button mStopBtn;
		
		/** 
		 *@param mFileName
		 *@param mProgressBar
		 *@param mStartBtn
		 *@param mStopBtn
		 */
		public ViewHolder(TextView mFileName, ProgressBar mProgressBar,
				Button mStartBtn, Button mStopBtn) {
			this.mFileName = mFileName;
			this.mProgressBar = mProgressBar;
			this.mStartBtn = mStartBtn;
			this.mStopBtn = mStopBtn;
		}
	}
	
	/**
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public Object getItem(int position)
	{
		return null;
	}

	/**
	 * @see android.widget.Adapter#getItemId(int)
	 */
	@Override
	public long getItemId(int position)
	{
		return 0;
	}
}
