package com.echo.multidownloader.entities;

import java.io.Serializable;

public class FileInfo implements Serializable {

    private String url;
	private String fileName;
	private long length;
	private int percent;

	/**
	 *@param url
	 *@param fileName
	 *@param length
	 *@param percent
	 */
	public FileInfo(String url, String fileName, long length,
			int percent) {
		this.url = url;
		this.fileName = fileName;
		this.length = length;
		this.percent = percent;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public long getLength()
	{
		return length;
	}

	public void setLength(long length) {
		this.length = length;
	}

	public int getPercent() {
		return percent;
	}

	public void setPercent(int finished) {
		this.percent = finished;
	}

    @Override
	public String toString() {
		return "FileInfo [url=" + url + ", fileName=" + fileName
				+ ", length=" + length + ", finished=" + percent + "]";
	}
}
