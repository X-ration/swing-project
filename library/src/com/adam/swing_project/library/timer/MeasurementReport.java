package com.adam.swing_project.library.timer;

import java.util.ArrayList;
import java.util.List;

/**
 * 观测报告父类
 * 不推荐直接使用，原因是大量数据对象存在于引用链，漫长的计时过程中无法被及时回收
 */
public abstract class MeasurementReport<T extends Comparable<T>> implements Measurable<T> {

    protected final List<MeasurementObject<T>> measurementObjectList = new ArrayList<>();

    public void addMeasurementObject(MeasurementObject<T> measurementObject) {
        this.measurementObjectList.add(measurementObject);
    }

    public String getBriefDiffReport() {
        if(measurementObjectList.size()==0) {
            return "观测数据为空！";
        }
        T maxDiff = null, minDiff = null, avgDiff = null;
        List<T> diffList = new ArrayList<>();
        for(MeasurementObject<T> object: measurementObjectList) {
            T diff = minus(object.getActural(), object.getExpected());
            if(maxDiff == null) {
                maxDiff = diff;
            } else if(maxDiff.compareTo(diff) < 0) {
                maxDiff = diff;
            }
            if(minDiff == null) {
                minDiff = diff;
            } else if(minDiff.compareTo(diff) > 0) {
                minDiff = diff;
            }
            diffList.add(diff);
        }
        avgDiff = computeAvg(diffList);
        return generateBriedDiffReport(measurementObjectList.size(), avgDiff, maxDiff, minDiff);
    }

    protected String generateBriedDiffReport(int totalCount, T avgDiff, T maxDiff, T minDiff) {
        StringBuilder sb = new StringBuilder();
        sb.append("观测误差简短报告").append(this).append("共").append(totalCount).append("条数据,平均误差=")
                .append(avgDiff).append(",最大误差=").append(maxDiff).append("最小误差=").append(minDiff);
        return sb.toString();
    }

}
