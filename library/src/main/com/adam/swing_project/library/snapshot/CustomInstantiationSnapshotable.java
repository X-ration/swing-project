package com.adam.swing_project.library.snapshot;

/**
 * 自定义实例化方法的快照对象类型，可用于单例模式
 */
public interface CustomInstantiationSnapshotable extends Snapshotable {
    /**
     * 实例化方法的名称，该方法应当是静态的、无参、返回值为Snapshotable对象的方法
     * @return
     */
    String instantiationMethodName();
}
