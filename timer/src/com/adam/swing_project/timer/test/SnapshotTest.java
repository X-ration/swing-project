package com.adam.swing_project.timer.test;

import com.adam.swing_project.timer.assertion.Assert;
import com.adam.swing_project.timer.snapshot.SnapshotReader;
import com.adam.swing_project.timer.snapshot.SnapshotWriter;

import java.util.Random;

/**
 * 序列化测试
 */
public class SnapshotTest {

    public static void main(String[] args) {
        Random random = new Random();
        boolean result = false;
        int randomTestTime = 1000000;
        try {
            testLong(Long.MAX_VALUE);
            testLong(Long.MIN_VALUE);
            testInt(Integer.MAX_VALUE);
            testInt(Integer.MIN_VALUE);
            while(randomTestTime-->0) {
                testLong(random.nextLong());
                testInt(random.nextInt());
            }
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(result?"通过测试":"不通过测试");
    }

    private static void testLong(long lv) {
        byte[] bytes = SnapshotWriter.writer().writeLong(lv).toByteArray();
        long lvRead = SnapshotReader.reader(bytes).readLong();
        Assert.isTrue(lv == lvRead);
    }

    private static void testInt(int iv) {
        byte[] bytes = SnapshotWriter.writer().writeInt(iv).toByteArray();
        int ivRead = SnapshotReader.reader(bytes).readInt();
        Assert.isTrue(iv == ivRead);
    }

}
