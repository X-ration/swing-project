package com.adam.swing_project.timer.core;

import com.adam.swing_project.timer.assertion.Assert;
import com.adam.swing_project.timer.helper.Logger;
import com.adam.swing_project.timer.helper.TimerStatistic;
import com.adam.swing_project.timer.snapshot.SnapshotManager;
import com.adam.swing_project.timer.snapshot.SnapshotReader;
import com.adam.swing_project.timer.snapshot.SnapshotWriter;
import com.adam.swing_project.timer.snapshot.Snapshotable;
import com.adam.swing_project.timer.thread.ThreadManager;
import com.adam.swing_project.timer.thread.TimerThread;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 计时器后端类，对应计时器的计时时间等内部属性
 * 对前端组件提供接口，并与TimerThread交互
 */
 //todo 持久化恢复后重新计算targetTime
public class Timer implements Snapshotable<Timer> {

    private final Time resetTime, targetTime, countingTime, startTime;
    private final Date startDate;
    private TimerStatus status;
    private TimerThread.TimerTask timerTask;
    private final List<TimerListener> timerListenerList = new ArrayList<>();
    private TimerThread timerThread;
    private long lastPauseTimerTaskStartTimeMills, lastPauseTimerTaskExitTimeMills, lastPauseTimerTaskTargetTimeMills;
    private final Logger logger = Logger.createLogger(this);
    /**
     * 从快照恢复超时状态
     */
    private boolean restoreCountingDone = false;

    public enum TimerStatus {
        STOPPED, COUNTING, PAUSED, USER_STOPPED
    }
    public class Time {
        private int hour;
        private int minute;
        private int second;

        public Time(int hour, int minute, int second) {
            this.hour = hour;
            this.minute = minute;
            this.second = second;
        }

        public int getHour() {
            return hour;
        }
        public int getMinute() {
            return minute;
        }
        public int getSecond() {
            return second;
        }
        private void setAllField(int hour, int minute, int second) {
            this.hour = hour;
            this.minute = minute;
            this.second = second;
        }
    }
    public class Date {
        private int year;
        private int month;
        private int day;

        public Date(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }

        public int getYear() {
            return year;
        }

        public int getMonth() {
            return month;
        }

        public int getDay() {
            return day;
        }

        private void setAllField(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }
    }

    public interface TimerListener {
        void timerStarted();
        void timerPaused();
        void timerStopped();
        void timerStoppedByUser();
        void timerUpdated();
        void timerReset();
    }

    public class TimeAdapter implements TimerListener {

        @Override
        public void timerStarted() {

        }

        @Override
        public void timerPaused() {

        }

        @Override
        public void timerStopped() {

        }

        @Override
        public void timerStoppedByUser() {

        }

        @Override
        public void timerUpdated() {

        }

        @Override
        public void timerReset() {

        }
    }


    public Timer() {
        this(0,0,0);
    }
    public Timer(int resetHour, int resetMinute, int resetSecond) {
        Assert.isTrue(resetHour >= 0, "hour>=0");
        Assert.isTrue(resetMinute >= 0, "minute>=0");
        Assert.isTrue(resetSecond >= 0, "second>=0");
        this.resetTime = new Time(resetHour, resetMinute, resetSecond);
        this.targetTime = new Time(0,0,0);
        this.countingTime = new Time(0,0,0);
        this.startTime = new Time(0,0,0);
        this.startDate = new Date(0, 0, 0);
        this.status = TimerStatus.STOPPED;
        this.timerThread = ThreadManager.getInstance().getTimerThread();
        SnapshotManager.getInstance().registerSnapshotable(this);
    }

    /**
     * 开始计时，外部调用
     */
    public void startTimer() {
        Assert.isTrue(!(resetTime.hour == 0 && resetTime.minute == 0 && resetTime.second == 0), "请先重设时间！");
        requireStatus(TimerStatus.STOPPED, TimerStatus.USER_STOPPED, TimerStatus.PAUSED);
        if(status == TimerStatus.STOPPED || status == TimerStatus.USER_STOPPED) {
            this.countingTime.hour = resetTime.hour;
            this.countingTime.minute = resetTime.minute;
            this.countingTime.second = resetTime.second;
            Time startTime = getCurrentTime(), targetTime = getCurrentTimePlus(resetTime.hour, resetTime.minute, resetTime.second);
            this.startTime.hour = startTime.hour;
            this.startTime.minute = startTime.minute;
            this.startTime.second = startTime.second;
            this.targetTime.hour = targetTime.hour;
            this.targetTime.minute = targetTime.minute;
            this.targetTime.second = targetTime.second;
            Date startDate = getCurrentDate();
            this.startDate.year = startDate.year;
            this.startDate.month = startDate.month;
            this.startDate.day = startDate.day;
            this.timerTask = timerThread.new TimerTask(1, TimeUnit.SECONDS, this::count1s);
            this.timerTask.setLoopTask(true, translateTimeToSeconds(resetTime), TimeUnit.SECONDS);
            logger.logDebug("Timer停止转开始状态注册任务");
            this.timerThread.registerTask(timerTask);
        } else {
            //恢复暂停前状态，即按照暂停前的时间点执行计时任务
            logger.logDebug("Timer暂停转开始状态移除任务");
            this.timerThread.removeTask(timerTask);
//            long newStartTimeMills = System.currentTimeMillis() / 1000 * 1000 + lastPauseTimerTask.getStartTimeMills() % 1000;
            long millsPassed = lastPauseTimerTaskExitTimeMills - lastPauseTimerTaskStartTimeMills;
            long currentTimeMills = System.currentTimeMillis();
            long millsPaused = currentTimeMills - lastPauseTimerTaskExitTimeMills;
            long newStartTimeMills = currentTimeMills - millsPassed % 1000;
            this.timerTask = timerThread.new TimerTask(newStartTimeMills, 1, TimeUnit.SECONDS, this::count1s);
            this.timerTask.setLoopTask(true, lastPauseTimerTaskTargetTimeMills + millsPaused);
            if(logger.debugEnabled()) {
                logger.logDebug("current=" + currentTimeMills + ",target=" + timerTask.getTargetTimeMills());
                logger.logDebug("Timer暂停转开始状态注册任务");
            }
            this.timerThread.registerTask(timerTask);
        }
        this.status = TimerStatus.COUNTING;
        timerStarted();
    }

    /**
     * 暂停计时，外部调用
     */
    public void pauseTimer() {
        requireStatus(TimerStatus.COUNTING);
        this.status = TimerStatus.PAUSED;
        logger.logDebug("Timer开始转暂停状态移除任务");
        timerThread.removeTask(timerTask);
        lastPauseTimerTaskStartTimeMills = timerTask.getStartTimeMills();
        lastPauseTimerTaskExitTimeMills = timerTask.getExitTimeMills();
        lastPauseTimerTaskTargetTimeMills = timerTask.getTargetTimeMills();
        timerTask = timerThread.new TimerTask(timerTask.getExitTimeMills() / 1000 * 1000,
                1, TimeUnit.SECONDS, this::timerPausedTaskAction);
        timerTask.setLoopTask(true, -1);
        logger.logDebug("Timer开始转暂停状态注册任务");
        timerThread.registerTask(timerTask);
        timerPaused();
    }

    private void timerPausedTaskAction() {
        if(targetTime.second < 59) {
            targetTime.second++;
        } else if(targetTime.minute < 59) {
            targetTime.second = 0;
            targetTime.minute++;
        } else if(targetTime.hour < 23) {
            targetTime.second = 0;
            targetTime.minute = 0;
            targetTime.hour++;
        } else {
            targetTime.second = 0;
            targetTime.minute = 0;
            targetTime.hour = 0;
        }
        timerUpdated();
    }

    /**
     * 结束计时，外部调用
     */
    public void stopTimer() {
        requireStatus(TimerStatus.COUNTING, TimerStatus.PAUSED);
        timerThread.removeTask(timerTask);
        timerTask = null;
        this.status = TimerStatus.USER_STOPPED;
        this.startTime.setAllField(0,0,0);
        this.countingTime.setAllField(0,0,0);
        this.targetTime.setAllField(0,0,0);
        this.startDate.setAllField(0,0,0);
        logger.logDebug("Timer转停止状态移除任务");
        timerStoppedByUser();
    }

    /**
     * 重设计时，外部调用
     */
    public void resetTimer(int hour, int minute) {
        requireStatus(TimerStatus.STOPPED, TimerStatus.USER_STOPPED);
        this.resetTime.hour = hour;
        this.resetTime.minute = minute;
        this.startTime.setAllField(0,0,0);
        this.countingTime.setAllField(0,0,0);
        this.targetTime.setAllField(0,0,0);
        this.startDate.setAllField(0,0,0);
        timerReset();
    }


    @Override
    public byte[] writeToSnapshot() {
        SnapshotWriter snapshotWriter = SnapshotWriter.writer();
        snapshotWriter.writeInt(status.ordinal());
        long currentTimeMills = System.currentTimeMillis();
        logger.logDebug("writing snapshot current=" + currentTimeMills);
        switch (status) {
            case STOPPED:
            case USER_STOPPED:
                snapshotWriter.writeInt(resetTime.hour).writeInt(resetTime.minute);
                break;
            case COUNTING:
                snapshotWriter.writeLong(timerTask.getStartTimeMills()).writeLong(timerTask.getTargetTimeMills()).writeLong(currentTimeMills);
                snapshotWriter.writeInt(resetTime.hour).writeInt(resetTime.minute);
                snapshotWriter.writeInt(startDate.year).writeInt(startDate.month).writeInt(startDate.day);
                snapshotWriter.writeInt(countingTime.hour).writeInt(countingTime.minute).writeInt(countingTime.second);
                snapshotWriter.writeInt(startTime.hour).writeInt(startTime.minute).writeInt(startTime.second);
                snapshotWriter.writeInt(targetTime.hour).writeInt(targetTime.minute).writeInt(targetTime.second);
                break;
            case PAUSED:
                snapshotWriter.writeLong(lastPauseTimerTaskStartTimeMills).writeLong(lastPauseTimerTaskExitTimeMills)
                        .writeLong(lastPauseTimerTaskTargetTimeMills);
                snapshotWriter.writeInt(startDate.year).writeInt(startDate.month).writeInt(startDate.day);
                snapshotWriter.writeInt(resetTime.hour).writeInt(resetTime.minute);
                snapshotWriter.writeInt(countingTime.hour).writeInt(countingTime.minute).writeInt(countingTime.second);
                snapshotWriter.writeInt(startTime.hour).writeInt(startTime.minute).writeInt(startTime.second);
                snapshotWriter.writeInt(targetTime.hour).writeInt(targetTime.minute).writeInt(targetTime.second);
        }
        return snapshotWriter.toByteArray();
    }

    @Override
    public Timer restoreFromSnapshot(byte[] bytes) {
        SnapshotReader snapshotReader = SnapshotReader.reader(bytes);
        int statusOrdinal = snapshotReader.readInt();
        status = TimerStatus.values()[statusOrdinal];
        switch (status) {
            case STOPPED:
            case USER_STOPPED:
                int hour = snapshotReader.readInt();
                int minute = snapshotReader.readInt();
                resetTimer(hour, minute);
                break;
            case COUNTING:
                long startTimeMills = snapshotReader.readLong(), targetTimeMills = snapshotReader.readLong();
                long programExitTimeMills = snapshotReader.readLong();
                long currentTimeMills = System.currentTimeMillis();
                logger.logDebug("readVals:" + startTimeMills + "," + targetTimeMills + "," + programExitTimeMills + "," + currentTimeMills);
                resetTime.hour = snapshotReader.readInt();
                resetTime.minute = snapshotReader.readInt();
                startDate.year = snapshotReader.readInt();
                startDate.month = snapshotReader.readInt();
                startDate.day = snapshotReader.readInt();
                //已经超时
                if(targetTimeMills <= currentTimeMills) {
                    restoreCountingDone = true;
                    status = TimerStatus.STOPPED;
                } else {
                    long newStartTimeMills = currentTimeMills /1000*1000 + startTimeMills%1000;
                    if(startTimeMills%1000>=currentTimeMills%1000) {
                        newStartTimeMills-=1000;
                    }
                    long millsPassed = currentTimeMills - programExitTimeMills + (programExitTimeMills - startTimeMills) % 1000;
                    int secondsPassed = (int)(millsPassed/1000);
                    this.timerTask = timerThread.new TimerTask(newStartTimeMills, 1, TimeUnit.SECONDS, this::count1s);
                    this.timerTask.setLoopTask(true, targetTimeMills);
                    countingTime.hour = snapshotReader.readInt();
                    countingTime.minute = snapshotReader.readInt();
                    countingTime.second = snapshotReader.readInt();
                    logger.logDebug("secondsPassed=" + secondsPassed);
                    logger.logDebug("countingTime=" + countingTime.hour + "," + countingTime.minute + "," + countingTime.second);
                    while(secondsPassed-->0) {
                        count1sInternal();
                    }
                    logger.logDebug("countingTime=" + countingTime.hour + "," + countingTime.minute + "," + countingTime.second);
                    startTime.hour = snapshotReader.readInt();
                    startTime.minute = snapshotReader.readInt();
                    startTime.second = snapshotReader.readInt();
                    targetTime.hour = snapshotReader.readInt();
                    targetTime.minute = snapshotReader.readInt();
                    targetTime.second = snapshotReader.readInt();
                    this.timerThread.registerTask(timerTask);
                    if(logger.debugEnabled()) {
                        logger.logDebug("current=" + currentTimeMills + ",target=" + timerTask.getTargetTimeMills());
                        logger.logDebug("Timer从快照恢复到计时状态");
                    }
                }
                break;
            case PAUSED:
                lastPauseTimerTaskStartTimeMills = snapshotReader.readLong();
                lastPauseTimerTaskExitTimeMills = snapshotReader.readLong();
                lastPauseTimerTaskTargetTimeMills = snapshotReader.readLong();
                startDate.year = snapshotReader.readInt();
                startDate.month = snapshotReader.readInt();
                startDate.day = snapshotReader.readInt();
                resetTime.hour = snapshotReader.readInt();
                resetTime.minute = snapshotReader.readInt();
                countingTime.hour = snapshotReader.readInt();
                countingTime.minute = snapshotReader.readInt();
                countingTime.second = snapshotReader.readInt();
                startTime.hour = snapshotReader.readInt();
                startTime.minute = snapshotReader.readInt();
                startTime.second = snapshotReader.readInt();
                targetTime.hour = snapshotReader.readInt();
                targetTime.minute = snapshotReader.readInt();
                targetTime.second = snapshotReader.readInt();
                currentTimeMills = System.currentTimeMillis();
                timerTask = timerThread.new TimerTask(currentTimeMills / 1000 * 1000,
                        1, TimeUnit.SECONDS, this::timerPausedTaskAction);
                timerTask.setLoopTask(true, -1);
                logger.logDebug("Timer开始转暂停状态注册任务");
                timerThread.registerTask(timerTask);
                if(logger.debugEnabled()) {
                    logger.logDebug("current=" + currentTimeMills + ",target=" + timerTask.getTargetTimeMills());
                    logger.logDebug("Timer从快照恢复到暂停状态");
                }
        }
        return this;
    }


    private void requireStatus(TimerStatus... statuses) {
        boolean permitStatus = false;
        for(TimerStatus status: statuses) {
            if(status == this.status) {
                permitStatus = true;
            }
        }
        Assert.isTrue(permitStatus, "不允许的操作，当前状态" + this.status + " 允许的状态" + Arrays.toString(statuses));
    }

    public TimerStatus getStatus() {
        return status;
    }

    public boolean isRestoreCountingDone() {
        return restoreCountingDone;
    }

    /**
     * 获取预设计时时间，前端调用
     * @return
     */
    public Time getResetTime() {
        return resetTime;
    }

    public Date getStartDate() {
        return startDate;
    }

    /**
     * 获取正在计时的剩余时间，前端调用
     * @return
     */
    public Time getCountingTime() {
        return countingTime;
    }

    /**
     * 获取计时截止时间，前端调用
     * @return
     */
    public Time getTargetTime() {
        if(status != TimerStatus.COUNTING && status != TimerStatus.PAUSED){
            return null;
        }
        return targetTime;
    }

    /**
     * 获取计时的开始时间，前端调用
     * @return
     */
    public Time getStartTime() {
        if(status != TimerStatus.COUNTING && status != TimerStatus.PAUSED){
            return null;
        }
        return startTime;
    }

    /**
     * 计时1秒，计时线程调用
     */
    public void count1s() {
        requireStatus(TimerStatus.COUNTING);
        Assert.isTrue(!(countingTime.hour == 0 && countingTime.minute == 0 && countingTime.second == 0), "Timer无法再减！");
        count1sInternal();
        timerUpdated();
        if(countingTime.second == 0 && countingTime.minute == 0 && countingTime.hour == 0) {
            this.status = TimerStatus.STOPPED;
            this.startTime.setAllField(0,0,0);
            this.countingTime.setAllField(0,0,0);
            this.targetTime.setAllField(0,0,0);
            timerStopped();
        }
    }

    private void count1sInternal() {
        if(countingTime.second > 0) {
            countingTime.second--;
        } else if(countingTime.minute > 0) {
            countingTime.minute--;
            countingTime.second = 59;
        } else if(countingTime.hour > 0){
            countingTime.hour--;
            countingTime.minute = countingTime.second = 59;
        }
    }

    /**
     * 注册TimerListener，前端调用
     * @param timerListener
     */
    public void registerTimerListener(TimerListener timerListener) {
        Assert.notNull(timerListener);
        this.timerListenerList.add(timerListener);
    }

    private void timerStarted() {
        for(TimerListener timerListener: timerListenerList) {
            timerListener.timerStarted();
        }
    }

    private void timerPaused() {
        for(TimerListener timerListener: timerListenerList) {
            timerListener.timerPaused();
        }
    }

    private void timerStopped() {
        for(TimerListener timerListener: timerListenerList) {
            timerListener.timerStopped();
        }
    }

    private void timerStoppedByUser() {
        for(TimerListener timerListener: timerListenerList) {
            timerListener.timerStoppedByUser();
        }
    }

    private void timerUpdated() {
        for(TimerListener timerListener: timerListenerList) {
            timerListener.timerUpdated();
        }
    }

    private void timerReset() {
        for(TimerListener timerListener: timerListenerList) {
            timerListener.timerReset();
        }
    }

    private Time getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        return new Time(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));
    }
    private Time getCurrentTimePlus(int hour, int minute, int second) {
        java.util.Date date = new java.util.Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR, hour);
        calendar.add(Calendar.MINUTE, minute);
        calendar.add(Calendar.SECOND, second);
        return new Time(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));
    }
    private Date getCurrentDate() {
        Calendar calendar = Calendar.getInstance();
        return new Date(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
    }

    private long translateTimeToSeconds(Time time) {
        long result = 0;
        result += (time.hour * 3600L);
        result += (time.minute * 60L);
        result += (time.second);
        return result;
    }

    public static void main(String[] args) {
        Timer timer = new Timer();
        Time time1 = timer.getCurrentTime(), time2 = timer.getCurrentTimePlus(0,1,0);
        System.out.println(time1);
        System.out.println(time2);
        Date date = timer.getCurrentDate();
        System.out.println(date);
    }

}
