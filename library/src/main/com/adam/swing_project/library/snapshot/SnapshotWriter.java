package com.adam.swing_project.library.snapshot;

import com.adam.swing_project.library.assertion.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * 序列化数据是序列化单元的重复。
 * 序列化单元结构：type(byte)|length(int)|data(byte[])
 * 原始数据类型省略length字段
 * 数组类型type第一位为1，length代表数组长度
 * 对象类型要在type字段后指定类型表的索引
 */
public class SnapshotWriter {

    private static final Comparator<Class<?>> CLASS_COMPARATOR = Comparator.comparingInt(Object::hashCode);
    private static final Class[] SUPPORTED_BASIC_CLASSES = new Class[]{Integer.class, Long.class, Byte.class, Boolean.class,
            String.class, String[].class, Enum.class};
    static {
        Arrays.<Class<?>>sort(SUPPORTED_BASIC_CLASSES, CLASS_COMPARATOR);
    }

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

    /**
     * 支持序列化写入的基本类型(除Snapshotable类型以外可序列化的类型)
     * @see SnapshotReader#getSupportedBasicUnitType()
     */
    public static Class[] getSupportedBasicClass() {
        return SUPPORTED_BASIC_CLASSES;
    }

    /**
     * @see SnapshotReader#isSupportedBasicUnitType(byte)
     */
    public static boolean isSupportedBasicClass(Class<?> clazz) {
        return Arrays.<Class<?>>binarySearch(SUPPORTED_BASIC_CLASSES, clazz, CLASS_COMPARATOR) != -1;
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

    public SnapshotWriter writeLong(long lv)  {
        writeByteInternal(SnapshotConstants.SNAPSHOT_UNIT_TYPE_LONG);
        writeLongInternal(lv);
        return this;
    }

    public SnapshotWriter writeByte(byte bv) {
        writeByteInternal(SnapshotConstants.SNAPSHOT_UNIT_TYPE_BYTE);
        writeByteInternal(bv);
        return this;
    }

    public SnapshotWriter writeBoolean(boolean bv) {
        writeByteInternal(SnapshotConstants.SNAPSHOT_UNIT_TYPE_BOOLEAN);
        writeByteInternal(bv ? (byte)1 : (byte)0);
        return this;
    }

    public SnapshotWriter writeString(String sv) {
        writeByteInternal(SnapshotConstants.SNAPSHOT_UNIT_TYPE_STRING);
        writeStringInternal(sv);
        return this;
    }

    public SnapshotWriter writeEnum(Enum<?> ev) {
        writeByteInternal(SnapshotConstants.SNAPSHOT_UNIT_TYPE_ENUM);
        writeEnumInternal(ev);
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

    public SnapshotWriter writeCommonObject(Object object) {
        Assert.notNull(object);
        if(object instanceof Snapshotable) {
            writeSnapshotableObject((Snapshotable) object);
        } else if(isSupportedBasicClass(object.getClass())) {
            writeCommonBasicObject(object);
        } else {
            throw new SnapshotException("Invalid switch!");
        }
        return this;
    }

    public SnapshotWriter writeCommonBasicObject(Object object) {
        Assert.isTrue(object != null && isSupportedBasicClass(object.getClass()), SnapshotException.class, "Object " + object + " is not a supported basic object");
        if(object instanceof Integer) {
            return writeInt((int)object);
        }
        if(object instanceof Long) {
            return writeLong((long)object);
        }
        if(object instanceof Byte) {
            return writeByte((byte)object);
        }
        if(object instanceof Boolean) {
            return writeBoolean((boolean)object);
        }
        if(object instanceof String) {
            return writeString((String)object);
        }
        if(object instanceof String[]) {
            return writeStringArray((String[])object);
        }
        if(object instanceof Enum) {
            return writeEnum((Enum<?>)object);
        }
        throw new SnapshotException("Invalid switch!");
    }

    public SnapshotWriter writeSnapshotableObject(Snapshotable snapshotable) {
        int classIndex = findObjectClassIndex(snapshotable);
        Assert.isTrue(classIndex != -1, "该类型未注册！");
        if(snapshotable instanceof CustomInstantiationSnapshotable) {
            writeByteInternal(SnapshotConstants.SNAPSHOT_UNIT_TYPE_OBJECT_CUSTOM_INSTANTIATION);
            writeString(((CustomInstantiationSnapshotable) snapshotable).instantiationMethodName());
        } else {
            writeByteInternal(SnapshotConstants.SNAPSHOT_UNIT_TYPE_OBJECT);
        }
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

    private void writeLongInternal(long lv) {
        byteArrayOutputStream.write((int)(lv >> 56));
        byteArrayOutputStream.write((int)(lv >> 48));
        byteArrayOutputStream.write((int)(lv >> 40));
        byteArrayOutputStream.write((int)(lv >> 32));
        byteArrayOutputStream.write((int)(lv >> 24));
        byteArrayOutputStream.write((int)(lv >> 16));
        byteArrayOutputStream.write((int)(lv >> 8));
        byteArrayOutputStream.write((int)(lv));
    }

    private void writeStringInternal(String sv) {
        byte[] svBytes = sv.getBytes(StandardCharsets.UTF_8);
        writeIntInternal(svBytes.length);
        writeByteArrayInternal(svBytes);
    }

    private void writeEnumInternal(Enum<?> ev) {
        writeStringInternal(ev.getClass().getName());
        writeIntInternal(ev.ordinal());
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

    public static void main(String[] args) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        long lv = 1636710987251L;
        byteArrayOutputStream.write((int)(lv >> 56) & 0xff);
        byteArrayOutputStream.write((int)(lv >> 48) & 0xff);
        byteArrayOutputStream.write((int)(lv >> 40) & 0xff);
        byteArrayOutputStream.write((int)(lv >> 32) & 0xff);
        byteArrayOutputStream.write((int)(lv >> 24) & 0xff);
        byteArrayOutputStream.write((int)(lv >> 16) & 0xff);
        byteArrayOutputStream.write((int)(lv >> 8) & 0xff);
        byteArrayOutputStream.write((int)(lv) & 0xff);
        byte[] buffer = byteArrayOutputStream.toByteArray();
        long lvRead = (((long)buffer[0]&0xff)<< 56) | (((long)buffer[1]&0xff)<< 48) |(((long)buffer[2]&0xff) << 40) |(((long)buffer[3]&0xff) << 32) |
                (((long)buffer[4]&0xff) << 24) |(((long)buffer[5]&0xff) << 16) |(((long)buffer[6]&0xff) << 8) |(((long)buffer[7]&0xff) );
        System.out.println(lv + "===" + lvRead);
    }

}
