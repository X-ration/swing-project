package com.adam.swing_project.library.snapshot;

import com.adam.swing_project.library.assertion.Assert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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

    private void requireTypeInternal(byte typeRead, byte... types) {
        int index = -1;
        for(int i = 0;i<types.length;i++) {
            if(types[i] == typeRead) {
                index = i;
            }
        }
        Assert.isTrue(index != -1, "错误的序列化类型，预期=[" + Arrays.toString(types) + "],实际=" + typeRead);
    }

    @Deprecated
    private void requireType(byte type) throws ReadEndException{
        readAFewBytes(1);
        Assert.isTrue(buffer[0] == type, "错误的序列化类型，预期=" + type + ",实际=" + buffer[0]);
    }

    public String readPreface() {
        byte[] prefaceArray = SnapshotConstants.SNAPSHOT_FILE_PREFACE.getBytes(StandardCharsets.UTF_8);
        try {
            readAFewBytes(prefaceArray.length);
        } catch (ReadEndException e) {
            throw new SnapshotException(e);
        }
        for(int i=0;i<prefaceArray.length;i++) {
            Assert.isTrue(buffer[i] == prefaceArray[i], SnapshotException.class, "读取的序言非法！");
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
            throw new SnapshotException(e);
        }
        this.objectClassArray = classArray;
        return classArray;
    }

    public int readInt() {
        try {
            requireType(SnapshotConstants.SNAPSHOT_UNIT_TYPE_INT);
            return readIntInternal();
        } catch (ReadEndException e) {
            throw new SnapshotException(e);
        }
    }

    public long readLong() {
        try {
            requireType(SnapshotConstants.SNAPSHOT_UNIT_TYPE_LONG);
            return readLongInternal();
        } catch (ReadEndException e) {
            throw new SnapshotException(e);
        }
    }

    public byte readByte() {
        try {
            requireType(SnapshotConstants.SNAPSHOT_UNIT_TYPE_BYTE);
            return readByteInternal();
        } catch (ReadEndException e) {
            throw new SnapshotException(e);
        }
    }

    public String readString() {
        try {
            requireType(SnapshotConstants.SNAPSHOT_UNIT_TYPE_STRING);
            int length = readIntInternal();
            Assert.isTrue(length > 0, "非法的长度值");
            byte[] strBytes;
            if (length <= BUFFER_SIZE) {
                readAFewBytes(length);
                strBytes = new byte[length];
                System.arraycopy(buffer, 0, strBytes, 0, length);
            } else {
                strBytes = readMoreBytes(length);
            }
            return new String(strBytes, StandardCharsets.UTF_8);
        } catch (ReadEndException e) {
            throw new SnapshotException(e);
        }
    }

    public String[] readStringArray() {
        try {
            requireType(SnapshotConstants.SNAPSHOT_UNIT_TYPE_ARRAY_STRING);
            int arrayLength = readIntInternal();
            String[] sArray = new String[arrayLength];
            for(int i=0;i<arrayLength;i++) {
                sArray[i] = readString();
            }
            return sArray;
        } catch (ReadEndException e) {
            e.printStackTrace();
            throw new SnapshotException(e);
        }
    }

//    public Snapshotable readSnapshotableObject() {
//        return readSnapshotableObject(null);
//    }

    /**
     * 适应使用内部类的情况，由根对象生成内部类实例后传递给方法参数
     * @return
     */
    //todo 异常处理优化
    public Snapshotable readSnapshotableObject() {
//    public Snapshotable readSnapshotableObject(Snapshotable object) {
        byte typeRead;
        String instantiationMethodName = null;
        try {
            typeRead = readByteInternal();
            requireTypeInternal(typeRead, SnapshotConstants.SNAPSHOT_UNIT_TYPE_OBJECT, SnapshotConstants.SNAPSHOT_UNIT_TYPE_OBJECT_CUSTOM_INSTANTIATION);
        } catch (ReadEndException e) {
            return null;
        }

        try {
            if (typeRead == SnapshotConstants.SNAPSHOT_UNIT_TYPE_OBJECT_CUSTOM_INSTANTIATION) {
                instantiationMethodName = readString();
            }

            int classIndex = readIntInternal();
            Assert.isTrue(classIndex >= 0 && classIndex < objectClassArray.length, "类型索引超出界限！");
            Assert.isTrue(Snapshotable.class.isAssignableFrom(objectClassArray[classIndex]));
            Class<Snapshotable> objectClass = objectClassArray[classIndex];

            Snapshotable object;
            if (typeRead == SnapshotConstants.SNAPSHOT_UNIT_TYPE_OBJECT_CUSTOM_INSTANTIATION) {
                Method method = objectClass.getDeclaredMethod(instantiationMethodName, null);
                object = (Snapshotable) method.invoke(null, null);
            } else {
                Constructor<Snapshotable> constructor = objectClass.getDeclaredConstructor(null);
                constructor.setAccessible(true);
                object = constructor.newInstance(null);
            }

            int objectLength = readIntInternal();
            byte[] objectData;
            if (objectLength == 0) {
                objectData = new byte[0];
            }
            else if(objectLength > 0 && objectLength <= BUFFER_SIZE) {
                readAFewBytes(objectLength);
                objectData = new byte[objectLength];
                System.arraycopy(buffer, 0, objectData, 0, objectLength);
            } else if(objectLength > BUFFER_SIZE) {
                objectData = readMoreBytes(objectLength);
            } else {
                throw new SnapshotException("Negative object length: " + objectLength);
            }

            object.restoreFromSnapshot(objectData);
            return object;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ReadEndException e) {
            throw new SnapshotException(e);
        }
    }


    private int readIntInternal() throws ReadEndException {
        readAFewBytes(4);
        return (((int)buffer[0] & 0xFF) << 24) | (((int)buffer[1] & 0xFF) << 16) |
                (((int)buffer[2] & 0xFF) << 8) | (((int)buffer[3] & 0xFF));
    }

    private long readLongInternal() throws ReadEndException {
        readAFewBytes(8);
        return (((long)buffer[0] & 0xFF) << 56) | (((long)buffer[1] & 0xFF) << 48) |
                (((long)buffer[2] & 0xFF) << 40) | (((long)buffer[3] & 0xFF) << 32) |
                (((long)buffer[4] & 0xFF) << 24) | (((long)buffer[5] & 0xFF) << 16) |
                (((long)buffer[6] & 0xFF) << 8) | (((long)buffer[7] & 0xFF));
    }

    private byte readByteInternal() throws ReadEndException {
        readAFewBytes(1);
        return buffer[0];
    }

    /**
     * 读取少于缓冲数组大小的字节数个字节
     * @param nBytes
     * @return 返回缓冲数组从0开始的截止位置
     */
    private int readAFewBytes(int nBytes) throws ReadEndException{
        Assert.notNull(inputStream);
        Assert.isTrue(nBytes >= 0, SnapshotException.class, "读取字节数非法！");
        Assert.isTrue(nBytes <= BUFFER_SIZE, SnapshotException.class, "读取字节数超过上限！");
        if(nBytes == 0) return 0;
        try {
            int nBytesRead = inputStream.read(buffer, 0, nBytes);
            if(nBytesRead == -1)
                throw new ReadEndException();
            Assert.isTrue(nBytes == nBytesRead, SnapshotException.class, "读取的字节数不符，预期=" + nBytes + ",实际=" + nBytesRead);
            return nBytesRead;
        } catch (IOException e) {
            throw new SnapshotException(e);
        }
    }

    //fix read snapshot error
    private byte[] readMoreBytes(int nBytes) throws ReadEndException{
        Assert.notNull(inputStream);
        Assert.isTrue(nBytes > BUFFER_SIZE, SnapshotException.class, "读取字节数过少！");
        byte[] byteArray = new byte[nBytes];
        int totalBytesRead = 0, bytesRead = 0;
        try {
            int remainingBytes = nBytes;
            while (remainingBytes != 0 && (bytesRead = inputStream.read(buffer, 0, Math.min(remainingBytes, BUFFER_SIZE))) != -1){
                //System.out.println("nBytes="+nBytes+",bytesRead="+bytesRead+",totalBytesRead="+totalBytesRead);
                System.arraycopy(buffer, 0, byteArray, totalBytesRead, bytesRead);
                totalBytesRead += bytesRead;
                remainingBytes -= bytesRead;
            }
        } catch (IOException e) {
            throw new SnapshotException(e);
        }
        if(totalBytesRead == 0)
            throw new ReadEndException();
        Assert.isTrue(totalBytesRead == nBytes, SnapshotException.class, "读取的字节数不符，预期=" + nBytes + ",实际=" + totalBytesRead);
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
