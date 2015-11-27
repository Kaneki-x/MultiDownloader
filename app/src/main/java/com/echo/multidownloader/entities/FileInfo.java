package com.echo.multidownloader.entities;

import java.io.Serializable;

public class FileInfo implements Serializable {

    private String url;
	private String fileName;
	private long length;
	private int percent;

	/**
     * @param url
     * @param fileName
     */
	public FileInfo(String url, String fileName) {
		this.url = url;
		this.fileName = fileName;
		this.length = (long) 0;
		this.percent = 0;
	}

	public String getUrl() {
		return url;
	}

	public String getFileName() {
		return fileName;
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
