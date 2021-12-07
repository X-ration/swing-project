package com.adam.swing_project.library.timer;

import java.util.List;

/**
 * 观测对象的依赖接口
 */
public interface Measurable<T>{

    T minus(T t1, T t2);
    T computeAvg(List<T> tList);

}
