package com.adam.swing_project.timer.component;

import com.adam.swing_project.library.logger.Logger;
import com.adam.swing_project.library.logger.LoggerFactory;
import com.adam.swing_project.library.runtime.ManagedShutdownHook;
import com.adam.swing_project.library.runtime.PriorityShutdownHook;
import com.adam.swing_project.library.snapshot.SnapshotManager;
import com.adam.swing_project.library.snapshot.Snapshotable;
import com.adam.swing_project.library.timer.Timer;
import com.adam.swing_project.library.util.ApplicationArgumentResolver;
import com.adam.swing_project.timer.frontend.TimerPanel;
import com.adam.swing_project.timer.thread.ThreadManager;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * 计时程序关键过程管理器，如：初始化操作，程序退出的清理操作
 */
public class ApplicationManager {

    private static final ApplicationManager instance = new ApplicationManager();
    private final Logger logger = LoggerFactory.getLogger(this);
    private final List<Object> programGlobalObjectList = new LinkedList<>();
    private ApplicationArgumentResolver argumentResolver;

    public static ApplicationManager getInstance() {
        return instance;
    }

    public void init() {
        ThreadManager.getInstance().initThreads();
        argumentResolver = getProgramGlobalObject(ApplicationArgumentResolver.class);
        updateSnapshotDir();
        //只保留最近的3个快照文件
        SnapshotManager.getInstance().clearSnapshot(3);

        List<Snapshotable> snapshotableList = SnapshotManager.getInstance().readLastSnapshot();
        PriorityShutdownHook shutdownHook = new PriorityShutdownHook() {
            @Override
            public void run() {
                close();
            }
        };
        shutdownHook.setName("TimerProgram shutdown hook");
        ManagedShutdownHook.getInstance().registerShutdownHook(shutdownHook);
        logger.logDebug("Registered shutdown hook");
        if(snapshotableList != null) {
            for (Snapshotable snapshotable : snapshotableList) {
                if (snapshotable instanceof Timer) {
                    TimerPanel timerPanel = getProgramGlobalObject(TimerPanel.class);
                    timerPanel.addSingleTimerPanel((Timer) snapshotable);
                }
            }
        }
    }

    public void updateSnapshotDir() {
        SnapshotManager.getInstance().setSnapshotDir(FileManager.getInstance().getAppRootDir());
    }

    public void close() {
        ThreadManager.getInstance().destroyThreads();
        FileManager.getInstance().cleanTempFiles();
        SnapshotManager.getInstance().generateSnapshot();
    }

    public void registerProgramGlobalObject(Object object) {
        programGlobalObjectList.add(object);
    }

    public <T> T getProgramGlobalObject(Class<T> tClass) {
        for(Object object: programGlobalObjectList) {
            if(tClass.isInstance(object)) {
                return (T) object;
            }
        }
        return null;
    }
}
