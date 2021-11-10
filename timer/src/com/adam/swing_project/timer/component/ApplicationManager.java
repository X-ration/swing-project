package com.adam.swing_project.timer.component;

import com.adam.swing_project.timer.snapshot.SnapshotManager;
import com.adam.swing_project.timer.thread.ThreadManager;

/**
 * 计时程序关键过程管理器，如：初始化操作，程序退出的清理操作
 */
public class ApplicationManager {

    private static final ApplicationManager instance = new ApplicationManager();

    public static ApplicationManager getInstance() {
        return instance;
    }

    public void init() {
        ThreadManager.getInstance().initThreads();
    }

    public void close() {
        ThreadManager.getInstance().destroyThreads();
        FileManager.getInstance().cleanTempFiles();
        SnapshotManager.getInstance().generateSnapshot();
        System.exit(0);
    }
}
