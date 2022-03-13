package com.adam.swing_project.timer.timer;

import com.adam.swing_project.library.datetime.Time;
import com.adam.swing_project.library.snapshot.*;
import com.adam.swing_project.library.timer.Timer;
import com.adam.swing_project.library.timer.TimerThread;
import com.adam.swing_project.timer.stat.ActionLog;
import com.adam.swing_project.timer.stat.ActionLogManager;

import java.util.concurrent.TimeUnit;

public class ExtendedTimer extends Timer implements CustomInstantiationSnapshotable {

    private int timerId;

    public ExtendedTimer() {
        this("unnamed extended timer");
    }

    public ExtendedTimer(String timerName) {
        this(timerName, true);
    }

    private ExtendedTimer(String timerName, boolean brandNew) {
        super(timerName);
        SnapshotManager.getInstance().registerSnapshotable(this);
        TimerThread.TimerTaskAction parentCountingAction = super.countingTimeAction;
        super.countingTimeAction = () -> {
            parentCountingAction.action();
            if (countingTime.getHour() == 0 && countingTime.getMinute() == 0 && countingTime.getSecond() == 0) {
                addActionLog(ActionLog.ActionLogType.TIMER_TIME_UP);
            }
        };
        if(brandNew) {
            this.timerId = TimerManager.getInstance().reportNewTimer();
            logger.logDebug("new ExtendedTimer id=" + timerId);
//            这里记日志会受到持久化影响多记日志，而且INITIALIZE类型对于统计没有作用
//            addActionLog(ActionLog.ActionLogType.TIMER_INITIALIZE);
        }
    }

    @Override
    public void start() {
        super.start();
        addActionLog(ActionLog.ActionLogType.TIMER_START);
    }

    @Override
    public void pause() {
        super.pause();
        addActionLog(ActionLog.ActionLogType.TIMER_PAUSE);
    }

    @Override
    public void stop() {
        super.stop();
        addActionLog(ActionLog.ActionLogType.TIMER_STOP);
    }

    @Override
    public void reset(Time time) {
        super.reset(time);
        addActionLog(ActionLog.ActionLogType.TIMER_RESET);
    }

    @Override
    public void terminate() {
        super.terminate();
        logger.logDebug("destroy ExtendedTimer id=" + timerId);
        SnapshotManager.getInstance().removeSnapshotable(this);
        TimerManager.getInstance().reportDestroyTimer();
        addActionLog(ActionLog.ActionLogType.TIMER_TERMINATE);
    }

    public int getTimerId() {
        return timerId;
    }

    void setTimerId(int timerId) {
        this.timerId = timerId;
    }

    @Override
    public byte[] writeToSnapshot() {
        SnapshotWriter writer = SnapshotWriter.writer();
        writer.writeString(timerName);
        writer.writeInt(timerId);
        writer.writeInt(status.ordinal());

        switch (status) {
            case READY:
            case STOPPED:
            case TIME_UP:
                writer.writeInt(resetTime.getHour()).writeInt(resetTime.getMinute()).writeInt(resetTime.getSecond());
                break;
            case RUNNING:
                //todo 考虑快照拍摄时冻结action
                int actionTimeLeft = calcActionTimeLeft(timerTask);
                long currentTimeMills = System.currentTimeMillis();
                int startDelayDuration = calcStartDelayDuration(timerTask, currentTimeMills);
                writer.writeInt(resetTime.getHour()).writeInt(resetTime.getMinute()).writeInt(resetTime.getSecond())
                        .writeInt(countingTime.getHour()).writeInt(countingTime.getMinute()).writeInt(countingTime.getSecond())
                        .writeInt(actionTimeLeft).writeInt(startDelayDuration).writeLong(currentTimeMills);
                break;
            case PAUSED:
                writer.writeInt(resetTime.getHour()).writeInt(resetTime.getMinute()).writeInt(resetTime.getSecond())
                        .writeInt(countingTime.getHour()).writeInt(countingTime.getMinute()).writeInt(countingTime.getSecond())
                        .writeInt(pausedTempArray[0]).writeInt(pausedTempArray[1]);
                break;
            case INITIALIZED:
            default:
                break;
        }

        return writer.toByteArray();
    }

    /**
     * 开发记录：RUNNING状态的经快照恢复后继续运行，会有20ms左右的误差，不过尚在允许范围内
     * @param bytes
     */
    @Override
    public void restoreFromSnapshot(byte[] bytes) {
        SnapshotReader reader = SnapshotReader.reader(bytes);
        timerName = reader.readString();
        timerId = reader.readInt();
        status = TimerStatus.values()[reader.readInt()];

        switch (status) {
            case READY:
            case STOPPED:
            case TIME_UP:
                resetTime.setAllField(reader.readInt(), reader.readInt(), reader.readInt());
                break;
            case RUNNING:
                resetTime.setAllField(reader.readInt(), reader.readInt(), reader.readInt());
                countingTime.setAllField(reader.readInt(), reader.readInt(), reader.readInt());
                int lastActionTimeLeft = reader.readInt();
                int lastStartDelayDuration = reader.readInt();
                long lastTimeMills = reader.readLong(), currentTimeMills = System.currentTimeMillis();
                int diff = (int)(currentTimeMills - lastTimeMills) - lastStartDelayDuration;
                if(diff < 0) {
                    startInternal(lastActionTimeLeft, -diff, TimeUnit.MILLISECONDS);
                } else {
                    int makeUpTime = 1 + diff / 1000;
                    if(makeUpTime >= lastActionTimeLeft) {
                        countingTime.setAllField(0,0,0);
                        this.status = TimerStatus.TIME_UP;
                        addActionLog(ActionLog.ActionLogType.TIMER_TIME_UP, lastTimeMills + 1000 - diff % 1000 + lastActionTimeLeft * 1000L);
                    } else {
                        for(int i=0;i<makeUpTime;i++) {
                            this.countingTimeAction.action();
                        }
                        startInternal(lastActionTimeLeft - makeUpTime, 1000 - diff % 1000, TimeUnit.MILLISECONDS);
                    }
                }
                break;
            case PAUSED:
                resetTime.setAllField(reader.readInt(), reader.readInt(), reader.readInt());
                countingTime.setAllField(reader.readInt(), reader.readInt(), reader.readInt());
                pausedTempArray[0] = reader.readInt();
                pausedTempArray[1] = reader.readInt();
                break;
            case INITIALIZED:
            default:
                break;
        }

        logger.logDebug("Restored ExtendedTimer id=" + timerId);
    }

    private void addActionLog(ActionLog.ActionLogType actionLogType) {
        Time resetTime = new Time(0,0,0), countingTime = new Time(0,0,0);
        resetTime.copyFrom(this.resetTime);
        countingTime.copyFrom(this.countingTime);
        ActionLog actionLog = new ActionLog(timerId, actionLogType, timerName, countingTime, resetTime);
        ActionLogManager.getInstance().addActionLog(actionLog);
    }

    protected void addActionLog(ActionLog.ActionLogType actionLogType, long timeMills) {
        Time resetTime = new Time(0,0,0), countingTime = new Time(0,0,0);
        resetTime.copyFrom(this.resetTime);
        countingTime.copyFrom(this.countingTime);
        ActionLog actionLog = new ActionLog(timerId, actionLogType, timerName, countingTime, resetTime, timeMills);
        ActionLogManager.getInstance().addActionLog(actionLog);
    }

    public static ExtendedTimer newInstanceForSnapshot() {
        return new ExtendedTimer("snapshot timer", false);
    }

    @Override
    public String instantiationMethodName() {
        return "newInstanceForSnapshot";
    }
}
