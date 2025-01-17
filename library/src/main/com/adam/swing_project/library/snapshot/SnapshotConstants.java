package com.adam.swing_project.library.snapshot;

import com.adam.swing_project.library.common.Tuple;

public class SnapshotConstants {

    /**
     * 快照文件序言
     */
    public static final String SNAPSHOT_FILE_PREFACE = "SnapshotManager of swing-timer" + System.lineSeparator();

    /**
     * 原始类型, 由于数组掩码的存在，最多支持2^7=128种序列化类型
     */
    public static final byte SNAPSHOT_UNIT_TYPE_BOOLEAN = 0;
    public static final byte SNAPSHOT_UNIT_TYPE_BYTE = 1;
    public static final byte SNAPSHOT_UNIT_TYPE_SHORT = 2;
    public static final byte SNAPSHOT_UNIT_TYPE_CHAR = 3;
    public static final byte SNAPSHOT_UNIT_TYPE_INT = 4;
    public static final byte SNAPSHOT_UNIT_TYPE_LONG = 5;
    public static final byte SNAPSHOT_UNIT_TYPE_FLOAT = 6;
    public static final byte SNAPSHOT_UNIT_TYPE_DOUBLE = 7;

    /**
     * 不定长和其他类型
     */
    public static final byte SNAPSHOT_UNIT_TYPE_STRING = 8;
    public static final byte SNAPSHOT_UNIT_TYPE_OBJECT = 9;
    public static final byte SNAPSHOT_UNIT_TYPE_OBJECT_CUSTOM_INSTANTIATION = 10;
    public static final byte SNAPSHOT_UNIT_TYPE_ENUM = 11;

    /**
     * 数组类型掩码
     */
    public static final byte SNAPSHOT_UNIT_TYPE_ARRAY_MASK = (byte) (1 << 7);
    public static final byte SNAPSHOT_UNIT_TYPE_ARRAY_STRING = SNAPSHOT_UNIT_TYPE_STRING | SNAPSHOT_UNIT_TYPE_ARRAY_MASK;

    public static final Tuple<Class<?>,Byte>[] BASIC_CLASS_UNIT_TYPE_TUPLES = new Tuple[]{
            new Tuple<>(Boolean.class, SNAPSHOT_UNIT_TYPE_BOOLEAN),
            new Tuple<>(Byte.class, SNAPSHOT_UNIT_TYPE_BYTE),
            new Tuple<>(Short.class, SNAPSHOT_UNIT_TYPE_SHORT),
            new Tuple<>(Character.class, SNAPSHOT_UNIT_TYPE_CHAR),
            new Tuple<>(Integer.class, SNAPSHOT_UNIT_TYPE_INT),
            new Tuple<>(Long.class, SNAPSHOT_UNIT_TYPE_LONG),
            new Tuple<>(Float.class, SNAPSHOT_UNIT_TYPE_FLOAT),
            new Tuple<>(Double.class, SNAPSHOT_UNIT_TYPE_DOUBLE),
            new Tuple<>(String.class, SNAPSHOT_UNIT_TYPE_STRING),
            new Tuple<>(String[].class, SNAPSHOT_UNIT_TYPE_ARRAY_STRING),
            new Tuple<>(Enum.class, SNAPSHOT_UNIT_TYPE_ENUM)
    };

}
