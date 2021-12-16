package com.adam.swing_project.library.timer;

/**
 * 观测数据模型
 */
public class MeasurementObject<T extends Comparable<T>>{

    //预期值，实际值，差值
    private T expected, actural;

    public MeasurementObject(T expected, T actural) {
        this.expected = expected;
        this.actural = actural;
    }

    public T getExpected() {
        return expected;
    }

    public T getActural() {
        return actural;
    }

}
