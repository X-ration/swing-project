package com.adam.swing_project.timer.snapshot;

import com.adam.swing_project.timer.component.ConfigManager;
import com.adam.swing_project.timer.component.FileManager;
import com.adam.swing_project.timer.helper.Logger;
import com.adam.swing_project.timer.util.DateTimeUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
        String env = ConfigManager.getInstance().getConfig("env");
        String subDir = "snapshot";
        if(env != null) {
            subDir += ("-" + env);
        }
        this.snapshotDir = FileManager.getInstance().requireSubDir(subDir);
        logger.logInfo("启用了快照目录" + snapshotDir.getPath());
    }

    public static SnapshotManager getInstance() {
        return instance;
    }

    /**
     * 注册快照管理对象
     * @param snapshotable
     */
    public void registerSnapshotable(Snapshotable snapshotable) {
        this.snapshotableList.add(snapshotable);
    }

    public Snapshot generateSnapshot() {
        String fileName = "snapshot-" + DateTimeUtil.getInstance().getDateTimeOfTodayInFormat("yyyy-MM-dd-HH-mm-ss") + ".dat";
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

    static class Test1 implements Snapshotable<Test1> {
        private String name;
        private int age;

        @Override
        public byte[] writeToSnapshot() {
            return SnapshotWriter.writer().writeString(name).writeInt(age).toByteArray();
        }

        @Override
        public Test1 restoreFromSnapshot(byte[] bytes) {
            SnapshotReader snapshotReader = new SnapshotReader(bytes);
            this.name = snapshotReader.readString();
            this.age = snapshotReader.readInt();
            return this;
        }
    }

    public static void main(String[] args) {
        SnapshotManager snapshotManager = SnapshotManager.getInstance();
        Test1 test1 = new Test1();
        test1.name = "刘明";
        test1.age = 34;
        snapshotManager.registerSnapshotable(test1);
        Snapshot snapshot = snapshotManager.generateSnapshot();
        List<Snapshotable> objects = snapshotManager.readSnapshot(snapshot);
        System.out.println(objects);
        Snapshot[] snapshots = snapshotManager.scanSnapshot();
        Arrays.sort(snapshots);
        System.out.println(snapshots);
    }
}
