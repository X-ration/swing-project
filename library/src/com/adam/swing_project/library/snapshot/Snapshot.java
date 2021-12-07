package com.adam.swing_project.library.snapshot;

import com.adam.swing_project.library.util.JdkDateTimeUtil;

import java.io.File;
import java.util.Date;

/**
 * 快照信息
 */
public class Snapshot implements Comparable<Snapshot>{

    private final File snapshotFile;
    private final Date captureDate;

    public static void main(String[] args) {
        File file = new File("snapshot-2021-11-09 12:00:00.dat");
        Snapshot snapshot = new Snapshot(file);
        System.out.println(snapshot);
    }

    public Snapshot(File snapshotFile) {
        this.snapshotFile = snapshotFile;
//        this.captureDate = Calen
        String fileName = snapshotFile.getName();
        int fi = fileName.indexOf("-") + 1, li = fileName.lastIndexOf(".dat");
        String dateString = fileName.substring(fi, li);
        this.captureDate = JdkDateTimeUtil.getInstance().getDateInFormat(dateString, "yyyy-MM-dd-HH-mm-ss");
    }

    public File getSnapshotFile() {
        return snapshotFile;
    }

    public Date getCaptureDate() {
        return captureDate;
    }

    @Override
    public int compareTo(Snapshot o) {
        return captureDate.compareTo(o.captureDate);
    }
}
