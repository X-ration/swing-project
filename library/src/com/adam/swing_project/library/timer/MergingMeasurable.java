package com.adam.swing_project.library.timer;

import java.util.List;

public interface MergingMeasurable<T> extends Measurable<T> {

    T mergeAvg(T avg, int avgCount, List<T> tList);

}
