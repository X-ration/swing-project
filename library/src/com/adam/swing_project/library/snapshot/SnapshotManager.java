package com.adam.swing_project.library.snapshot;

import com.adam.swing_project.library.assertion.Assert;
import com.adam.swing_project.library.datetime.Date;
import com.adam.swing_project.library.datetime.Time;
import com.adam.swing_project.library.logger.Logger;
import com.adam.swing_project.library.util.DateTimeUtil;
import com.adam.swing_project.library.util.JdkDateTimeUtil;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 快照管理器，用于计时程序的存档和恢复
 */
public class SnapshotManager {
    private static final SnapshotManager instance = new SnapshotManager();
    private final Logger logger = Logger.createLogger(this);
    private final List<Snapshotable> snapshotableList = new ArrayList<>();
    private File snapshotDir;
    private static final Pattern SNAPSHOT_FILE_NAME_PATTERN = Pattern.compile("snapshot-(\\d{4})-(\\d{2})-(\\d{2})-(\\d{2})-(\\d{2})-(\\d{2})\\.dat");

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
        Date currentDate = DateTimeUtil.getCurrentDate();
        Time currentTime = DateTimeUtil.getCurrentTime();
        return new Snapshot(snapShotFile, currentDate, currentTime);
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
            Matcher matcher = SNAPSHOT_FILE_NAME_PATTERN.matcher(fileName);
            if(matcher.matches()) {
                try {
                    int year = Integer.parseInt(matcher.group(1))
                            , month = Integer.parseInt(matcher.group(2))
                            , day = Integer.parseInt(matcher.group(3))
                            , hour = Integer.parseInt(matcher.group(4))
                            , minute = Integer.parseInt(matcher.group(5))
                            , second = Integer.parseInt(matcher.group(6));
                    Date captureDate = new Date(year, month, day);
                    Time captureTime = new Time(hour, minute, second);
                    snapshotList.add(new Snapshot(file, captureDate, captureTime));
                } catch (NumberFormatException e) {
                    logger.logWarning("Invalid snapshot file: " + fileName);
                }
            }
        }
        return snapshotList.toArray(new Snapshot[0]);
    }

    public boolean deleteSnapshot(Snapshot snapshot) {
        return snapshot.getSnapshotFile().delete();
    }

    /**
     * 清理快照文件，按照时间顺序保留最近的reserveNum个文件
     * @param reserveNum 要保留的快照文件数，0为全部清理
     * @return
     */
    public synchronized boolean clearSnapshot(int reserveNum) {
        Assert.isTrue(reserveNum >= 0, "Invalid param");
        Snapshot[] snapshots = scanSnapshot();
        Arrays.sort(snapshots);
        if(snapshots.length <= reserveNum) {
            logger.logInfo("clearSnapshot skipped operation: total=" + snapshots.length + " reserveNum=" + reserveNum);
            return true;
        }
        int deleteNum = snapshots.length - reserveNum;
        for(int i=0;i<deleteNum;i++) {
            deleteSnapshot(snapshots[i]);
        }
        logger.logInfo("clearSnapshot finished: " + deleteNum + " of " + snapshots.length + " cleared");
        return false;
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
        String[] names = {
                "snapshot",
                "snapshot-2020-01-02-12-20-20.dat"
        };
        for(String name: names) {
            Matcher matcher = SNAPSHOT_FILE_NAME_PATTERN.matcher(name);
            System.out.println("name '" + name + "' matches? " + (matcher.matches()? "yes" : "no"));
            if(matcher.matches()) {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));
                int hour = Integer.parseInt(matcher.group(4));
                int minute = Integer.parseInt(matcher.group(5));
                int second = Integer.parseInt(matcher.group(6));
                System.out.println("Resolved group: " + year + month + day + hour + minute + second);
            }
        }

        SnapshotManager.getInstance().setSnapshotDir(new File("C:\\Users\\Adam\\swing-timer\\snapshot-dev"));
        Snapshot[] snapshots = SnapshotManager.getInstance().scanSnapshot();
//        Arrays.stream(snapshots).forEach(snapshot -> System.out.println(snapshot.getSnapshotFile().getName()));
        SnapshotManager.getInstance().clearSnapshot(0);
    }
}
