package com.adam.swing_project.library.timer.newcode;

import com.adam.swing_project.library.assertion.Assert;
import com.adam.swing_project.library.datetime.Time;
import com.adam.swing_project.library.logger.Logger;
import com.adam.swing_project.library.runtime.ManagedShutdownHook;
import com.adam.swing_project.library.runtime.PriorityShutdownHook;
import com.adam.swing_project.library.snapshot.SnapshotManager;
import com.adam.swing_project.library.snapshot.SnapshotReader;
import com.adam.swing_project.library.snapshot.SnapshotWriter;
import com.adam.swing_project.library.snapshot.Snapshotable;
import com.adam.swing_project.library.util.DateTimeUtil;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Timer implements Snapshotable {

    private static final NewTimerThread TIMER_THREAD = new NewTimerThread();

    static {
        TIMER_THREAD.start();
        PriorityShutdownHook priorityShutdownHook = new PriorityShutdownHook();
        priorityShutdownHook.setName("Timers' common thread terminate");
        priorityShutdownHook.setRunnable(TIMER_THREAD::terminate);
        ManagedShutdownHook.getInstance().registerShutdownHook(priorityShutdownHook);
    }

    public enum TimerStatus {
        INITIALIZED, READY, RUNNING, PAUSED, STOPPED, TIME_UP
    }

    public interface CountingListener {
        void countingUpdated(Time countingTime);
    }

    public interface StateChangeListener {
        void stateChanged(TimerStatus oldStatus, TimerStatus newStatus);
    }

    private final Time countingTime = new Time(0,0,0), resetTime = new Time(0,0,0);
    private final NewTimerThread timerThread;
    private final Logger logger = Logger.createLogger(this);
    private String timerName;
    private volatile TimerStatus status;
    private NewTimerThread.FixedDelayTimerTask timerTask;
    private final int[] pausedTempArray = new int[2];
    private final List<CountingListener> countingListenerList = new LinkedList<>();
    private final List<StateChangeListener> stateChangeListenerList = new LinkedList<>();
    NewTimerThread.TimerTaskAction countingTimeAction = () -> {
        countingTime.minusSecond();
        fireCountingUpdated();
        logger.logDebug("Timer '" + timerName + "' : " + DateTimeUtil.wrapTimeHourToSecond(countingTime) + " at " + System.currentTimeMillis());
        if (countingTime.getHour() == 0 && countingTime.getMinute() == 0 && countingTime.getSecond() == 0) {
            logger.logDebug("Timer '" + timerName + "' stopped at " + System.currentTimeMillis());
            changeStatus(TimerStatus.TIME_UP);
        }
    };


    public Timer() {
        this("Unnamed timer");
    }

    public Timer(String timerName) {
        this.timerName = timerName;
        this.timerThread = TIMER_THREAD;
        this.status = TimerStatus.INITIALIZED;
        SnapshotManager.getInstance().registerSnapshotable(this);
    }


    @Override
    public byte[] writeToSnapshot() {
        SnapshotWriter writer = SnapshotWriter.writer();
        writer.writeString(timerName);
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
    }

    public void start() {
        requireStatus(TimerStatus.READY, TimerStatus.STOPPED, TimerStatus.PAUSED, TimerStatus.TIME_UP);
        int startDelayDuration;
        TimeUnit startDelayUnit;
        int actionTimeLimit;
        if(status == TimerStatus.READY || status == TimerStatus.STOPPED || status == TimerStatus.TIME_UP) {
            this.countingTime.copyFrom(this.resetTime);
            fireCountingUpdated();
            startDelayDuration = 1;
            startDelayUnit = TimeUnit.SECONDS;
            actionTimeLimit = (int) DateTimeUtil.translateTimeToSeconds(resetTime);
        } else {
            actionTimeLimit = pausedTempArray[0];
            startDelayDuration = pausedTempArray[1];
            startDelayUnit = TimeUnit.MILLISECONDS;
        }
        startInternal(actionTimeLimit, startDelayDuration, startDelayUnit);
        logger.logDebug("Timer '" + timerName + "' started at " + System.currentTimeMillis());
        changeStatus(TimerStatus.RUNNING);
    }

    private void startInternal(int actionTimeLimit, int startDelayDuration, TimeUnit startDelayUnit) {
        this.timerTask = timerThread.new FixedDelayTimerTask(actionTimeLimit, startDelayDuration, startDelayUnit, 1, TimeUnit.SECONDS);
        timerTask.setTaskName("Timer '" + timerName + "' countingTimeAction");
        timerTask.setAction(countingTimeAction);
        timerThread.addTask(timerTask);
    }

    private int calcActionTimeLeft(NewTimerThread.TimerTask timerTask) {
        return timerTask.getActionTimeLimit() - timerTask.getLastActionTime();
    }

    private int calcStartDelayDuration(NewTimerThread.FixedDelayTimerTask timerTask) {
        return calcStartDelayDuration(timerTask, timerTask.getCanceledMills());
    }

    private int calcStartDelayDuration(NewTimerThread.FixedDelayTimerTask timerTask, long timeMills) {
        int startDelayDuration;
        if(timerTask.getLastActionTime() > 0) {
            startDelayDuration = (int) (1000 - (timeMills - timerTask.startActionMills()) % 1000);
        } else {
            startDelayDuration = (int) (timerTask.startActionMills() - timeMills);
            if(startDelayDuration < 0) {
                logger.logWarning("startDelayDuration " + startDelayDuration);
                startDelayDuration = 0;
            }
        }
        return startDelayDuration;
    }

    public void pause() {
        requireStatus(TimerStatus.RUNNING);
        timerThread.cancelTask(this.timerTask);
        pausedTempArray[0] = calcActionTimeLeft(timerTask);
        pausedTempArray[1] = calcStartDelayDuration(timerTask);
        logger.logDebug("Timer '" + timerName + "' paused at " + System.currentTimeMillis());
        changeStatus(TimerStatus.PAUSED);
    }

    public void stop() {
        requireStatus(TimerStatus.RUNNING, TimerStatus.PAUSED);
        timerTask.setAction(()->{});
        timerThread.cancelTask(timerTask);
        this.countingTime.setAllField(0,0,0);
        fireCountingUpdated();
        this.timerTask = null;
        logger.logDebug("Timer '" + timerName + "' stopped at " + System.currentTimeMillis());
        changeStatus(TimerStatus.STOPPED);
    }

    /**
     * 当TIME_UP对应的事件，例如响铃完成后，调用该方法使状态变成STOPPED。
     */
    public void timeUpClear() {
        requireStatus(TimerStatus.TIME_UP);
        changeStatus(TimerStatus.STOPPED);
    }

    public void terminate() {
        if(timerTask != null) {
            timerTask.setAction(() -> {});
            timerThread.cancelTask(timerTask);
        }
        this.countingTime.setAllField(0,0,0);
        fireCountingUpdated();
        this.timerTask = null;
        logger.logDebug("Timer '" + timerName + "' stopped at " + System.currentTimeMillis());
        changeStatus(TimerStatus.STOPPED);
        SnapshotManager.getInstance().removeSnapshotable(this);
    }

    public void reset(Time time) {
        requireStatus(TimerStatus.INITIALIZED, TimerStatus.READY, TimerStatus.STOPPED, TimerStatus.TIME_UP);
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

    public Time getCountingTime() {
        return countingTime;
    }

    public Time getResetTime() {
        return resetTime;
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
        Assert.isTrue(found, TimerException.class, "Invalid timer status: actual=" + status + ",required=" + Arrays.toString(statuses));
    }

    private void changeStatus(TimerStatus newStatus) {
        TimerStatus oldStatus = this.status;
        this.status = newStatus;
        fireStateChanged(oldStatus, newStatus);
    }

    private void fireStateChanged(TimerStatus oldStatus, TimerStatus newStatus) {
        for(StateChangeListener stateChangeListener: stateChangeListenerList) {
            stateChangeListener.stateChanged(oldStatus, newStatus);
        }
    }

    public void fireStateChanged() {
        fireStateChanged(null, status);
    }

    public void fireCountingUpdated() {
        Time countingTimeCopy = new Time(0,0,0);
        countingTimeCopy.copyFrom(countingTime);
        for(CountingListener countingListener: countingListenerList) {
            countingListener.countingUpdated(countingTimeCopy);
        }
    }

    public static void main(String[] args) {
        Timer timer1 = new Timer("Timer1"),
                timer2 = new Timer("Timer2"),
                timer3 = new Timer("Timer3");
        timer1.reset(new Time(0,0,30));
        timer2.reset(new Time(0,0,30));
        timer3.reset(new Time(0,0,30));
        timer1.start();
        timer2.start();
        timer3.start();
        /*
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
         */
    }

}
