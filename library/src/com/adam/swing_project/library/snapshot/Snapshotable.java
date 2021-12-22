package com.adam.swing_project.library.snapshot;


/**
 * 快照化接口
 * @param <T>
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
