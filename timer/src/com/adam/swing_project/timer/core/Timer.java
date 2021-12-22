//package com.adam.swing_project.timer.core;
//
//import com.adam.swing_project.library.assertion.Assert;
//import com.adam.swing_project.library.datetime.Date;
//import com.adam.swing_project.library.datetime.Time;
//import com.adam.swing_project.library.logger.Logger;
//import com.adam.swing_project.library.snapshot.SnapshotManager;
//import com.adam.swing_project.library.snapshot.SnapshotReader;
//import com.adam.swing_project.library.snapshot.SnapshotWriter;
//import com.adam.swing_project.library.snapshot.Snapshotable;
//import com.adam.swing_project.timer.thread.ThreadManager;
//import com.adam.swing_project.library.timer.TimerThread;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//
//import com.adam.swing_project.library.util.DateTimeUtil;
//
///**
// * 计时器后端类，对应计时器的计时时间等内部属性
// * 对前端组件提供接口，并与TimerThread交互
// */
// //todo 持久化恢复后重新计算targetTime
//public class Timer implements Snapshotable{
//
//    private final Time resetTime, targetTime, countingTime, startTime;
//    private final Date startDate;
//    private TimerStatus status;
//    private TimerThread.TimerTask timerTask;
//    private final List<TimerListener> timerListenerList = new ArrayList<>();
//    private TimerThread timerThread;
//    private long lastPauseTimerTaskStartTimeMills, lastPauseTimerTaskExitTimeMills, lastPauseTimerTaskTargetTimeMills;
//    private final Logger logger = Logger.createLogger(this);
//    /**
//     * 从快照恢复超时状态
//     */
//    private boolean restoreCountingDone = false;
//
//    public enum TimerStatus {
//        STOPPED, COUNTING, PAUSED, USER_STOPPED, TERMINATED
//    }
//
//    public interface TimerListener {
//        void timerStarted();
//        void timerPaused();
//        void timerStopped();
//        void timerStoppedByUser();
//        void timerUpdated();
//        void timerReset();
//    }
//
//    public Timer() {
//        this(0,0,0);
//    }
//    public Timer(int resetHour, int resetMinute, int resetSecond) {
//        Assert.isTrue(resetHour >= 0, "hour>=0");
//        Assert.isTrue(resetMinute >= 0, "minute>=0");
//        Assert.isTrue(resetSecond >= 0, "second>=0");
//        this.resetTime = new Time(resetHour, resetMinute, resetSecond);
//        this.targetTime = new Time(0,0,0);
//        this.countingTime = new Time(0,0,0);
//        this.startTime = new Time(0,0,0);
//        this.startDate = new Date(0, 0, 0);
//        this.status = TimerStatus.STOPPED;
//        this.timerThread = null;//ThreadManager.getInstance().getTimerThread();
//        SnapshotManager.getInstance().registerSnapshotable(this);
//    }
//
//    /**
//     * 开始计时，外部调用
//     */
//    public void startTimer() {
//        Assert.isTrue(!(resetTime.getHour() == 0 && resetTime.getMinute() == 0 && resetTime.getSecond() == 0), "请先重设时间！");
//        requireStatus(TimerStatus.STOPPED, TimerStatus.USER_STOPPED, TimerStatus.PAUSED);
//        if(status == TimerStatus.STOPPED || status == TimerStatus.USER_STOPPED) {
//            this.countingTime.copyFrom(this.resetTime);
//            Time startTime = DateTimeUtil.getCurrentTime(), targetTime = DateTimeUtil.getCurrentTimePlus(resetTime.getHour(), resetTime.getMinute(), resetTime.getSecond());
//            this.startTime.copyFrom(startTime);
//            this.targetTime.copyFrom(targetTime);
//            Date startDate = DateTimeUtil.getCurrentDate();
//            this.startDate.copyFrom(startDate);
//            this.timerTask = timerThread.new TimerTask(1, TimeUnit.SECONDS, this::count1s);
//            this.timerTask.setLoopTask(true, DateTimeUtil.translateTimeToSeconds(resetTime), TimeUnit.SECONDS);
//            logger.logDebug("Timer停止转开始状态注册任务");
//            this.timerThread.registerTask(timerTask);
//        } else {
//            //恢复暂停前状态，即按照暂停前的时间点执行计时任务
//            logger.logDebug("Timer暂停转开始状态移除任务");
//            this.timerThread.removeTask(timerTask);
////            long newStartTimeMills = System.currentTimeMillis() / 1000 * 1000 + lastPauseTimerTask.getStartTimeMills() % 1000;
//            long millsPassed = lastPauseTimerTaskExitTimeMills - lastPauseTimerTaskStartTimeMills;
//            long currentTimeMills = System.currentTimeMillis();
//            long millsPaused = currentTimeMills - lastPauseTimerTaskExitTimeMills;
//            long newStartTimeMills = currentTimeMills - millsPassed % 1000;
//            this.timerTask = timerThread.new TimerTask(newStartTimeMills, 1, TimeUnit.SECONDS, this::count1s);
//            this.timerTask.setLoopTask(true, lastPauseTimerTaskTargetTimeMills + millsPaused);
//            if(logger.debugEnabled()) {
//                logger.logDebug("current=" + currentTimeMills + ",target=" + timerTask.getTargetTimeMills());
//                logger.logDebug("Timer暂停转开始状态注册任务");
//            }
//            this.timerThread.registerTask(timerTask);
//        }
//        this.status = TimerStatus.COUNTING;
//        timerStarted();
//    }
//
//    /**
//     * 暂停计时，外部调用
//     */
//    public void pauseTimer() {
//        requireStatus(TimerStatus.COUNTING);
//        this.status = TimerStatus.PAUSED;
//        logger.logDebug("Timer开始转暂停状态移除任务");
//        timerThread.removeTask(timerTask);
//        lastPauseTimerTaskStartTimeMills = timerTask.getStartTimeMills();
//        lastPauseTimerTaskExitTimeMills = timerTask.getExitTimeMills();
//        lastPauseTimerTaskTargetTimeMills = timerTask.getTargetTimeMills();
//        timerTask = timerThread.new TimerTask(timerTask.getExitTimeMills() / 1000 * 1000,
//                1, TimeUnit.SECONDS, this::timerPausedTaskAction);
//        timerTask.setLoopTask(true, -1);
//        logger.logDebug("Timer开始转暂停状态注册任务");
//        timerThread.registerTask(timerTask);
//        timerPaused();
//    }
//
//    private void timerPausedTaskAction() {
//        targetTime.addSecond();
//        timerUpdated();
//    }
//
//    /**
//     * 结束计时，外部调用
//     */
//    public void stopTimer() {
//        requireStatus(TimerStatus.COUNTING, TimerStatus.PAUSED);
//        timerThread.removeTask(timerTask);
//        timerTask = null;
//        this.status = TimerStatus.USER_STOPPED;
//        this.startTime.setAllField(0,0,0);
//        this.countingTime.setAllField(0,0,0);
//        this.targetTime.setAllField(0,0,0);
//        logger.logDebug("Timer转停止状态移除任务");
//        timerStoppedByUser();
//        //todo 后期优化统计功能只在Timer内触发
//        this.startDate.setAllField(0,0,0);
//    }
//
//    /**
//     * 停止计时器，从程序中移除此对象
//     */
//    public void terminateTimer() {
//        if(timerTask!=null) {
//            timerThread.removeTask(timerTask);
//        }
//        timerTask = null;
//        SnapshotManager.getInstance().removeSnapshotable(this);
//        this.status = TimerStatus.TERMINATED;
//        timerThread = null;
//        logger.logDebug("Timer被移除了");
//    }
//
//    /**
//     * 重设计时，外部调用
//     */
//    public void resetTimer(int hour, int minute) {
//        requireStatus(TimerStatus.STOPPED, TimerStatus.USER_STOPPED);
//        this.resetTime.setHour(hour);
//        this.resetTime.setMinute(minute);
//        this.startTime.setAllField(0,0,0);
//        this.countingTime.setAllField(0,0,0);
//        this.targetTime.setAllField(0,0,0);
//        this.startDate.setAllField(0,0,0);
//        timerReset();
//    }
//
//
//    @Override
//    public byte[] writeToSnapshot() {
//        SnapshotWriter snapshotWriter = SnapshotWriter.writer();
//        snapshotWriter.writeInt(status.ordinal());
//        long currentTimeMills = System.currentTimeMillis();
//        logger.logDebug("writing snapshot current=" + currentTimeMills);
//        switch (status) {
//            case STOPPED:
//            case USER_STOPPED:
//                snapshotWriter.writeInt(resetTime.getHour()).writeInt(resetTime.getMinute());
//                break;
//            case COUNTING:
//                snapshotWriter.writeLong(timerTask.getStartTimeMills()).writeLong(timerTask.getTargetTimeMills()).writeLong(currentTimeMills);
//                snapshotWriter.writeInt(resetTime.getHour()).writeInt(resetTime.getMinute());
//                snapshotWriter.writeInt(startDate.getYear()).writeInt(startDate.getMonth()).writeInt(startDate.getDay());
//                snapshotWriter.writeInt(countingTime.getHour()).writeInt(countingTime.getMinute()).writeInt(countingTime.getSecond());
//                snapshotWriter.writeInt(startTime.getHour()).writeInt(startTime.getMinute()).writeInt(startTime.getSecond());
//                snapshotWriter.writeInt(targetTime.getHour()).writeInt(targetTime.getMinute()).writeInt(targetTime.getSecond());
//                break;
//            case PAUSED:
//                snapshotWriter.writeLong(lastPauseTimerTaskStartTimeMills).writeLong(lastPauseTimerTaskExitTimeMills)
//                        .writeLong(lastPauseTimerTaskTargetTimeMills);
//                snapshotWriter.writeInt(startDate.getYear()).writeInt(startDate.getMonth()).writeInt(startDate.getDay());
//                snapshotWriter.writeInt(resetTime.getHour()).writeInt(resetTime.getMinute());
//                snapshotWriter.writeInt(countingTime.getHour()).writeInt(countingTime.getMinute()).writeInt(countingTime.getSecond());
//                snapshotWriter.writeInt(startTime.getHour()).writeInt(startTime.getMinute()).writeInt(startTime.getSecond());
//                snapshotWriter.writeInt(targetTime.getHour()).writeInt(targetTime.getMinute()).writeInt(targetTime.getSecond());
//        }
//        return snapshotWriter.toByteArray();
//    }
//
//    @Override
//    public void restoreFromSnapshot(byte[] bytes) {
//        SnapshotReader snapshotReader = SnapshotReader.reader(bytes);
//        int statusOrdinal = snapshotReader.readInt();
//        status = TimerStatus.values()[statusOrdinal];
//        switch (status) {
//            case TERMINATED:
//                logger.logWarning("Terminated timer restored");
//                break;
//            case STOPPED:
//            case USER_STOPPED:
//                int hour = snapshotReader.readInt();
//                int minute = snapshotReader.readInt();
//                resetTimer(hour, minute);
//                break;
//            case COUNTING:
//                long startTimeMills = snapshotReader.readLong(), targetTimeMills = snapshotReader.readLong();
//                long programExitTimeMills = snapshotReader.readLong();
//                long currentTimeMills = System.currentTimeMillis();
//                logger.logDebug("readVals:" + startTimeMills + "," + targetTimeMills + "," + programExitTimeMills + "," + currentTimeMills);
//                resetTime.setHour(snapshotReader.readInt());
//                resetTime.setMinute(snapshotReader.readInt());
//                startDate.setYear(snapshotReader.readInt());
//                startDate.setMonth(snapshotReader.readInt());
//                startDate.setDay(snapshotReader.readInt());
//                //已经超时
//                if(targetTimeMills <= currentTimeMills) {
//                    restoreCountingDone = true;
//                    status = TimerStatus.STOPPED;
//                } else {
//                    long newStartTimeMills = currentTimeMills /1000*1000 + startTimeMills%1000;
//                    if(startTimeMills%1000>=currentTimeMills%1000) {
//                        newStartTimeMills-=1000;
//                    }
//                    long millsPassed = currentTimeMills - programExitTimeMills + (programExitTimeMills - startTimeMills) % 1000;
//                    int secondsPassed = (int)(millsPassed/1000);
//                    this.timerTask = timerThread.new TimerTask(newStartTimeMills, 1, TimeUnit.SECONDS, this::count1s);
//                    this.timerTask.setLoopTask(true, targetTimeMills);
//                    countingTime.setHour(snapshotReader.readInt());
//                    countingTime.setMinute(snapshotReader.readInt());
//                    countingTime.setSecond(snapshotReader.readInt());
//                    logger.logDebug("secondsPassed=" + secondsPassed);
//                    logger.logDebug("countingTime=" + countingTime.getHour() + "," + countingTime.getMinute() + "," + countingTime.getSecond());
//                    while(secondsPassed-->0) {
//                        count1sInternal();
//                    }
//                    logger.logDebug("countingTime=" + countingTime.getHour() + "," + countingTime.getMinute() + "," + countingTime.getSecond());
//                    startTime.setHour(snapshotReader.readInt());
//                    startTime.setMinute(snapshotReader.readInt());
//                    startTime.setSecond(snapshotReader.readInt());
//                    targetTime.setHour(snapshotReader.readInt());
//                    targetTime.setMinute(snapshotReader.readInt());
//                    targetTime.setSecond(snapshotReader.readInt());
//                    this.timerThread.registerTask(timerTask);
//                    if(logger.debugEnabled()) {
//                        logger.logDebug("current=" + currentTimeMills + ",target=" + timerTask.getTargetTimeMills());
//                        logger.logDebug("Timer从快照恢复到计时状态");
//                    }
//                }
//                break;
//            case PAUSED:
//                lastPauseTimerTaskStartTimeMills = snapshotReader.readLong();
//                lastPauseTimerTaskExitTimeMills = snapshotReader.readLong();
//                lastPauseTimerTaskTargetTimeMills = snapshotReader.readLong();
//                startDate.setYear(snapshotReader.readInt());
//                startDate.setMonth(snapshotReader.readInt());
//                startDate.setDay(snapshotReader.readInt());
//                resetTime.setHour(snapshotReader.readInt());
//                resetTime.setMinute(snapshotReader.readInt());
//                countingTime.setHour(snapshotReader.readInt());
//                countingTime.setMinute(snapshotReader.readInt());
//                countingTime.setSecond(snapshotReader.readInt());
//                startTime.setHour(snapshotReader.readInt());
//                startTime.setMinute(snapshotReader.readInt());
//                startTime.setSecond(snapshotReader.readInt());
//                targetTime.setHour(snapshotReader.readInt());
//                targetTime.setMinute(snapshotReader.readInt());
//                targetTime.setSecond(snapshotReader.readInt());
//                currentTimeMills = System.currentTimeMillis();
//                timerTask = timerThread.new TimerTask(currentTimeMills / 1000 * 1000,
//                        1, TimeUnit.SECONDS, this::timerPausedTaskAction);
//                timerTask.setLoopTask(true, -1);
//                logger.logDebug("Timer开始转暂停状态注册任务");
//                timerThread.registerTask(timerTask);
//                if(logger.debugEnabled()) {
//                    logger.logDebug("current=" + currentTimeMills + ",target=" + timerTask.getTargetTimeMills());
//                    logger.logDebug("Timer从快照恢复到暂停状态");
//                }
//        }
//    }
//
//
//    private void requireStatus(TimerStatus... statuses) {
//        boolean permitStatus = false;
//        for(TimerStatus status: statuses) {
//            if(status == this.status) {
//                permitStatus = true;
//            }
//        }
//        Assert.isTrue(permitStatus, "不允许的操作，当前状态" + this.status + " 允许的状态" + Arrays.toString(statuses));
//    }
//
//    public TimerStatus getStatus() {
//        return status;
//    }
//
//    public boolean isRestoreCountingDone() {
//        return restoreCountingDone;
//    }
//
//    /**
//     * 获取预设计时时间，前端调用
//     * @return
//     */
//    public Time getResetTime() {
//        return resetTime;
//    }
//
//    public Date getStartDate() {
//        return startDate;
//    }
//
//    /**
//     * 获取正在计时的剩余时间，前端调用
//     * @return
//     */
//    public Time getCountingTime() {
//        return countingTime;
//    }
//
//    /**
//     * 获取计时截止时间，前端调用
//     * @return
//     */
//    public Time getTargetTime() {
//        if(status != TimerStatus.COUNTING && status != TimerStatus.PAUSED){
//            return null;
//        }
//        return targetTime;
//    }
//
//    /**
//     * 获取计时的开始时间，前端调用
//     * @return
//     */
//    public Time getStartTime() {
//        if(status != TimerStatus.COUNTING && status != TimerStatus.PAUSED){
//            return null;
//        }
//        return startTime;
//    }
//
//    /**
//     * 计时1秒，计时线程调用
//     */
//    public void count1s() {
//        requireStatus(TimerStatus.COUNTING);
//        Assert.isTrue(!(countingTime.getHour() == 0 && countingTime.getMinute() == 0 && countingTime.getSecond() == 0), "Timer无法再减！");
//        count1sInternal();
//        timerUpdated();
//        if(countingTime.getSecond() == 0 && countingTime.getMinute() == 0 && countingTime.getHour() == 0) {
//            this.status = TimerStatus.STOPPED;
//            this.startTime.setAllField(0,0,0);
//            this.countingTime.setAllField(0,0,0);
//            this.targetTime.setAllField(0,0,0);
//            timerStopped();
//        }
//    }
//
//    private void count1sInternal() {
//        countingTime.minusSecond();
//    }
//
//    /**
//     * 注册TimerListener，前端调用
//     * @param timerListener
//     */
//    public void registerTimerListener(TimerListener timerListener) {
//        Assert.notNull(timerListener);
//        this.timerListenerList.add(timerListener);
//    }
//
//    private void timerStarted() {
//        for(TimerListener timerListener: timerListenerList) {
//            timerListener.timerStarted();
//        }
//    }
//
//    private void timerPaused() {
//        for(TimerListener timerListener: timerListenerList) {
//            timerListener.timerPaused();
//        }
//    }
//
//    private void timerStopped() {
//        for(TimerListener timerListener: timerListenerList) {
//            timerListener.timerStopped();
//        }
//    }
//
//    private void timerStoppedByUser() {
//        for(TimerListener timerListener: timerListenerList) {
//            timerListener.timerStoppedByUser();
//        }
//    }
//
//    private void timerUpdated() {
//        for(TimerListener timerListener: timerListenerList) {
//            timerListener.timerUpdated();
//        }
//    }
//
//    private void timerReset() {
//        for(TimerListener timerListener: timerListenerList) {
//            timerListener.timerReset();
//        }
//    }
//
//    public static void main(String[] args) {
//        Timer timer = new Timer();
//        Time time1 = DateTimeUtil.getCurrentTime(), time2 = DateTimeUtil.getCurrentTimePlus(0,1,0);
//        System.out.println(time1);
//        System.out.println(time2);
//        Date date = DateTimeUtil.getCurrentDate();
//        System.out.println(date);
//    }
//
//}
