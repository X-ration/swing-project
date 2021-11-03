package com.adam.swing_project.timer.newcode;

import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 计时器工作线程
 */
public class TimerThread extends Thread {
    private final Object workingLock = new Object();
    private final List<TimerTask> timerTaskList = Collections.synchronizedList(new LinkedList<>());
    private final Map<TimerTask, MeasurementReport<Long>> timerTaskMeasurementReportMap = new HashMap<>();
    private boolean isTerminating = false;
    private final Logger logger = Logger.createLogger(this);
    private final ThreadPoolExecutor workerThreadPool;

    TimerThread(ThreadPoolExecutor threadPoolExecutor) {
        workerThreadPool = threadPoolExecutor;
    }

    public class TimerTask implements Comparable<TimerTask> {
        private final long startTimeMills;
        private long updatingStartTimeMills, countTime;
        private TimeUnit timeUnit;
        private TimerTaskAction action;
        //循环执行的任务
        private boolean isLoopTask;
        private long targetTimeMills, round = 1;

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
            return Long.compare(getTargetTimeMills(), o.getTargetTimeMills());
        }

        private long getTargetTimeMills ()  {
            return updatingStartTimeMills + timeUnit.toMillis(countTime);
        }

        private long getActuralTargetTimeMills() {
            return startTimeMills + round * timeUnit.toMillis(countTime);
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
                        logger.log("线程等待时被中断");
                    }
                }
                if (isTerminating) {
                    break;
                }

                boolean isInterrupted = false;
                while (!timerTaskList.isEmpty()) {
                    long currentTimeMills = System.currentTimeMillis();
                    logger.log("currentTimeMills=" + currentTimeMills);
                    for (int i=0;i<timerTaskList.size();i++) {
                        TimerTask timerTask = timerTaskList.get(i);
                        MeasurementReport<Long> measurementReport = timerTaskMeasurementReportMap.get(timerTask);
                        if(measurementReport == null) {
                            measurementReport = createNewReport();
                            timerTaskMeasurementReportMap.put(timerTask, measurementReport);
                        }
                        long timeMillsToSleep = timerTask.getTargetTimeMills() - currentTimeMills;
                        logger.log(timerTask + "计划休眠" + timeMillsToSleep + "毫秒");
                        isInterrupted = sleepInternal(timeMillsToSleep);
                        if (isInterrupted) {
                            logger.log("线程工作时被中断");
                            break;
                        }
                        logger.log(timerTask + "从休眠中醒来");
                        currentTimeMills += timeMillsToSleep;
                        logger.log("currentTimeMills=" + currentTimeMills);
                        long currentTimeMillsForMeasurement = System.currentTimeMillis();
                        measurementReport.addMeasurementObject(new MeasurementObject<>(timerTask.getActuralTargetTimeMills() , currentTimeMillsForMeasurement));
//                        workerThreadPool.submit(()->{
                        //todo 误差问题
                            timerTask.action.action();
                            logger.log(timerTask + "执行了action");
//                        });
                        if (!timerTask.isLoopTask || currentTimeMills >= timerTask.targetTimeMills) {
                            timerTaskList.remove(i--);
                            logger.logInfo(timerTask + "执行完毕，退出任务队列，总误差=" + (timerTask.targetTimeMills - currentTimeMillsForMeasurement)
                                    + " " + measurementReport.getBriefDiffReport());
                        } else {
                            timerTask.updatingStartTimeMills = currentTimeMills;
                            timerTask.round++;
                        }
                        if(Thread.interrupted()) {
                            logger.log("检测到中断");
                            break;
                        }
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
        synchronized (workingLock) {
            workingLock.notify();
        }
    }

    private MeasurementReport<Long> createNewReport() {
        return new MeasurementReport<>() {
            @Override
            public Long computeAvg(List<Long> longs) {
                long summary = 0;
                for(long longV: longs) {
                    summary+=longV;
                }
                return summary/longs.size();
            }
            @Override
            public Long computeDiff(Long t1, Long t2) {
                return t1-t2;
            }
        };
    }

    /**
     * 注册计时任务，并按照升序放到合适的位置
     * @param timerTask
     */
    public void registerTask(TimerTask timerTask) {
        int i=0;
        for(;i<timerTaskList.size();i++) {
            TimerTask taskInQueue = timerTaskList.get(i);
            if(taskInQueue.compareTo(timerTask) > 0) {
                break;
            }
        }
        timerTaskList.add(i, timerTask);
        logger.logInfo("注册了计时任务" + timerTask + (timerTask.isLoopTask ? "循环任务至" + timerTask.targetTimeMills : ""));
        this.interrupt();
    }

    /**
     * 通知计时线程一个任务已经发生变化
     * @param timerTask
     */
    public void taskChanged(TimerTask timerTask) {
        if(!timerTaskList.contains(timerTask)) {
            logger.logWarning("计时任务" + timerTask + "已经执行完毕了");
            return;
        }
        this.interrupt();
    }

    /**
     * 移出一个任务
     * @param timerTask
     */
    public void removeTask(TimerTask timerTask) {
        if(!timerTaskList.contains(timerTask)) {
            logger.logWarning("计时任务" + timerTask + "已经执行完毕了");
            return;
        }
        timerTaskList.remove(timerTask);
        this.interrupt();
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
