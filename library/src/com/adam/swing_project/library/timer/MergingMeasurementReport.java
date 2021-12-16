package com.adam.swing_project.library.timer;

import com.adam.swing_project.library.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 阶段性汇总数据合并，以减少内存占用
 * @param <T>
 */
public abstract class MergingMeasurementReport<T extends Comparable<T>>
        extends MeasurementReport<T> implements MergingMeasurable<T>{

    //每1000条数据合并一次
    private int threshold = 1000;
    //已经合并的数据量
    private int mergedCount;
    private T maxDiff, minDiff, avgDiff;
    private final Logger logger = Logger.createLogger(this);

    @Override
    public void addMeasurementObject(MeasurementObject<T> measurementObject) {
        super.addMeasurementObject(measurementObject);
        if(super.measurementObjectList.size() % threshold == 0) {
            merge();
        }
    }

    private void merge() {
        logger.logDebug("Merging measurement result");
        List<T> diffList = new ArrayList<>(threshold);
        for(MeasurementObject<T> measurementObject1: super.measurementObjectList) {
            T diff = minus(measurementObject1.getActural(), measurementObject1.getExpected());
            diffList.add(diff);
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
        }
        if(avgDiff == null) {
            avgDiff = computeAvg(diffList);
        } else {
            avgDiff = mergeAvg(avgDiff, mergedCount, diffList);
        }
        mergedCount+=diffList.size();
        super.measurementObjectList.clear();
    }

    @Override
    public String getBriefDiffReport() {
        if(super.measurementObjectList.size()>0) {
            merge();
        }
        return generateBriedDiffReport(mergedCount, avgDiff, maxDiff, minDiff);
    }
}
