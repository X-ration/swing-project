package com.adam.swing_project.timer.snapshot;

import com.adam.swing_project.timer.assertion.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 序列化数据是序列化单元的重复。
 * 序列化单元结构：type(byte)|length(int)|data(byte[])
 * 原始数据类型省略length字段
 * 数组类型type第一位为1，length代表数组长度
 * 对象类型要在type字段后指定类型表的索引
 */
public class SnapshotWriter {

    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private Class[] objectClassArray;

    public SnapshotWriter(Class[] classArray) {
        this.objectClassArray = classArray;
    }

    public static SnapshotWriter writer() {
        return new SnapshotWriter(new Class[0]);
    }

    public static SnapshotWriter writer(Class[] classArray) {
        return new SnapshotWriter(classArray);
    }

    public SnapshotWriter writeClassTable() {
        String[] classNames = Arrays.stream(objectClassArray).map(Class::getName).collect(Collectors.toList()).toArray(new String[0]);
        writeStringArray(classNames);
        return this;
    }

    public SnapshotWriter writePreface() {
        try {
            byteArrayOutputStream.write(SnapshotConstants.SNAPSHOT_FILE_PREFACE.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public SnapshotWriter writeToStream(OutputStream outputStream) {
        try {
            outputStream.write(toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public byte[] toByteArray() {
        return byteArrayOutputStream.toByteArray();
    }

    public SnapshotWriter writeInt(int iv) {
        writeByteInternal(SnapshotConstants.SNAPSHOT_UNIT_TYPE_INT);
        writeIntInternal(iv);
        return this;
    }

    public SnapshotWriter writeByte(byte bv) {
        writeByteInternal(SnapshotConstants.SNAPSHOT_UNIT_TYPE_BYTE);
        writeByteInternal(bv);
        return this;
    }

    public SnapshotWriter writeString(String sv) {
        writeByteInternal(SnapshotConstants.SNAPSHOT_UNIT_TYPE_STRING);
        byte[] svBytes = sv.getBytes(StandardCharsets.UTF_8);
        writeIntInternal(svBytes.length);
        writeByteArrayInternal(svBytes);
        return this;
    }

    public SnapshotWriter writeStringArray(String[] sArray) {
        writeByteInternal(SnapshotConstants.SNAPSHOT_UNIT_TYPE_ARRAY_STRING);
        writeIntInternal(sArray.length);
        for(String sv: sArray) {
            writeString(sv);
        }
        return this;
    }

    public SnapshotWriter writeSnapshotableObject(Snapshotable snapshotable) {
        int classIndex = findObjectClassIndex(snapshotable);
        Assert.isTrue(classIndex != -1, "该类型未注册！");
        writeByteInternal(SnapshotConstants.SNAPSHOT_UNIT_TYPE_OBJECT);
        writeIntInternal(classIndex);
        byte[] objBytes = snapshotable.writeToSnapshot();
        writeIntInternal(objBytes.length);
        writeByteArrayInternal(objBytes);
        return this;
    }

    private void writeByteInternal(byte bv) {
        byteArrayOutputStream.write(bv);
    }

    private void writeIntInternal(int iv) {
        byteArrayOutputStream.write(iv >> 24);
        byteArrayOutputStream.write(iv >> 16);
        byteArrayOutputStream.write(iv >> 8);
        byteArrayOutputStream.write(iv);
    }

    private SnapshotWriter writeByteArrayInternal(byte[] bytes) {
        try {
            byteArrayOutputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    private int findObjectClassIndex(Snapshotable snapshotable) {
        Assert.notNull(objectClassArray);
        for(int i=0;i<objectClassArray.length;i++) {
            if(objectClassArray[i] == snapshotable.getClass()) {
                return i;
            }
        }
        return -1;
    }

}
