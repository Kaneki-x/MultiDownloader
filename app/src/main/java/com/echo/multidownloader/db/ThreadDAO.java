package com.echo.multidownloader.db;

import com.echo.multidownloader.entities.ThreadInfo;
import java.util.List;

public interface ThreadDAO {

	void insertThread(ThreadInfo threadInfo);

	void deleteThread(String url);

	void updateThread(String url, int thread_id, long finished);

	List<ThreadInfo> getThreads(String url);

	boolean isExists(String url, int thread_id);
}
