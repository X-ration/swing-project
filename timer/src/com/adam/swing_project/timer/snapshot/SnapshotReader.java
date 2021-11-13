package com.adam.swing_project.timer.snapshot;

import com.adam.swing_project.timer.assertion.Assert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;

public class SnapshotReader {

    private Class[] objectClassArray;
    private InputStream inputStream;
    private static final int BUFFER_SIZE = 1024;
    private byte[] buffer = new byte[BUFFER_SIZE];

    /**
     * 读取到结束时的异常类型
     */
    public class ReadEndException extends Exception {
    }

    /**
     * 读取内存数据，适合Snapshotable对象使用
     * @param data
     */
    public SnapshotReader(byte[] data) {
        this.inputStream = new ByteArrayInputStream(data);
    }

    /**
     * 读取输入流数据，读取完整快照文件
     * @param inputStream
     */
    public SnapshotReader(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public static SnapshotReader reader(byte[] data) {
        return new SnapshotReader(data);
    }

    public static SnapshotReader reader(InputStream inputStream) {
        return new SnapshotReader(inputStream);
    }

    /**
     * 在Snapshotable对象内部使用
     * @param data
     * @param classArray
     */
    public SnapshotReader(byte[] data, Class[] classArray) {
        this(data);
        this.objectClassArray = classArray;
    }

    private void requireType(byte type) throws ReadEndException{
        readAFewBytes(1);
        Assert.isTrue(buffer[0] == type, "错误的序列化类型，预期=" + type + ",实际=" + buffer[0]);
    }

    public String readPreface() {
        byte[] prefaceArray = SnapshotConstants.SNAPSHOT_FILE_PREFACE.getBytes(StandardCharsets.UTF_8);
        try {
            readAFewBytes(prefaceArray.length);
        } catch (ReadEndException e) {
            e.printStackTrace();
            throw new SnapshotException(e);
        }
        for(int i=0;i<prefaceArray.length;i++) {
            Assert.isTrue(buffer[i] == prefaceArray[i], "读取的序言非法！");
        }
        return SnapshotConstants.SNAPSHOT_FILE_PREFACE;
    }

    public Class[] readClassTable() {
        String[] classNameArray = readStringArray();
        Class[] classArray = new Class[classNameArray.length];
        try {
            for(int i=0;i< classArray.length;i++) {
                classArray[i] = Class.forName(classNameArray[i]);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        this.objectClassArray = classArray;
        return classArray;
    }

    public int readInt() {
        try {
            requireType(SnapshotConstants.SNAPSHOT_UNIT_TYPE_INT);
        } catch (ReadEndException e) {
            e.printStackTrace();
            throw new SnapshotException(e);
        }
        return readIntInternal();
    }

    public long readLong() {
        try {
            requireType(SnapshotConstants.SNAPSHOT_UNIT_TYPE_LONG);
        } catch (ReadEndException e) {
            e.printStackTrace();
            throw new SnapshotException(e);
        }
        return readLongInternal();
    }

    public byte readByte() {
        try {
            requireType(SnapshotConstants.SNAPSHOT_UNIT_TYPE_BYTE);
        } catch (ReadEndException e) {
            e.printStackTrace();
            throw new SnapshotException(e);
        }
        return readByteInternal();
    }

    public String readString() {
        try {
            requireType(SnapshotConstants.SNAPSHOT_UNIT_TYPE_STRING);
        } catch (ReadEndException e) {
            e.printStackTrace();
            throw new SnapshotException(e);
        }
        int length = readIntInternal();
        Assert.isTrue(length > 0, "非法的长度值");
        byte[] strBytes;
        try {
            if (length <= BUFFER_SIZE) {
                readAFewBytes(length);
                strBytes = new byte[length];
                System.arraycopy(buffer, 0, strBytes, 0, length);
            } else {
                strBytes = readMoreBytes(length);
            }
        } catch (ReadEndException e) {
            e.printStackTrace();
            throw new SnapshotException(e);
        }
        return new String(strBytes, StandardCharsets.UTF_8);
    }

    public String[] readStringArray() {
        try {
            requireType(SnapshotConstants.SNAPSHOT_UNIT_TYPE_ARRAY_STRING);
        } catch (ReadEndException e) {
            e.printStackTrace();
            throw new SnapshotException(e);
        }
        int arrayLength = readIntInternal();
        String[] sArray = new String[arrayLength];
        for(int i=0;i<arrayLength;i++) {
            sArray[i] = readString();
        }
        return sArray;
    }

    public Snapshotable readSnapshotableObject() {
        try {
            requireType(SnapshotConstants.SNAPSHOT_UNIT_TYPE_OBJECT);
        } catch (ReadEndException e) {
            return null;
        }
        int classIndex = readIntInternal();
        Assert.isTrue(classIndex >= 0 && classIndex < objectClassArray.length, "类型索引超出界限！");
        Class objectClass = objectClassArray[classIndex];
        Snapshotable object ;
        try {
            object = (Snapshotable) objectClass.getDeclaredConstructor(null).newInstance(null);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
        int objectLength = readIntInternal();
        byte[] objectData;
        try {
            if (objectLength <= BUFFER_SIZE) {
                readAFewBytes(objectLength);
                objectData = new byte[objectLength];
                System.arraycopy(buffer, 0, objectData, 0, objectLength);
            } else {
                objectData = readMoreBytes(objectLength);
            }
        } catch (ReadEndException e) {
           return null;
        }
        object.restoreFromSnapshot(objectData);
        return object;
    }


    private int readIntInternal() {
        try {
            readAFewBytes(4);
        } catch (ReadEndException e) {
            e.printStackTrace();
            throw new SnapshotException(e);
        }
        return (((int)buffer[0] & 0xFF) << 24) | (((int)buffer[1] & 0xFF) << 16) |
                (((int)buffer[2] & 0xFF) << 8) | (((int)buffer[3] & 0xFF));
    }

    private long readLongInternal() {
        try {
            readAFewBytes(8);
        } catch (ReadEndException e) {
            e.printStackTrace();
            throw new SnapshotException(e);
        }
        return (((long)buffer[0] & 0xFF) << 56) | (((long)buffer[1] & 0xFF) << 48) |
                (((long)buffer[2] & 0xFF) << 40) | (((long)buffer[3] & 0xFF) << 32) |
                (((long)buffer[4] & 0xFF) << 24) | (((long)buffer[5] & 0xFF) << 16) |
                (((long)buffer[6] & 0xFF) << 8) | (((long)buffer[7] & 0xFF));
    }

    private byte readByteInternal() {
        try {
            readAFewBytes(1);
        } catch (ReadEndException e) {
            e.printStackTrace();
            throw new SnapshotException(e);
        }
        return buffer[0];
    }

    /**
     * 读取少于缓冲数组大小的字节数个字节
     * @param nBytes
     * @return 返回缓冲数组从0开始的截止位置
     */
    private int readAFewBytes(int nBytes) throws ReadEndException{
        Assert.notNull(inputStream);
        Assert.isTrue(nBytes <= BUFFER_SIZE, "读取字节数超过上限！");
        try {
            int nBytesRead = inputStream.read(buffer, 0, nBytes);
            if(nBytesRead == -1)
                throw new ReadEndException();
            Assert.isTrue(nBytes == nBytesRead, "读取的字节数不符，预期=" + nBytes + ",实际=" + nBytesRead);
            return nBytesRead;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private byte[] readMoreBytes(int nBytes) throws ReadEndException{
        Assert.notNull(inputStream);
        Assert.isTrue(nBytes > BUFFER_SIZE, "读取字节数过少！");
        byte[] byteArray = new byte[nBytes];
        int totalBytesRead = 0, bytesRead = 0;
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1){
                System.arraycopy(buffer, 0, byteArray, totalBytesRead, bytesRead);
                totalBytesRead += bytesRead;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(totalBytesRead == 0)
            throw new ReadEndException();
        Assert.isTrue(totalBytesRead == nBytes, "读取的字节数不符，预期=" + nBytes + ",实际=" + totalBytesRead);
        return byteArray;
    }

    public static void main(String[] args) {
        long lv = 0x3;
        long nlv = lv & 0xFF << 8;
        long nlv2 = (lv & 0xFF) << 8;
        System.out.println(nlv2);
        byte bv = -50;
        long nlv3 = (long) bv & 0xFF;
        long nlv4 = ((long) bv) & 0xFF;
        System.out.println(nlv3 + "," + nlv4);
    }

}
