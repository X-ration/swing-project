package com.adam.swing_project.timer.helper;

import com.adam.swing_project.library.assertion.Assert;
import com.adam.swing_project.timer.snapshot.SnapshotReader;
import com.adam.swing_project.timer.snapshot.SnapshotWriter;
import com.adam.swing_project.timer.snapshot.Snapshotable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TimerStatistic implements Snapshotable<TimerStatistic> {

    private static TimerStatistic instance = null;
    private boolean statEnabled = true;
    private final Logger logger = Logger.createLogger(this);

    private final Map<String, DayStatistic> statisticMap = new HashMap<>();

    public class DayStatistic implements Snapshotable<DayStatistic>{
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

        @Override
        public byte[] writeToSnapshot() {
            return SnapshotWriter.writer()
                    .writeInt(totalHour).writeInt(totalMinute).writeInt(totalSecond)
                    .writeInt(naturalHour).writeInt(naturalMinute).writeInt(naturalSecond)
                    .writeInt(userStoppedHour).writeInt(userStoppedMinute).writeInt(userStoppedSecond)
                    .toByteArray();
        }

        @Override
        public DayStatistic restoreFromSnapshot(byte[] bytes) {
            SnapshotReader snapshotReader = SnapshotReader.reader(bytes);
            totalHour = snapshotReader.readInt();
            totalMinute = snapshotReader.readInt();
            totalSecond = snapshotReader.readInt();
            naturalHour = snapshotReader.readInt();
            naturalMinute = snapshotReader.readInt();
            naturalSecond = snapshotReader.readInt();
            userStoppedHour = snapshotReader.readInt();
            userStoppedMinute = snapshotReader.readInt();
            userStoppedSecond = snapshotReader.readInt();
            return this;
        }
    }

    private TimerStatistic() {
        instance = this;
    }

    public static TimerStatistic getInstance() {
        if(instance == null) {
            instance = new TimerStatistic();
        }
        return instance;
    }

    /**
     * 统计自然停止计时，外部调用
     * @param hour
     * @param minute
     */
    public void recordNaturalCounting(int year, int month, int day, int hour, int minute) {
        if(statEnabled) {
            DayStatistic dayStatistic = getOrPut(year, month, day);
            dayStatistic.recordNatural(hour, minute);
        }
    }

    /**
     * 统计用户停止计时，外部调用
     * @param hour
     * @param minute
     * @param second
     */
    public void recordUserStoppedCounting(int year, int month, int day, int hour, int minute, int second) {
        if(statEnabled) {
            DayStatistic dayStatistic = getOrPut(year, month, day);
            dayStatistic.recordUserStopped(hour, minute, second);
        }
    }

    public boolean isStatEnabled() {
        return statEnabled;
    }

    public void setStatEnabled(boolean statEnabled) {
        logger.logInfo("统计功能已[" + (statEnabled ? "启用" : "禁用") + "]");
        this.statEnabled = statEnabled;
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


    @Override
    public byte[] writeToSnapshot() {
        Set<Map.Entry<String, DayStatistic>> entrySet = statisticMap.entrySet();
        SnapshotWriter snapshotWriter = SnapshotWriter.writer(new Class[]{DayStatistic.class});
        snapshotWriter.writeClassTable();
        snapshotWriter.writeInt(entrySet.size());
        for(Map.Entry<String, DayStatistic> entry: entrySet) {
            snapshotWriter.writeString(entry.getKey());
            snapshotWriter.writeSnapshotableObject(entry.getValue());
        }
        return snapshotWriter.toByteArray();
    }

    @Override
    public TimerStatistic restoreFromSnapshot(byte[] bytes) {
        SnapshotReader snapshotReader = SnapshotReader.reader(bytes);
        snapshotReader.readClassTable();
        int size = snapshotReader.readInt();
        Assert.isTrue(size>=0);
        for(int i=0;i<size;i++) {
            String mapKey = snapshotReader.readString();
            DayStatistic dayStatistic = new DayStatistic();
            snapshotReader.readSnapshotableObject(dayStatistic);
            statisticMap.put(mapKey, dayStatistic);
        }
        return this;
    }

    private DayStatistic getOrPut(int year, int month, int day) {
        String dateString = year + "-" + (month < 10 ? "0" : "") + month + "-" + (day < 10 ? "0" : "") + day;
        DayStatistic dayStatistic = statisticMap.get(dateString);
        if(dayStatistic == null) {
            dayStatistic = new DayStatistic();
            statisticMap.put(dateString, dayStatistic);
        }
        return dayStatistic;
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

    public static void main(String[] args) {
        TimerStatistic timerStatistic = TimerStatistic.getInstance();
        timerStatistic.recordNaturalCounting(2021, 11, 15, 1, 1);
        timerStatistic.recordUserStoppedCounting(2021, 4, 3, 1,0, 1);
        System.out.println(timerStatistic);
    }

}
