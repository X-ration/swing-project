package com.adam.swing_project.timer.stat;

import com.adam.swing_project.library.datetime.Time;

import java.util.Date;

/**
 * 和Timer关联的事件记录，例如开始计时，停止计时等，后期可以制成表格形式展示
 */
public class ActionLog {

    public enum ActionLogType {
        TIMER_INITIALIZED, TIMER_RESET, TIMER_RUNNING, TIMER_PAUSED, TIMER_STOPPED, TIMER_TIME_UP, TIMER_TERMINATED
    }

    private final ActionLogType actionLogType;
    private final Date date;
    private final String timerName;
    private final Time countingTime, resetTime;

    public ActionLog(ActionLogType actionLogType, String timerName, Time countingTime, Time resetTime) {
        this.date = new Date();
        this.actionLogType = actionLogType;
        this.timerName = timerName;
        this.countingTime = countingTime;
        this.resetTime = resetTime;
    }

}
