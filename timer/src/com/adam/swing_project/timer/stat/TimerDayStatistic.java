package com.adam.swing_project.timer.stat;

import com.adam.swing_project.library.datetime.Date;
import com.adam.swing_project.library.datetime.Time;
import com.adam.swing_project.library.util.DateTimeUtil;

public class TimerDayStatistic {

    private final Date date;
    private final Time totalResetTime, totalCountedTime;

    public TimerDayStatistic(Date date) {
        this.date = date;
        this.totalResetTime = new Time(0,0,0);
        this.totalCountedTime = new Time(0,0,0);
    }

    public TimerDayStatistic(Date date, Time totalResetTime, Time totalCountedTime) {
        this(date);
        this.totalResetTime.copyFrom(totalResetTime);
        this.totalCountedTime.copyFrom(totalCountedTime);
    }

    void revise(Time totalResetTime, Time totalCountedTime) {
        this.totalResetTime.copyFrom(totalResetTime);
        this.totalCountedTime.copyFrom(totalCountedTime);
    }

    void statResetTime(Time resetTime) {
        DateTimeUtil.timeSelfPlusTime(this.totalResetTime, resetTime);
    }

    void statCountedTime(Time countedTime) {
        DateTimeUtil.timeSelfPlusTime(this.totalCountedTime, countedTime);
    }

    public Date getDate() {
        return date;
    }

    public Time getTotalResetTime() {
        return totalResetTime;
    }

    public Time getTotalCountedTime() {
        return totalCountedTime;
    }
}
