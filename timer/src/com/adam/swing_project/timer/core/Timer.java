package com.adam.swing_project.timer.core;

import com.adam.swing_project.timer.assertion.Assert;
import com.adam.swing_project.timer.helper.Logger;
import com.adam.swing_project.timer.thread.ThreadManager;
import com.adam.swing_project.timer.thread.TimerThread;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 计时器后端类，对应计时器的计时时间等内部属性
 * 对前端组件提供接口，并与TimerThread交互
 */
public class Timer {

    private final Time resetTime, targetTime, countingTime, startTime;
    private TimerStatus status;
    private TimerThread.TimerTask timerTask;
    private final List<TimerListener> timerListenerList = new ArrayList<>();
    private TimerThread timerThread;
    private TimerThread.TimerTask lastPauseTimerTask;
    private final Logger logger = Logger.createLogger(this);

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
        this.status = TimerStatus.STOPPED;
        this.timerThread = ThreadManager.getInstance().getTimerThread();
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
            this.timerTask = timerThread.new TimerTask(1, TimeUnit.SECONDS, this::count1s);
            this.timerTask.setLoopTask(true, translateTimeToSeconds(resetTime), TimeUnit.SECONDS);
            logger.logDebug("Timer停止转开始状态注册任务");
            this.timerThread.registerTask(timerTask);
        } else {
            //恢复暂停前状态，即按照暂停前的时间点执行计时任务
            logger.logDebug("Timer暂停转开始状态移除任务");
            this.timerThread.removeTask(timerTask);
//            long newStartTimeMills = System.currentTimeMillis() / 1000 * 1000 + lastPauseTimerTask.getStartTimeMills() % 1000;
            long millsPassed = lastPauseTimerTask.getExitTimeMills() - lastPauseTimerTask.getStartTimeMills();
            long currentTimeMills = System.currentTimeMillis();
            long millsPaused = currentTimeMills - lastPauseTimerTask.getExitTimeMills();
            long newStartTimeMills = currentTimeMills - millsPassed % 1000;
            this.timerTask = timerThread.new TimerTask(newStartTimeMills, 1, TimeUnit.SECONDS, this::count1s);
            this.timerTask.setLoopTask(true, lastPauseTimerTask.getTargetTimeMills() + millsPaused);
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
        lastPauseTimerTask = timerTask;
        timerTask = timerThread.new TimerTask(timerTask.getExitTimeMills() / 1000 * 1000,
                1, TimeUnit.SECONDS, ()->{
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
        });
        timerTask.setLoopTask(true, -1);
        logger.logDebug("Timer开始转暂停状态注册任务");
        timerThread.registerTask(timerTask);
        timerPaused();
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
        timerReset();
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

    /**
     * 获取预设计时时间，前端调用
     * @return
     */
    public Time getResetTime() {
        return resetTime;
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
        if(countingTime.second > 0) {
            countingTime.second--;
        } else if(countingTime.minute > 0) {
            countingTime.minute--;
            countingTime.second = 59;
        } else if(countingTime.hour > 0){
            countingTime.hour--;
            countingTime.minute = countingTime.second = 59;
        }
        timerUpdated();
        if(countingTime.second == 0 && countingTime.minute == 0 && countingTime.hour == 0) {
            this.status = TimerStatus.STOPPED;
            this.startTime.setAllField(0,0,0);
            this.countingTime.setAllField(0,0,0);
            this.targetTime.setAllField(0,0,0);
            timerStopped();
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
        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR, hour);
        calendar.add(Calendar.MINUTE, minute);
        calendar.add(Calendar.SECOND, second);
        return new Time(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));
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
    }

}
