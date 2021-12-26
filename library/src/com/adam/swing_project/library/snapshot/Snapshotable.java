package com.adam.swing_project.library.snapshot;


/**
 * 基础快照对象类型
 */
public interface Snapshotable{

    /**
     * 将自己转化为字节数组
     * @return
     */
    byte[] writeToSnapshot();

    /**
     * 从字节数组恢复自身的各项属性
     * @param bytes
     * @return
     */
    void restoreFromSnapshot(byte[] bytes);

}
