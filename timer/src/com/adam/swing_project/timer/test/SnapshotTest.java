package com.adam.swing_project.timer.test;

import com.adam.swing_project.timer.assertion.Assert;
import com.adam.swing_project.timer.snapshot.SnapshotReader;
import com.adam.swing_project.timer.snapshot.SnapshotWriter;

public class SnapshotTest {

    public static void main(String[] args) {
        boolean result = false;
        try {
            long lv = 1636710987251L;
            byte[] bytes = SnapshotWriter.writer().writeLong(lv).toByteArray();
            long lvRead = SnapshotReader.reader(bytes).readLong();
            Assert.isTrue(lv == lvRead);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(result?"通过测试":"不通过测试");
    }

}
