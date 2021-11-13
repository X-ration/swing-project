package com.adam.swing_project.timer.component;

import com.adam.swing_project.timer.core.Timer;
import com.adam.swing_project.timer.frontend.TimerPanel;
import com.adam.swing_project.timer.helper.Logger;
import com.adam.swing_project.timer.snapshot.SnapshotManager;
import com.adam.swing_project.timer.snapshot.Snapshotable;
import com.adam.swing_project.timer.thread.ThreadManager;

import java.util.LinkedList;
import java.util.List;

/**
 * 计时程序关键过程管理器，如：初始化操作，程序退出的清理操作
 */
public class ApplicationManager {

    private static final ApplicationManager instance = new ApplicationManager();
    private final Logger logger = Logger.createLogger(this);
    private final List<Object> programGlobalObjectList = new LinkedList<>();

    public static ApplicationManager getInstance() {
        return instance;
    }

    public void init() {
        ThreadManager.getInstance().initThreads();
        List<Snapshotable> snapshotableList = SnapshotManager.getInstance().readLastSnapshot();
        if(snapshotableList == null)
            return;
        for(Snapshotable snapshotable: snapshotableList) {
            if(snapshotable instanceof Timer) {
                TimerPanel timerPanel = getProgramGlobalObject(TimerPanel.class);
                timerPanel.addSingleTimerPanel((Timer) snapshotable);
            }
        }
    }

    public void close() {
        ThreadManager.getInstance().destroyThreads();
        FileManager.getInstance().cleanTempFiles();
        SnapshotManager.getInstance().generateSnapshot();
        System.exit(0);
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
