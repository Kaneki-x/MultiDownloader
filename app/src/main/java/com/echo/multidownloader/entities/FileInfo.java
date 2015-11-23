package com.echo.multidownloader.entities;

import com.echo.multidownloader.task.MultiDownloadConnectListener;

import java.io.Serializable;

public class FileInfo implements Serializable {
	private String url;
	private String fileName;
	private long length;
	private int percent;
    private MultiDownloadConnectListener multiDownloadConnectListener;
	
	/** 
	 *@param id
	 *@param url
	 *@param fileName
	 *@param length
	 *@param finished
	 */
	public FileInfo(String url, String fileName, long length,
			int percent, MultiDownloadConnectListener multiDownloadConnectListener) {
		this.url = url;
		this.fileName = fileName;
		this.length = length;
		this.percent = percent;
        this.multiDownloadConnectListener = multiDownloadConnectListener;
	}

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public String getFileName()
	{
		return fileName;
	}

	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}

	public long getLength()
	{
		return length;
	}

	public void setLength(long length)
	{
		this.length = length;
	}

	public int getPercent()
	{
		return percent;
	}

	public void setPercent(int finished)
	{
		this.percent = finished;
	}

    public MultiDownloadConnectListener getMultiDownloadConnectListener() {
        return multiDownloadConnectListener;
    }

    public void setMultiDownloadConnectListener(MultiDownloadConnectListener multiDownloadConnectListener) {
        this.multiDownloadConnectListener = multiDownloadConnectListener;
    }

    @Override
	public String toString() {
		return "FileInfo [url=" + url + ", fileName=" + fileName
				+ ", length=" + length + ", finished=" + percent + "]";
	}
}
