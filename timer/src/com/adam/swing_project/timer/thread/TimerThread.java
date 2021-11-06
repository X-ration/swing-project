package com.adam.swing_project.timer.thread;

import com.adam.swing_project.timer.helper.Logger;
import com.adam.swing_project.timer.helper.MeasurementObject;
import com.adam.swing_project.timer.helper.MeasurementReport;
import com.adam.swing_project.timer.helper.MergingMeasurementReport;

import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 计时器工作线程
 */
public class TimerThread extends Thread {
    private final Object workingLock = new Object();
//    private final List<TimerTask> timerTaskList = Collections.synchronizedList(new LinkedList<>());
    private final List<WrappedTimerTask> wrappedTimerTaskList = Collections.synchronizedList(new LinkedList<>());
//    private final Map<TimerTask, MeasurementReport<Long>> timerTaskMeasurementReportMap = new HashMap<>();
    private boolean isTerminating = false;
    private boolean isMeasurementEnabled = true;
    private final Logger logger = Logger.createLogger(this);
    private final ThreadPoolExecutor workerThreadPool;

    TimerThread(ThreadPoolExecutor threadPoolExecutor) {
        workerThreadPool = threadPoolExecutor;
    }

    private class WrappedTimerTask {
        TimerTask timerTask;
        MeasurementReport<Long> measurementReport;
        WrappedTimerTask(TimerTask timerTask) {
            this.timerTask = timerTask;
        }
    }
    public class TimerTask implements Comparable<TimerTask> {
        private final long startTimeMills;
        private long updatingStartTimeMills, countTime;
        private TimeUnit timeUnit;
        private TimerTaskAction action;
        //循环执行的任务
        private boolean isLoopTask;
        private long targetTimeMills, round = 1;
        //任务结束时的时间戳
        private long exitTimeMills;

        public TimerTask(long startTimeMills, long countTime, TimeUnit timeUnit, TimerTaskAction action) {
            this.startTimeMills = startTimeMills;
            this.updatingStartTimeMills = startTimeMills;
            this.countTime = countTime;
            this.timeUnit = timeUnit;
            this.action = action;
        }
        public TimerTask(long countTime, TimeUnit timeUnit, TimerTaskAction action) {
            this(System.currentTimeMillis(), countTime, timeUnit, action);
        }

        /**
         * 设置为循环执行任务
         * @param loopTask true为循环任务
         * @param targetTimeMills 目标循环的时间点，-1为永久循环
         */
        public void setLoopTask(boolean loopTask, long targetTimeMills) {
            this.isLoopTask = loopTask;
            this.targetTimeMills = targetTimeMills;
        }

        public void setLoopTask(boolean loopTask, long countTime, TimeUnit timeUnit) {
            this.isLoopTask = loopTask;
            this.targetTimeMills = this.startTimeMills + timeUnit.toMillis(countTime);
        }

        @Override
        public int compareTo(TimerTask o) {
            return Long.compare(getNextActionTimeMills(), o.getNextActionTimeMills());
        }

        public long getNextActionTimeMills()  {
            return updatingStartTimeMills + timeUnit.toMillis(countTime);
        }

        public long getTargetTimeMills() {
            return targetTimeMills;
        }

        private long getActualNextActionTimeMills() {
            return startTimeMills + round * timeUnit.toMillis(countTime);
        }

        public long getExitTimeMills() {
            return exitTimeMills;
        }

        public long getStartTimeMills() {
            return startTimeMills;
        }
    }

    public interface TimerTaskAction {
        void action();
    }

    /**
     * run方法
     */
    @Override
    public void run() {
        logger.logInfo("(New)TimerThread started");
        try {
            while (!isTerminating) {
                synchronized (workingLock) {
                    try {
                        workingLock.wait();
                    } catch (InterruptedException e) {
                        logger.logDebug("线程等待时被中断");
                    }
                }
                if (isTerminating) {
                    break;
                }

                boolean isInterrupted = false;
                while (!wrappedTimerTaskList.isEmpty()) {
                    if(logger.debugEnabled()) {
                        debugDumpTaskList();
                    }
                    long currentTimeMills = System.currentTimeMillis();
                    if(logger.debugEnabled()) {
                        logger.logDebug("currentTimeMills=" + currentTimeMills);
                    }
                    WrappedTimerTask wrappedTimerTask = wrappedTimerTaskList.get(0);
                    TimerTask timerTask = wrappedTimerTask.timerTask;
                    MeasurementReport<Long> measurementReport = null;
                    if(isMeasurementEnabled) {
                        measurementReport = wrappedTimerTask.measurementReport;
                        if (measurementReport == null) {
                            measurementReport = createNewReport();
                            wrappedTimerTask.measurementReport = measurementReport;
                        }
                    }
                    long timeMillsToSleep = timerTask.getNextActionTimeMills() - currentTimeMills;
                    if(logger.debugEnabled()) {
                        logger.logDebug(timerTask + "计划休眠" + timeMillsToSleep + "毫秒");
                    }
                    isInterrupted = sleepInternal(timeMillsToSleep);
                    if(isTerminating) {
                        break;
                    }
                    if (isInterrupted) {
                        logger.logDebug("线程工作时被中断");
                        continue;
                    }
                    currentTimeMills += timeMillsToSleep;
                    if(logger.debugEnabled()) {
                        logger.logDebug(timerTask + "从休眠中醒来");
                        logger.logDebug("currentTimeMills=" + currentTimeMills);
                    }
                    long currentTimeMillsForMeasurement = Long.MAX_VALUE;
                    if(isMeasurementEnabled) {
                        currentTimeMillsForMeasurement = System.currentTimeMillis();
                        measurementReport.addMeasurementObject(new MeasurementObject<>(timerTask.getActualNextActionTimeMills(), currentTimeMillsForMeasurement));
                    }
                    workerThreadPool.submit(()->{
                        timerTask.action.action();
                        if(logger.debugEnabled()) {
                            logger.logDebug(timerTask + "执行了action");
                        }
                    });
                    if (!timerTask.isLoopTask || (timerTask.targetTimeMills != -1 && currentTimeMills >= timerTask.targetTimeMills)) {
                        removeTask(timerTask);
                        StringBuilder exitLog = new StringBuilder();
                        exitLog.append(timerTask).append("执行完毕，退出任务队列");
                        if(isMeasurementEnabled) {
                            exitLog.append(",总误差=").append(timerTask.targetTimeMills - currentTimeMillsForMeasurement);
                            exitLog.append(";").append(measurementReport.getBriefDiffReport());
                        }
                        logger.logInfo(exitLog.toString());
                    } else {
                        timerTask.updatingStartTimeMills = currentTimeMills;
                        timerTask.round++;
                        relocateTask(timerTask);
                    }
                    if(Thread.interrupted()) {
                        logger.logDebug("检测到中断");
                        if(isTerminating)
                            break;
                    }
                }
            }
        } catch (Exception e) {
            logger.logException(e);
        }
        logger.logInfo("(New)TimerThread terminated");
    }

    /**
     * 终止线程
     */
    public void terminate() {
        this.isTerminating = true;
        this.interrupt();
    }

    private MeasurementReport<Long> createNewReport() {
        return new MergingMeasurementReport<>() {
            @Override
            public Long mergeAvg(Long avg, int avgCount, List<Long> longs) {
                long summary = avg * avgCount;
                for (long longV : longs) {
                    summary += longV;
                }
                return summary / (avgCount + longs.size());
            }

            @Override
            public Long computeAvg(List<Long> longs) {
                long summary = 0;
                for (long longV : longs) {
                    summary += longV;
                }
                return summary / longs.size();
            }

            @Override
            public Long minus(Long t1, Long t2) {
                return t1 - t2;
            }
        };
    }

    /**
     * 注册计时任务，并按照升序放到合适的位置
     * @param timerTask
     */
    public void registerTask(TimerTask timerTask) {
        relocateTask(timerTask);
        logger.logInfo("注册了计时任务" + timerTask + (timerTask.isLoopTask ? "循环任务至" + timerTask.targetTimeMills : ""));
        if(logger.debugEnabled()) {
            debugDumpTaskList();
        }
        this.interrupt();
    }

    public void debugDumpTaskList() {
        StringBuilder sb = new StringBuilder();
        for(WrappedTimerTask wrappedTimerTask: wrappedTimerTaskList) {
            sb.append("task[").append(wrappedTimerTask.timerTask)
                    .append("]NextAction=").append(wrappedTimerTask.timerTask.getNextActionTimeMills())
                    .append(",Target=").append(wrappedTimerTask.timerTask.getTargetTimeMills());
        }
        logger.logDebug("计时任务表：" + sb);
    }

    /**
     * 重新定位一个TimerTask到合适位置
     * @param timerTask
     */
    private void relocateTask(TimerTask timerTask) {
        WrappedTimerTask wrappedTimerTask = null;
        for(WrappedTimerTask wrappedTimerTask1: wrappedTimerTaskList) {
            if(wrappedTimerTask1.timerTask == timerTask) {
                wrappedTimerTask = wrappedTimerTask1;
                break;
            }
        }
        if(wrappedTimerTask != null) {
            wrappedTimerTaskList.remove(wrappedTimerTask);
        } else {
            wrappedTimerTask = new WrappedTimerTask(timerTask);
        }
        int i=0;
        for(;i<wrappedTimerTaskList.size();i++) {
            TimerTask taskInQueue = wrappedTimerTaskList.get(i).timerTask;
            if(taskInQueue.compareTo(wrappedTimerTask.timerTask) > 0) {
                break;
            }
        }
        wrappedTimerTaskList.add(i, wrappedTimerTask);
    }

    public boolean isMeasurementEnabled() {
        return isMeasurementEnabled;
    }

    public void setMeasurementEnabled(boolean measurementEnabled) {
        this.isMeasurementEnabled = measurementEnabled;
    }

    /**
     * 通知计时线程一个任务已经发生变化
     * @param timerTask
     */
//    public void taskChanged(TimerTask timerTask) {
//        if(!timerTaskList.contains(timerTask)) {
//            logger.logWarning("计时任务" + timerTask + "已经执行完毕了");
//            return;
//        }
//        this.interrupt();
//    }



    private void removeTask(TimerTask timerTask, long exitTimeMills) {
        int i=0;
        for(i=0;i<wrappedTimerTaskList.size();i++) {
            WrappedTimerTask wrappedTimerTaskInQueue = wrappedTimerTaskList.get(i);
            if(wrappedTimerTaskInQueue.timerTask == timerTask) {
                break;
            }
        }
        if(i == wrappedTimerTaskList.size()) {
            logger.logWarning("计时任务" + timerTask + "已经执行完毕了");
            return;
        }
        wrappedTimerTaskList.remove(i);
        if(logger.debugEnabled()) {
            logger.logInfo("移除了计时任务" + timerTask);
        }
        timerTask.exitTimeMills = exitTimeMills;
        this.interrupt();
    }

    /**
     * 移出一个任务
     * @param timerTask
     */
    public void removeTask(TimerTask timerTask) {
        removeTask(timerTask, System.currentTimeMillis());
    }

    /**
     * 睡眠指定的毫秒数，内部调用
     * @param milliseconds
     * @return 是否被中断
     */
    private boolean sleepInternal(long milliseconds) {
        if(milliseconds <= 0) {
            logger.logWarning("Warning: sleepInternal parameter '" + milliseconds + "'is negative");
            return false;
        }
        try {
            Thread.sleep(milliseconds);
            return false;
        } catch (InterruptedException e) {
            return true;
        }
    }
}
