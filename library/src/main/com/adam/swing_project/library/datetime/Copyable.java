package com.adam.swing_project.library.datetime;

/**
 * 可复制类型的接口
 */
public interface Copyable<T> {

    void copyFrom(T another);

}
