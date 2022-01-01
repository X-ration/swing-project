package com.adam.swing_project.library.snapshot;

import com.adam.swing_project.library.datetime.Date;
import com.adam.swing_project.library.datetime.Time;

import java.io.File;

/**
 * 快照信息
 */
public class Snapshot implements Comparable<Snapshot>{

    private final File snapshotFile;
    private final Date captureDate;
    private final Time captureTime;

    public Snapshot(File snapshotFile, Date captureDate, Time captureTime) {
        this.snapshotFile = snapshotFile;
        this.captureDate = captureDate;
        this.captureTime = captureTime;
    }

    public File getSnapshotFile() {
        return snapshotFile;
    }

    @Override
    public int compareTo(Snapshot o) {
        int cmp = captureDate.compareTo(o.captureDate);
        if(cmp != 0)
            return cmp;
        return captureTime.compareTo(o.captureTime);
    }
}
