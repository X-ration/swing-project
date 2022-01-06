package com.adam.swing_project.timer.stat;

import com.adam.swing_project.library.datetime.Date;
import com.adam.swing_project.library.datetime.Time;
import com.adam.swing_project.library.snapshot.CustomInstantiationSnapshotable;
import com.adam.swing_project.library.snapshot.SnapshotManager;
import com.adam.swing_project.library.snapshot.SnapshotReader;
import com.adam.swing_project.library.snapshot.SnapshotWriter;
import com.adam.swing_project.library.timer.action_log.ActionLog;
import com.adam.swing_project.library.timer.action_log.ActionLogManager;
import com.adam.swing_project.library.util.DateTimeUtil;

import java.util.*;

public class ActionLogStatistic implements CustomInstantiationSnapshotable {

    private static final ActionLogStatistic instance = new ActionLogStatistic();
    private final Map<Date, ActionLogDayStatistic> dayStatisticMap = new HashMap<>();

    static {
        SnapshotManager.getInstance().registerSnapshotable(instance);
    }

    /**
     * 获取所有有数据的日期
     * @return
     */
    public Date[] availableDates() {
        List<Date> result = new LinkedList<>();
        Iterator<Date> actionLogDateIterator = ActionLogManager.getInstance().getActionLogDateIterator();
        while(actionLogDateIterator.hasNext()) {
            result.add(actionLogDateIterator.next());
        }
        result.sort(Comparator.reverseOrder());
        return result.toArray(new Date[result.size()]);
    }

    public List<ActionLog> getDetailedActionLogByDate(Date date) {
        return ActionLogManager.getInstance().getActionLogListByDate(date, false);
    }

    public ActionLogDayStatistic getDayStatistic(Date date) {
        if(dayStatisticMap.containsKey(date)) {
            return dayStatisticMap.get(date);
        }
        ActionLogDayStatistic dayStatistic = new ActionLogDayStatistic(date, getDetailedActionLogByDate(date));
        if(!DateTimeUtil.getCurrentDate().equals(date)) {
            dayStatisticMap.put(date, dayStatistic);
        }
        return dayStatistic;
    }

    public void reviseDayStatistic(Date date, Time totalResetTime, Time totalCountedTime) {
        dayStatisticMap.put(date, new ActionLogDayStatistic(date, totalResetTime, totalCountedTime));
    }

    public static ActionLogStatistic getInstance() {
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
        for(Map.Entry<Date, ActionLogDayStatistic> entry: dayStatisticMap.entrySet()) {
            Date date = entry.getKey();
            ActionLogDayStatistic dayStatistic = entry.getValue();
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
            ActionLogDayStatistic dayStatistic = new ActionLogDayStatistic(date, totalResetTime, totalCountedTime);
            dayStatisticMap.put(date, dayStatistic);
        }
    }
}
