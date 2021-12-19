package com.adam.swing_project.library.timer.newcode;

import com.adam.swing_project.library.assertion.Assert;
import com.adam.swing_project.library.datetime.Time;
import com.adam.swing_project.library.logger.Logger;
import com.adam.swing_project.library.runtime.ManagedShutdownHook;
import com.adam.swing_project.library.runtime.PriorityShutdownHook;
import com.adam.swing_project.library.util.DateTimeUtil;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Timer {

    private static final NewTimerThread TIMER_THREAD = new NewTimerThread();

    private final Time countingTime, resetTime;
    private final NewTimerThread timerThread;
    private final Logger logger = Logger.createLogger(this);
    private String timerName;
    private TimerStatus status;
    private NewTimerThread.FixedDelayTimerTask timerTask;
    private final List<CountingListener> countingListenerList = new LinkedList<>();
    private final List<StateChangeListener> stateChangeListenerList = new LinkedList<>();

    static {
        TIMER_THREAD.start();
        PriorityShutdownHook priorityShutdownHook = new PriorityShutdownHook();
        priorityShutdownHook.setName("Timers' common thread terminate");
        priorityShutdownHook.setRunnable(TIMER_THREAD::terminate);
        ManagedShutdownHook.getInstance().registerShutdownHook(priorityShutdownHook);
    }

    enum TimerStatus {
        INITIALIZED, READY, RUNNING, PAUSED, STOPPED
    }

    interface CountingListener {
        void countingUpdated(Time countingTime);
    }

    interface StateChangeListener {
        void stateChanged(TimerStatus oldStatus, TimerStatus newStatus);
    }

    public Timer(String timerName) {
        this.timerName = timerName;
        this.countingTime = new Time(0,0,0);
        this.resetTime = new Time(0,0,0);
        this.timerThread = TIMER_THREAD;
        this.status = TimerStatus.INITIALIZED;
    }

    public void start() {
        requireStatus(TimerStatus.READY, TimerStatus.STOPPED, TimerStatus.PAUSED);
        int startDelayDuration;
        TimeUnit startDelayUnit;
        int actionTimeLimit;
        if(status == TimerStatus.READY || status == TimerStatus.STOPPED) {
            this.countingTime.copyFrom(this.resetTime);
            fireCountingUpdated();
            startDelayDuration = 1;
            startDelayUnit = TimeUnit.SECONDS;
            actionTimeLimit = (int) DateTimeUtil.translateTimeToSeconds(resetTime);
        } else {
            actionTimeLimit = timerTask.getActionTimeLimit() - timerTask.getLastActionTime();
            if(timerTask.getLastActionTime() > 0) {
                startDelayDuration = (int) (1000 - (timerTask.getCanceledMills() - timerTask.startActionMills()) % 1000);
            } else {
                startDelayDuration = (int) (timerTask.startActionMills() - timerTask.getCanceledMills());
                if(startDelayDuration < 0) {
                    logger.logWarning("startDelayDuration " + startDelayDuration);
                    startDelayDuration = 0;
                }
            }
            startDelayUnit = TimeUnit.MILLISECONDS;
        }
        this.timerTask = timerThread.new FixedDelayTimerTask(actionTimeLimit, startDelayDuration, startDelayUnit, 1, TimeUnit.SECONDS);
        NewTimerThread.TimerTaskAction countingTimeAction = () -> {
            countingTime.minusSecond();
            fireCountingUpdated();
            logger.logDebug("Timer '" + timerName + "' : " + DateTimeUtil.wrapTimeHourToSecond(countingTime) + " at " + System.currentTimeMillis());
            if (countingTime.getHour() == 0 && countingTime.getMinute() == 0 && countingTime.getSecond() == 0) {
//                timerThread.cancelTask(timerTask);  //没必要，执行次数到上限自动退出队列
                logger.logDebug("Timer '" + timerName + "' stopped at " + System.currentTimeMillis());
                changeStatus(TimerStatus.STOPPED);
            }
        };
        timerTask.setTaskName("countingTimeAction for Timer '" + timerName + "'");
        timerTask.setAction(countingTimeAction);
        timerThread.addTask(timerTask);
        logger.logDebug("Timer '" + timerName + "' started at " + System.currentTimeMillis());
        changeStatus(TimerStatus.RUNNING);
    }

    public void pause() {
        requireStatus(TimerStatus.RUNNING);
        timerThread.cancelTask(this.timerTask);
        logger.logDebug("Timer '" + timerName + "' paused at " + System.currentTimeMillis());
        changeStatus(TimerStatus.PAUSED);
    }

    public void stop() {
        requireStatus(TimerStatus.RUNNING, TimerStatus.PAUSED);
        timerTask.setAction(()->{});
        timerThread.cancelTask(timerTask);
        this.countingTime.setAllField(0,0,0);
        this.timerTask = null;
        logger.logDebug("Timer '" + timerName + "' stopped at " + System.currentTimeMillis());
        changeStatus(TimerStatus.STOPPED);
    }

    public void reset(Time time) {
        requireStatus(TimerStatus.INITIALIZED, TimerStatus.READY, TimerStatus.STOPPED);
        Assert.notNull(time);
        Assert.isTrue(time.getHour() >= 0 && time.getMinute() >= 0 && time.getSecond() >= 0, "Invalid param");
        this.resetTime.copyFrom(time);
        this.countingTime.setAllField(0,0,0);
        if(time.getHour() == 0 && time.getMinute() == 0 && time.getSecond() == 0) {
            changeStatus(TimerStatus.INITIALIZED);
        } else {
            changeStatus(TimerStatus.READY);
        }
        logger.logDebug("Timer '" + timerName + "' reset to " + DateTimeUtil.wrapTimeHourToSecond(time));
    }

    public void addCountingListener(CountingListener countingListener) {
        this.countingListenerList.add(countingListener);
    }

    public void addStateChangeListener(StateChangeListener stateChangeListener) {
        this.stateChangeListenerList.add(stateChangeListener);
    }

    private void requireStatus(TimerStatus... statuses) {
        Assert.notNull(statuses);
        boolean found = false;
        for(int i=0;i< statuses.length;i++) {
            if(this.status == statuses[i]) {
                found = true;
            }
        }
        Assert.isTrue(found, TimerException.class, "Invalid timer status");
    }

    private void changeStatus(TimerStatus newStatus) {
        TimerStatus oldStatus = this.status;
        if(oldStatus != newStatus) {
            this.status = newStatus;
            fireStateChanged(oldStatus, newStatus);
        }
    }

    private void fireStateChanged(TimerStatus oldStatus, TimerStatus newStatus) {
        for(StateChangeListener stateChangeListener: stateChangeListenerList) {
            stateChangeListener.stateChanged(oldStatus, newStatus);
        }
    }

    private void fireCountingUpdated() {
        Time countingTimeCopy = new Time(0,0,0);
        countingTimeCopy.copyFrom(countingTime);
        for(CountingListener countingListener: countingListenerList) {
            countingListener.countingUpdated(countingTimeCopy);
        }
    }

    public static void main(String[] args) {
        Timer timer = new Timer("MyTimer");
        timer.reset(new Time(0,0,10));

        JFrame jFrame = new JFrame();
        Container contentPane = jFrame.getContentPane();
        JPanel jPanel = new JPanel(new GridLayout(1,3));
        JButton button1 = new JButton("Start"),
                button2 = new JButton("Pause"),
                button3 = new JButton("Stop");
        button2.setEnabled(false);
        button3.setEnabled(false);
        JLabel jLabel = new JLabel(DateTimeUtil.wrapTimeHourToSecond(timer.countingTime));
        JLabel monitorLabel = new JLabel("counted: 0");
        jLabel.setFont(jLabel.getFont().deriveFont(20).deriveFont(Font.BOLD));
        jLabel.setHorizontalAlignment(JLabel.CENTER);
        monitorLabel.setFont(monitorLabel.getFont().deriveFont(20));
        monitorLabel.setHorizontalAlignment(JLabel.CENTER);
        button1.addActionListener(e -> timer.start());
        button2.addActionListener(e -> timer.pause());
        button3.addActionListener(e -> timer.stop());

        long[] mills = new long[2];
        timer.addCountingListener(countingTime -> jLabel.setText(DateTimeUtil.wrapTimeHourToSecond(countingTime)));
        timer.addStateChangeListener(((oldStatus, newStatus) -> {
            switch (newStatus) {
                case RUNNING:
                    button1.setEnabled(false);
                    button2.setEnabled(true);
                    button3.setEnabled(true);
                    mills[0] = System.currentTimeMillis();
                    if(oldStatus == TimerStatus.READY || oldStatus == TimerStatus.STOPPED) {
                        mills[1] = 0;
                        monitorLabel.setText("counted: " + mills[1]);
                    }
                    break;
                case PAUSED:
                    button1.setEnabled(true);
                    button2.setEnabled(false);
                    button3.setEnabled(true);
                    mills[1] += (System.currentTimeMillis() - mills[0]);
                    monitorLabel.setText("counted: " + mills[1]);
                    break;
                case STOPPED:
                    button1.setEnabled(true);
                    button2.setEnabled(false);
                    button3.setEnabled(false);
                    mills[1] += (System.currentTimeMillis() - mills[0]);
                    monitorLabel.setText("counted: " + mills[1]);
                    break;
            }
        }));

        jPanel.add(button1);
        jPanel.add(button2);
        jPanel.add(button3);
        contentPane.add(monitorLabel, BorderLayout.NORTH);
        contentPane.add(jLabel, BorderLayout.CENTER);
        contentPane.add(jPanel, BorderLayout.SOUTH);

        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.pack();
        jFrame.setVisible(true);
    }

}
