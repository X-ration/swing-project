package com.adam.swing_project.timer.timer;

import com.adam.swing_project.library.datetime.Date;
import com.adam.swing_project.library.datetime.Time;
import com.adam.swing_project.library.logger.Logger;
import com.adam.swing_project.library.logger.LoggerFactory;
import com.adam.swing_project.library.snapshot.CustomInstantiationSnapshotable;
import com.adam.swing_project.library.snapshot.SnapshotReader;
import com.adam.swing_project.library.snapshot.SnapshotWriter;
import com.adam.swing_project.library.snapshot.Snapshotable;
import com.adam.swing_project.library.util.DateTimeUtil;
import com.adam.swing_project.timer.stat.TimerStatistic;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * 计时轮次，代表一次计时的整个过程
 * 把无关联的ActionLog通过计时轮次一次计时过程关联起来
 */
public class CountingRound implements CustomInstantiationSnapshotable {
    private final List<ActionLog> actionLogList = new LinkedList<>();
    private final Logger logger = LoggerFactory.getLogger(this);
    private final Date startDate, endDate;
    private final Time resetTime, countedTime;
    private boolean isFinished;

    /**
     * 实例化方法仅应在计时开始时调用，记录计时开始的日期
     */
    public CountingRound(Time resetTime) {
        this();
        this.startDate.copyFrom(DateTimeUtil.getCurrentDate());
        this.resetTime.copyFrom(resetTime);
        TimerStatistic.getInstance().reportCountingRoundStart(this);
    }

    private CountingRound() {
        this.startDate = new Date(0,0,0);
        this.endDate = new Date(0,0,0);
        this.resetTime = new Time(0,0,0);
        this.countedTime = new Time(0,0,0);
        this.isFinished = false;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void addActionLog(ActionLog actionLog) {
        if(!isFinished) {
            actionLogList.add(actionLog);
        } else {
            logger.logWarning("CountingRound finished, no action logs will be added.");
        }
    }

    public void complete() {
        complete(resetTime);
    }

    public void complete(Time countedTime) {
        if(!isFinished) {
            this.isFinished = true;
            this.endDate.copyFrom(DateTimeUtil.getCurrentDate());
            this.countedTime.copyFrom(countedTime);
            TimerStatistic.getInstance().reportCountingRoundEnd(this);
        } else {
            logger.logWarning("CountingRound already finished!");
        }
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Time getResetTime() {
        return resetTime;
    }

    public Time getCountedTime() {
        return countedTime;
    }

    @Override
    public byte[] writeToSnapshot() {
        SnapshotWriter writer = SnapshotWriter.writer(new Class[]{ActionLog.class});
        writer.writeClassTable();
        writer.writeBoolean(isFinished)
                .writeInt(startDate.getYear()).writeInt(startDate.getMonth()).writeInt(startDate.getDay())
                .writeInt(endDate.getYear()).writeInt(endDate.getMonth()).writeInt(endDate.getDay())
                .writeInt(resetTime.getHour()).writeInt(resetTime.getMinute()).writeInt(resetTime.getSecond())
                .writeInt(countedTime.getHour()).writeInt(countedTime.getMinute()).writeInt(countedTime.getSecond())
                .writeInt(actionLogList.size());
        Iterator<ActionLog> actionLogIterator = actionLogList.iterator();
        while(actionLogIterator.hasNext()) {
            writer.writeSnapshotableObject(actionLogIterator.next());
        }
        return writer.toByteArray();
    }

    @Override
    public void restoreFromSnapshot(byte[] bytes) {
        SnapshotReader reader = SnapshotReader.reader(bytes);
        reader.readClassTable();
        this.isFinished = reader.readBoolean();
        startDate.setAllField(reader.readInt(), reader.readInt(), reader.readInt());
        endDate.setAllField(reader.readInt(), reader.readInt(), reader.readInt());
        resetTime.setAllField(reader.readInt(), reader.readInt(), reader.readInt());
        countedTime.setAllField(reader.readInt(), reader.readInt(), reader.readInt());
        int actionLogSize = reader.readInt();
        while(actionLogSize-->0) {
            Snapshotable object = reader.readSnapshotableObject();
            actionLogList.add((ActionLog) object);
        }
    }

    private static CountingRound getSnapshotInstance() {
        return new CountingRound();
    }

    @Override
    public String instantiationMethodName() {
        return "getSnapshotInstance";
    }
}
