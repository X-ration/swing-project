package com.adam.swing_project.timer;

import com.adam.swing_project.timer.assertion.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * 计时器单独线程
 */
public class TimerThread extends Thread{

    private static final TimerThread instance = new TimerThread();

    private final Object lock = new Object();
    private TimerStatus status = TimerStatus.STOPPED;

    private int countingHour, countingMinute, countingSecond
            , resetHour, resetMinute, resetSecond;
    private long beforeInterruptTimeMills = 0, interruptTimeMills = 0;
    private boolean needMakeUpTimeMills = false;

    private List<TimerListener> listenerList = new ArrayList<>();

    private enum TimerStatus {
        STOPPED, COUNTING, PAUSED, TERMINATING,
        USER_STOPPED
    }

    public interface TimerListener {
        void timerStarted();
        void timerPaused();
        void timerStopped();
        void timerUpdated(int hour, int minute, int second);
        void timerReset(int hour, int minute);
        void timerStoppedByUser();
    }

    private TimerThread() {
    }

    /**
     * 线程run方法
     */
    @Override
    public void run() {
        System.out.println("TimerThread started.");

        while(status != TimerStatus.TERMINATING) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
//                    e.printStackTrace();
                }
            }
            if(status == TimerStatus.TERMINATING) {
                break;
            }
            while(status == TimerStatus.COUNTING) {
                boolean interrupted = false;
                long sleepTimeMills = 1000;
                if(needMakeUpTimeMills) {
                    sleepTimeMills = 1000 - (interruptTimeMills - beforeInterruptTimeMills);
                    needMakeUpTimeMills = false;
                }
                beforeInterruptTimeMills = System.currentTimeMillis();
                try {
                    Thread.sleep(sleepTimeMills);
                } catch (InterruptedException e) {
//                    e.printStackTrace();
                    interrupted = true;
                }
                if(interrupted) {
                    interruptTimeMills = System.currentTimeMillis();
                    break;
                }
                countOnce();
                timerUpdated();
            }
            if(status == TimerStatus.STOPPED) {
                needMakeUpTimeMills = false;
                timerStopped();
                countingClear();
            } else if(status == TimerStatus.USER_STOPPED) {
                needMakeUpTimeMills = false;
                timerStoppedByUser();
                countingClear();
            } else if(status == TimerStatus.PAUSED) {
                timerPaused();
                needMakeUpTimeMills = true;
            }
        }

        System.out.println("TimerThread terminated.");
    }

    public static TimerThread getInstance() {
        return instance;
    }

    public int getResetHour() {
        return resetHour;
    }

    public int getResetMinute() {
        return resetMinute;
    }

    public int getCountingHour() {
        return countingHour;
    }

    public int getCountingMinute() {
        return countingMinute;
    }

    public int getCountingSecond() {
        return countingSecond;
    }

    /**
     * 重设计时，外部调用
     * 分钟粒度，范围值：00:00~23:59(24小时)
     * @param hour
     * @param minute
     */
    public void resetTime(int hour, int minute) {
        Assert.isTrue(hour >= 0 && hour < 24, "24>hour>=0!");
        Assert.isTrue(minute >= 0 && minute < 60, "60>minute>=0!");
        this.resetHour = hour;
        this.resetMinute = minute;
        this.resetSecond = 0;
        if(hour != 0 || minute != 0) {
            timerReset();
        }
    }

    /**
     * 开始计时，外部调用
     */
    public void startTimer() {
        if(status == TimerStatus.STOPPED ||status == TimerStatus.USER_STOPPED|| status == TimerStatus.PAUSED) {
            if(status == TimerStatus.STOPPED || status == TimerStatus.USER_STOPPED) {
                if(resetHour > 0 || resetMinute > 0) {
                    countingHour = resetHour;
                    countingMinute = resetMinute;
                }
            }

            status = TimerStatus.COUNTING;
            synchronized (lock) {
                lock.notify();
            }
            timerStarted();
        }
    }
    /**
     * 暂停计时，外部调用
     */
    public void pauseTimer() {
        status = TimerStatus.PAUSED;
        TimerThread.getInstance().interrupt();
    }
    /**
     * 停止计时，外部调用
     */
    public void stopTimer() {
        status = TimerStatus.USER_STOPPED;
        TimerThread.getInstance().interrupt();
    }

    public void terminate() {
        status = TimerStatus.TERMINATING;
        TimerThread.getInstance().interrupt();
    }

    /**
     * 计算消耗一秒的剩余时间
     */
    private void countOnce() {
        if(countingSecond > 0) {
            countingSecond--;
        } else if(countingMinute > 0) {
            countingMinute--;
            countingSecond= 59;
        } else if(countingHour> 0) {
            countingHour--;
            countingMinute = 59;
            countingSecond = 59;
        } else {
            status = TimerStatus.STOPPED;
        }
    }

    private void countingClear() {
        this.countingHour = 0;
        this.countingMinute = 0;
        this.countingSecond = 0;
    }

    public void registerListener(TimerListener listener) {
        listenerList.add(listener);
    }

    private void timerStarted() {
        for(TimerListener listener: listenerList) {
            listener.timerStarted();
        }
    }

    private void timerStopped() {
        TimerStatistic.getInstance().recordNaturalCounting(resetHour, resetMinute);
        for(TimerListener listener: listenerList) {
            listener.timerStopped();
        }
    }

    private void timerPaused() {
        for(TimerListener listener: listenerList) {
            listener.timerPaused();
        }
    }

    private void timerUpdated() {
        for(TimerListener listener: listenerList) {
            listener.timerUpdated(countingHour, countingMinute, countingSecond);
        }
    }

    private void timerReset() {
        for(TimerListener listener: listenerList) {
            listener.timerReset(resetHour, resetMinute);
        }
    }

    private void timerStoppedByUser() {
        int diffHour = resetHour - countingHour
                , diffMinute = resetMinute - countingMinute
                , diffSecond = resetSecond - countingSecond;
        if(diffSecond < 0) {
            diffSecond += 60;
            diffMinute -= 1;
        }
        if(diffMinute < 0) {
            diffMinute += 60;
            diffHour -= 1;
        }
        TimerStatistic.getInstance().recordUserStoppedCounting(diffHour, diffMinute, diffSecond);
        for(TimerListener listener: listenerList) {
            listener.timerStoppedByUser();
        }
    }

}
