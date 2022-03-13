package com.adam.swing_project.timer.stat;

import com.adam.swing_project.library.datetime.Date;
import com.adam.swing_project.library.datetime.Time;
import com.adam.swing_project.library.snapshot.SnapshotReader;
import com.adam.swing_project.library.snapshot.SnapshotWriter;
import com.adam.swing_project.library.snapshot.Snapshotable;

/**
 * 和Timer关联的事件记录，例如开始计时，停止计时等，后期可以制成表格形式展示
 */
public class ActionLog implements Snapshotable {

    public enum ActionLogType {
        TIMER_INITIALIZE, TIMER_RESET, TIMER_START, TIMER_PAUSE, TIMER_STOP, TIMER_TIME_UP, TIMER_TERMINATE
    }

    private ActionLogType actionLogType;
    private java.util.Date utilDate;
    private Date date;
    private String timerName;
    private Time countingTime, resetTime;
    private int timerId;

    private ActionLog() {}

    public ActionLog(int timerId, ActionLogType actionLogType, String timerName, Time countingTime, Time resetTime) {
        this(timerId, actionLogType, new java.util.Date(), timerName, countingTime, resetTime);
    }

    public ActionLog(int timerId, ActionLogType actionLogType, String timerName, Time countingTime, Time resetTime, long timeMills) {
        this(timerId, actionLogType, new java.util.Date(timeMills), timerName, countingTime, resetTime);
    }

    public ActionLog(int timerId, ActionLogType actionLogType, java.util.Date utilDate, String timerName, Time countingTime, Time resetTime) {
        this.timerId = timerId;
        this.utilDate = utilDate;
        this.date = new Date(utilDate);
        this.actionLogType = actionLogType;
        this.timerName = timerName;
        this.countingTime = countingTime;
        this.resetTime = resetTime;
    }

    @Override
    public byte[] writeToSnapshot() {
        SnapshotWriter writer = SnapshotWriter.writer();
        writer.writeInt(timerId);
        writer.writeInt(actionLogType.ordinal());
        writer.writeLong(utilDate.getTime());
        writer.writeString(timerName);
        writer.writeInt(resetTime.getHour()).writeInt(resetTime.getMinute()).writeInt(resetTime.getSecond());
        writer.writeInt(countingTime.getHour()).writeInt(countingTime.getMinute()).writeInt(countingTime.getSecond());
        return writer.toByteArray();
    }

    @Override
    public void restoreFromSnapshot(byte[] bytes) {
        SnapshotReader reader = SnapshotReader.reader(bytes);
        this.timerId = reader.readInt();
        this.actionLogType = ActionLogType.values()[reader.readInt()];
        this.utilDate = new java.util.Date(reader.readLong());
        this.date = new Date(utilDate);
        this.timerName = reader.readString();
        this.resetTime = new Time(reader.readInt(), reader.readInt(), reader.readInt());
        this.countingTime = new Time(reader.readInt(), reader.readInt(), reader.readInt());
    }

    public ActionLogType getActionLogType() {
        return actionLogType;
    }

    public java.util.Date getUtilDate() {
        return utilDate;
    }

    public Date getDate() {
        return date;
    }

    public String getTimerName() {
        return timerName;
    }

    public Time getCountingTime() {
        return countingTime;
    }

    public Time getResetTime() {
        return resetTime;
    }

    public int getTimerId() {
        return timerId;
    }

    void setTimerId(int timerId) {
        this.timerId = timerId;
    }
}
