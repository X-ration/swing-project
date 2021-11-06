package com.adam.swing_project.timer.helper;

import com.adam.swing_project.timer.assertion.Assert;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TimerStatistic {

    private static final TimerStatistic instance = new TimerStatistic();

    private final Map<String, DayStatistic> statisticMap = new HashMap<>();

    public class DayStatistic {
        private int totalHour;
        private int totalMinute;
        private int totalSecond;

        private int naturalHour;
        private int naturalMinute;
        private int naturalSecond;

        private int userStoppedHour;
        private int userStoppedMinute;
        private int userStoppedSecond;

        private void recordNatural(int hour, int minute) {
            naturalHour += hour;
            naturalMinute += minute;
            totalHour += hour;
            totalMinute += minute;
            recompute();
        }
        private void recordUserStopped(int hour, int minute, int second) {
            userStoppedHour += hour;
            userStoppedMinute += minute;
            userStoppedSecond += second;
            totalHour += hour;
            totalMinute += minute;
            totalSecond += second;
            recompute();
        }
        private void recompute() {
            if(totalSecond >= 60) {
                totalMinute += totalSecond / 60;
                totalSecond = totalSecond % 60;
            }
            if(totalMinute >= 60) {
                totalHour += totalMinute / 60;
                totalMinute = totalMinute % 60;
            }

            if(naturalSecond >= 60) {
                naturalSecond += naturalSecond / 60;
                naturalSecond = naturalSecond % 60;
            }
            if(naturalMinute >= 60) {
                naturalHour += naturalMinute / 60;
                naturalMinute = naturalMinute % 60;
            }

            if(userStoppedSecond >= 60) {
                userStoppedMinute += userStoppedSecond / 60;
                userStoppedSecond = userStoppedSecond % 60;
            }
            if(userStoppedMinute >= 60) {
                userStoppedHour += userStoppedMinute / 60;
                userStoppedMinute = userStoppedMinute % 60;
            }
        }

        public String getTotalStatistic() {
            return totalHour + ":" + totalMinute + ":" + totalSecond;
        }

        public String getNaturalStatistic() {
            return naturalHour + ":" + naturalMinute + ":" + naturalSecond;
        }

        public String getUserStoppedStatistic() {
            return userStoppedHour + ":" + userStoppedMinute + ":" + userStoppedSecond;
        }
    }

    public static TimerStatistic getInstance() {
        return instance;
    }

    /**
     * 统计自然停止计时，外部调用
     * @param hour
     * @param minute
     */
    public void recordNaturalCounting(int hour, int minute) {
        DayStatistic dayStatistic = getOrPut(0);
        dayStatistic.recordNatural(hour, minute);
    }

    /**
     * 统计用户停止计时，外部调用
     * @param hour
     * @param minute
     * @param second
     */
    public void recordUserStoppedCounting(int hour, int minute, int second) {
        DayStatistic dayStatistic = getOrPut(0);
        dayStatistic.recordUserStopped(hour, minute, second);
    }

    /**
     * 获取最近<requireDays>天的统计数据，外部调用
     * @param requireDays 包括今天在内 需要的天数
     * @return 返回值为Object[]，其格式按照(String)日期、(DayStatistic)统计数据重复，数组长度为2*requireDays；按照日期降序排列
     */
    public Object[] getDayStatistic(int requireDays) {
        Assert.isTrue(requireDays > 0, "requireDays > 0!");
        Object[] result = new Object[2*requireDays];
        for(int i=0;i<requireDays;i++) {
            String date = getDateString(i);
            DayStatistic dayStatistic = statisticMap.get(date);
            result[2*i] = date;
            result[2*i+1] = dayStatistic;
        }
        return result;
    }

    private DayStatistic getOrPut(int daysDiff) {
        String date = getDateString(daysDiff);
        DayStatistic dayStatistic = statisticMap.get(date);
        if(dayStatistic == null) {
            dayStatistic = new DayStatistic();
            statisticMap.put(date, dayStatistic);
        }
        return dayStatistic;
    }

    private String getDateString(int daysDiff) {
        Date date = new Date();
        if(daysDiff != 0) {
            date.setTime(date.getTime() - (long) daysDiff * 24 * 3600 * 1000);
        }
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return simpleDateFormat.format(date);
    }

}
