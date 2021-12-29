package com.adam.swing_project.library.snapshot;

import com.adam.swing_project.library.assertion.Assert;
import com.adam.swing_project.library.logger.Logger;
import com.adam.swing_project.library.util.JdkDateTimeUtil;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 快照管理器，用于计时程序的存档和恢复
 */
public class SnapshotManager {
    private static final SnapshotManager instance = new SnapshotManager();
    private final Logger logger = Logger.createLogger(this);
    private final List<Snapshotable> snapshotableList = new ArrayList<>();
    private File snapshotDir;


    private SnapshotManager() {
    }

    public static SnapshotManager getInstance() {
        return instance;
    }

    public void setSnapshotDir(File dir) {
        Assert.notNull(dir);
        Assert.isTrue(dir.isDirectory() && dir.exists(), "非法的文件夹路径！");
        this.snapshotDir = dir;
        logger.logInfo("启用了快照目录" + snapshotDir.getPath());
    }

    /**
     * 注册快照管理对象
     * @param snapshotable
     */
    public void registerSnapshotable(Snapshotable snapshotable) {
        this.snapshotableList.add(snapshotable);
        logger.logDebug("Registered Snapshotable object '" + snapshotable + "'");
    }

    /**
     * 取消快照管理
     * @param snapshotable
     */
    public void removeSnapshotable(Snapshotable snapshotable) {
        this.snapshotableList.remove(snapshotable);
        logger.logDebug("Removed Snapshotable object '" + snapshotable + "'");
    }

    public Snapshot generateSnapshot() {
        String fileName = "snapshot-" + JdkDateTimeUtil.getInstance().getDateTimeOfTodayInFormat("yyyy-MM-dd-HH-mm-ss") + ".dat";
        File snapShotFile = new File(snapshotDir, fileName);
        logger.logInfo("开始生成快照文件" + snapShotFile.getPath());
        Class[] classList = collectSnapshotableClassName();
        SnapshotWriter snapshotWriter = SnapshotWriter.writer(classList);
        snapshotWriter.writePreface();
        snapshotWriter.writeClassTable();
        for(Snapshotable snapshotable: snapshotableList) {
            snapshotWriter.writeSnapshotableObject(snapshotable);
        }
        try(FileOutputStream fileOutputStream = new FileOutputStream(snapShotFile)) {
            snapshotWriter.writeToStream(fileOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.logInfo("成功写入了" + snapshotableList.size() + "个对象数据");
        return new Snapshot(snapShotFile);
    }

    private Class[] collectSnapshotableClassName() {
        return snapshotableList.stream().map(Object::getClass).collect(Collectors.toList()).toArray(new Class[0]);
    }

    /**
     * 扫描快照目录下的所有快照文件
     * @return
     */
    public Snapshot[] scanSnapshot() {
        File[] files = snapshotDir.listFiles();
        List<Snapshot> snapshotList = new LinkedList<>();
        for(File file: files) {
            String fileName = file.getName();
            if(fileName.startsWith("snapshot-") && fileName.endsWith(".dat")) {
                snapshotList.add(new Snapshot(file));
            }
        }
        return snapshotList.toArray(new Snapshot[0]);
    }

    /**
     * 读取最近的快照
     */
    public List<Snapshotable> readLastSnapshot() {
        Snapshot[] snapshots = scanSnapshot();
        if(snapshots.length == 0) {
            logger.logWarning("没有找到快照文件");
            return null;
        } else {
            Arrays.sort(snapshots);
            return readSnapshot(snapshots[snapshots.length-1]);
        }
    }

    //todo 考虑单例模式的情况，用构造器构造可能导致单例模式问题
    public List<Snapshotable> readSnapshot(Snapshot snapshot) {
        logger.logInfo("开始读取快照文件" + snapshot.getSnapshotFile().getPath());
        List<Snapshotable> snapshotableList = new LinkedList<>();
        File file = snapshot.getSnapshotFile();
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            SnapshotReader snapshotReader = SnapshotReader.reader(fileInputStream);
            snapshotReader.readPreface();
            snapshotReader.readClassTable();
            Snapshotable snapshotable;
            while((snapshotable = snapshotReader.readSnapshotableObject()) != null) {
                snapshotableList.add(snapshotable);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.logInfo("成功读取了" + snapshotableList.size() + "个对象数据");
        return snapshotableList;
    }

    public static void main(String[] args) {


    }
}
