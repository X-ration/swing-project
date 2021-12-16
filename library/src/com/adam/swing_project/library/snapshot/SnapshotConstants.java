package com.adam.swing_project.library.snapshot;

public class SnapshotConstants {

    /**
     * 快照文件序言
     */
    public static final String SNAPSHOT_FILE_PREFACE = "SnapshotManager of swing-timer" + System.lineSeparator();

    /**
     * 原始类型
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
     * 不定长类型
     */
    public static final byte SNAPSHOT_UNIT_TYPE_STRING = 8;
    public static final byte SNAPSHOT_UNIT_TYPE_OBJECT = 9;

    /**
     * 数组类型掩码
     */
    public static final byte SNAPSHOT_UNIT_TYPE_ARRAY_MASK = (byte) (1 << 7);
    public static final byte SNAPSHOT_UNIT_TYPE_ARRAY_STRING = SNAPSHOT_UNIT_TYPE_STRING | SNAPSHOT_UNIT_TYPE_ARRAY_MASK;

}
