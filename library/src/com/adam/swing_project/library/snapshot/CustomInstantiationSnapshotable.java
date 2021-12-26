package com.adam.swing_project.library.snapshot;

/**
 * 自定义实例化方法的快照对象类型
 */
public interface CustomInstantiationSnapshotable extends Snapshotable {
    /**
     * 实例化方法的名称，该方法必须是静态方法
     * @return
     */
    String instantiationMethodName();
}
