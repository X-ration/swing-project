package com.adam.swing_project.timer.snapshot;

import com.adam.swing_project.timer.component.FileManager;
import com.adam.swing_project.timer.helper.Logger;
import com.adam.swing_project.timer.util.DateTimeUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
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
        this.snapshotDir = FileManager.getInstance().requireSubDir("snapshot");
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
        return new Snapshot(snapShotFile);
    }

    private Class[] collectSnapshotableClassName() {
        return snapshotableList.stream().map(Object::getClass).collect(Collectors.toList()).toArray(new Class[0]);
    }

    public List<Snapshotable> readSnapshot(Snapshot snapshot) {
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
    }
}
