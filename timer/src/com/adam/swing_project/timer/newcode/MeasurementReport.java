package com.adam.swing_project.timer.newcode;

import com.adam.swing_project.timer.assertion.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * 观测报告
 */
public abstract class MeasurementReport<T extends Comparable<T>> implements Measurable<T> {

    private final List<MeasurementObject<T>> measurementObjectList = new ArrayList<>();

    public void addMeasurementObject(MeasurementObject<T> measurementObject) {
        this.measurementObjectList.add(measurementObject);
    }

    public String getBriefDiffReport() {
        if(measurementObjectList.size()==0) {
            return "观测数据为空！";
        }
        StringBuilder sb = new StringBuilder();
        T maxDiff = null, minDiff = null, avgDiff = null;
        List<T> diffList = new ArrayList<>();
        for(MeasurementObject<T> object: measurementObjectList) {
            T diff = computeDiff(object.getActural(), object.getExpected());
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
        sb.append("观测误差简短报告").append(this).append("共" + measurementObjectList.size() + "条数据,平均误差=" + avgDiff +",最大误差=" + maxDiff + "最小误差=" + minDiff);
        return sb.toString();
    }

}
