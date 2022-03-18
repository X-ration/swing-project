package com.adam.swing_project.library.timer;

import com.adam.swing_project.library.assertion.Assert;
import com.adam.swing_project.library.logger.ConsoleLogger;
import com.adam.swing_project.library.logger.Logger;
import com.adam.swing_project.library.logger.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TimerThread extends Thread{

    protected abstract class TimerTask implements Comparable<TimerTask> {
        protected long startMills, lastActionMills, canceledMills;
        /**
         * 任务执行次数，-1表示永久循环
         */
        protected int actionTimeLimit, lastActionTime;
        protected TimerTaskAction action;
        protected String taskName;

        protected TimerTask(long startMills, int actionTimeLimit, String taskName) {
            this.startMills = startMills;
            this.actionTimeLimit = actionTimeLimit;
            this.taskName = taskName;
            this.lastActionMills = 0;
            this.lastActionTime = 0;
        }

        protected TimerTask(long startMills, int actionTimeLimit) {
            this(startMills, actionTimeLimit, "Unnamed timer task");
        }

        protected abstract long delayMills();
        protected void action() {
            Assert.notNull(action);
            action.action();
        }

        public long getStartMills() {
            return startMills;
        }

        public long getCanceledMills() {
            return canceledMills;
        }

        public int getActionTimeLimit() {
            return actionTimeLimit;
        }

        public int getLastActionTime() {
            return lastActionTime;
        }

        protected void setAction(TimerTaskAction action) {
            this.action = action;
        }

        protected void setTaskName(String taskName) {
            this.taskName = taskName;
        }

        protected long nextActionMills() {
            if(lastActionMills == 0) {
                return startMills;
            } else {
                return lastActionMills + delayMills();
            }
        }

        @Override
        public int compareTo(TimerTask o) {
            return Long.compare(this.nextActionMills(), o.nextActionMills());
        }
    }

    public interface TimerTaskAction {
        void action();
    }

    public class FixedDelayTimerTask extends TimerTask {
        protected final long startDelayDuration, delayDuration;
        protected final TimeUnit startDelayUnit, delayUnit;

        public FixedDelayTimerTask(int actionTimeLimit, long startDelayDuration, TimeUnit startDelayUnit, long delayDuration, TimeUnit delayUnit, TimerTaskAction action) {
            super(System.currentTimeMillis(), actionTimeLimit);
            Assert.isTrue(actionTimeLimit >= -1 && startDelayDuration >= 0 && startDelayUnit != null && delayDuration > 0 && delayUnit != null, "Invalid param");
            this.startDelayDuration = startDelayDuration;
            this.startDelayUnit = startDelayUnit;
            this.delayDuration = delayDuration;
            this.delayUnit = delayUnit;
            this.action = action;
        }

        public FixedDelayTimerTask(int actionTimeLimit, long delayDuration, TimeUnit delayUnit) {
            this(actionTimeLimit, 0, TimeUnit.SECONDS, delayDuration, delayUnit);
        }

        public FixedDelayTimerTask(int actionTimeLimit, long startDelayDuration, TimeUnit startDelayUnit, long delayDuration, TimeUnit delayUnit) {
            this(actionTimeLimit, startDelayDuration, startDelayUnit, delayDuration, delayUnit, null);
        }

        @Override
        protected long delayMills() {
            return delayUnit.toMillis(delayDuration);
        }

        public long startActionMills() {
            return startMills + startDelayUnit.toMillis(startDelayDuration);
        }

        @Override
        protected long nextActionMills() {
            if(lastActionMills == 0) {
                return startActionMills();
            } else {
                return lastActionMills + delayMills();
            }
        }
    }

    /**
     * 计时任务队列
     */
    private final List<TimerTask> timerTaskList = new LinkedList<>();
    private final Logger logger = LoggerFactory.getLogger(this);
    private volatile boolean terminateSign = false;
    private final Object notEmptyLock = new Object(), modifyingLock = new Object();
    private final ThreadPoolExecutor workThreadPool = new ThreadPoolExecutor(1,1,
            0,TimeUnit.SECONDS,new LinkedBlockingQueue<>());

    @Override
    public void run() {
        logger.logInfo("TimerThread started");

        try {
            while (true) {
                while (!terminateSign && timerTaskList.isEmpty()) {
                    synchronized (notEmptyLock) {
                        try {
                            notEmptyLock.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
//                logger.logDebug("Jump out of wait, terminateSign=" + terminateSign);
                if (terminateSign) {
                    break;
                }
                synchronized (modifyingLock) {
                    Collections.sort(timerTaskList);
                }
//                StringBuilder sb = new StringBuilder();
//                for(TimerTask timerTask: timerTaskList) {
//                    sb.append("Task '").append(timerTask.taskName).append("' nextActionMills=").append(timerTask.nextActionMills()).append(" ");
//                }
//                System.out.println(sb.toString());

                TimerTask timerTask = timerTaskList.get(0);
                long currentMills = System.currentTimeMillis();
                long sleepMills = timerTask.nextActionMills() - currentMills;
                if(sleepInternal(sleepMills)) {
                    continue;
                }
                workThreadPool.submit(timerTask::action);
                timerTask.lastActionMills = currentMills + sleepMills;
                timerTask.lastActionTime++;

                if (timerTask.lastActionTime == timerTask.actionTimeLimit) {
                    synchronized (modifyingLock) {
                        timerTaskList.remove(timerTask);
                    }
                }
            }
            workThreadPool.shutdownNow();
        } catch (Exception e) {
            e.printStackTrace();
        }

        logger.logInfo("TimerThread stopped");
    }

    public void addTask(TimerTask timerTask) {
        logger.logDebug(timerTask.taskName + " added ");
        synchronized (modifyingLock) {
            this.timerTaskList.add(timerTask);
        }
        this.interrupt();
    }

    public void cancelTask(TimerTask timerTask) {
        if(!timerTaskList.contains(timerTask)) {
            logger.logWarning("TimerTask " + timerTask.taskName + " already stopped");
        } else {
            synchronized (modifyingLock) {
                this.timerTaskList.remove(timerTask);
                this.interrupt();
            }
            timerTask.canceledMills = System.currentTimeMillis();
        }
    }

    public void terminate() {
        terminateSign = true;
        logger.logDebug("sets terminate sign");
        this.interrupt();
    }

    /**
     * 休眠<mills>毫秒数
     * @param mills
     * @return 中断标志
     */
    private boolean sleepInternal(long mills) {
        logger.logDebug("sleepInternal mills=" + mills);
        if(mills < 0) {
            logger.logWarning("sleepInternal param 'mills' = " + mills);
            return false;
        }
        try {
            Thread.sleep(mills);
            return false;
        } catch (InterruptedException e) {
            return true;
        }
    }

    public static void main(String[] args) {
        TimerThread timerThread = new TimerThread();
        timerThread.start();
        Random random = new Random();
        int i=0;
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            timerThread.terminate();
            try {
                timerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));
        while(true) {
            try {
                Thread.sleep((long) (random.nextFloat() * 1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            i++;
            TimerTask task = timerThread.new FixedDelayTimerTask(3, 1, TimeUnit.SECONDS);
            task.taskName = "Task " + i;
            task.setAction(()-> System.out.println(task.taskName + " action " + System.currentTimeMillis()));
            timerThread.addTask(task);
        }
    }

}
