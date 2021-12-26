package com.adam.swing_project.timer.stat;

import com.adam.swing_project.library.snapshot.CustomInstantiationSnapshotable;
import com.adam.swing_project.library.snapshot.SnapshotManager;
import com.adam.swing_project.library.snapshot.Snapshotable;

import java.io.File;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * ActionLog全局控制器，单例模式
 */
public class ActionLogManager implements CustomInstantiationSnapshotable {

    private static final ActionLogManager instance = new ActionLogManager();
    private final Deque<ActionLog> actionLogQueue = new LinkedList<>();

    private ActionLogManager() {
        SnapshotManager.getInstance().registerSnapshotable(this);
        System.out.println("ActionLogManager new instance");
    }

    public void addActionLog(ActionLog actionLog) {
        synchronized (actionLogQueue) {
            actionLogQueue.addLast(actionLog);
        }
    }

    public static ActionLogManager getInstance() {
        return instance;
    }

    @Override
    public String instantiationMethodName() {
        return "getInstance";
    }

    @Override
    public byte[] writeToSnapshot() {
        System.out.println("ActionLogManager write snapshot");
        return new byte[0];
    }

    @Override
    public void restoreFromSnapshot(byte[] bytes) {
        System.out.println("ActionLogManager read snapshot");
    }

    public static void main(String[] args) {
        File snapshotDir = new File("C:\\Users\\Adam\\swing-timer\\snapshot-dev");
        SnapshotManager.getInstance().setSnapshotDir(snapshotDir);
        SnapshotManager.getInstance().generateSnapshot();
        List<Snapshotable> snapshotableList = SnapshotManager.getInstance().readLastSnapshot();
        snapshotableList.forEach(System.out::println);
    }
}
