package com.adam.swing_project.timer.stat;

import com.adam.swing_project.library.assertion.Assert;
import com.adam.swing_project.library.datetime.Date;
import com.adam.swing_project.library.datetime.Time;
import com.adam.swing_project.library.snapshot.CustomInstantiationSnapshotable;
import com.adam.swing_project.library.snapshot.SnapshotManager;
import com.adam.swing_project.library.snapshot.SnapshotReader;
import com.adam.swing_project.library.snapshot.SnapshotWriter;
import com.adam.swing_project.timer.component.OptionManager;
import com.adam.swing_project.timer.option.OptionConstants;
import com.adam.swing_project.timer.timer.CountingRound;

import java.util.*;

public class TimerStatistic implements CustomInstantiationSnapshotable {

    private static final TimerStatistic instance = new TimerStatistic();
    private final Map<Date, TimerDayStatistic> dayStatisticMap = new HashMap<>();

    static {
        SnapshotManager.getInstance().registerSnapshotable(instance);
    }

    /**
     * 获取所有有数据的日期
     * @return
     */
    public Date[] availableDates() {
        List<Date> result = new LinkedList<>(dayStatisticMap.keySet());
        return result.toArray(new Date[0]);
    }

    /**
     * 获取特定日期的统计数据
     * @param date
     * @return
     */
    public TimerDayStatistic getDayStatistic(Date date) {
        return dayStatisticMap.get(date);
    }

    /**
     * 手动修正统计时长
     * @param date
     * @param totalResetTime
     * @param totalCountedTime
     */
    public void reviseDayStatistic(Date date, Time totalResetTime, Time totalCountedTime) {
        TimerDayStatistic dayStatistic = getDayStatisticOrNew(date);
        dayStatistic.revise(totalResetTime, totalCountedTime);
    }

    private TimerDayStatistic getDayStatisticOrNew(Date date) {
        TimerDayStatistic dayStatistic = dayStatisticMap.get(date);
        if(dayStatistic == null) {
            dayStatistic = new TimerDayStatistic(date,
                    new Time(0,0,0), new Time(0,0,0));
            dayStatisticMap.put(date, dayStatistic);
        }
        return dayStatistic;
    }

    public void reportCountingRoundStart(CountingRound countingRound) {
        OptionConstants.StatDefaultMethod statDefaultMethod = OptionManager.getInstance().getOptionValue(OptionConstants.OPTION_GENERAL_STAT_DEFAULT, OptionConstants.StatDefaultMethod.class);
        if(statDefaultMethod != OptionConstants.StatDefaultMethod.DISABLED) {
            Date date = countingRound.getStartDate();
            TimerDayStatistic dayStatistic = getDayStatisticOrNew(date);
            dayStatistic.statResetTime(countingRound.getResetTime());
        }
    }

    public void reportCountingRoundEnd(CountingRound countingRound) {
        Assert.isTrue(countingRound.isFinished(), "CountingRound not finished!");
        OptionConstants.StatDefaultMethod statDefaultMethod = OptionManager.getInstance().getOptionValue(OptionConstants.OPTION_GENERAL_STAT_DEFAULT, OptionConstants.StatDefaultMethod.class);
        if(statDefaultMethod != OptionConstants.StatDefaultMethod.DISABLED) {
            Date statDate;
            if(statDefaultMethod == OptionConstants.StatDefaultMethod.STAT_BY_START_DAY) {
                statDate = countingRound.getStartDate();
            } else {
                statDate = countingRound.getEndDate();
            }
            TimerDayStatistic dayStatistic = getDayStatisticOrNew(statDate);
            dayStatistic.statCountedTime(countingRound.getCountedTime());
        }
    }

    public static TimerStatistic getInstance() {
        return instance;
    }

    @Override
    public String instantiationMethodName() {
        return "getInstance";
    }

    @Override
    public byte[] writeToSnapshot() {
        SnapshotWriter writer = SnapshotWriter.writer();
        writer.writeInt(dayStatisticMap.size());
        for(Map.Entry<Date, TimerDayStatistic> entry: dayStatisticMap.entrySet()) {
            Date date = entry.getKey();
            TimerDayStatistic dayStatistic = entry.getValue();
            Time totalResetTime = dayStatistic.getTotalResetTime(),
                    totalCountedTime = dayStatistic.getTotalCountedTime();
            writer.writeInt(date.getYear()).writeInt(date.getMonth()).writeInt(date.getDay())
                    .writeInt(totalResetTime.getHour()).writeInt(totalResetTime.getMinute()).writeInt(totalResetTime.getSecond())
                    .writeInt(totalCountedTime.getHour()).writeInt(totalCountedTime.getMinute()).writeInt(totalCountedTime.getSecond());
        }
        return writer.toByteArray();
    }

    @Override
    public void restoreFromSnapshot(byte[] bytes) {
        SnapshotReader reader = SnapshotReader.reader(bytes);
        int size = reader.readInt();
        while(size-->0) {
            Date date = new Date(reader.readInt(), reader.readInt(), reader.readInt());
            Time totalResetTime = new Time(reader.readInt(), reader.readInt(), reader.readInt());
            Time totalCountedTime = new Time(reader.readInt(), reader.readInt(), reader.readInt());
            TimerDayStatistic dayStatistic = getDayStatisticOrNew(date);
            dayStatistic.statResetTime(totalResetTime);
            dayStatistic.statCountedTime(totalCountedTime);
            dayStatisticMap.put(date, dayStatistic);
        }
    }
}
