package com.adam.swing_project.timer.timer;

import com.adam.swing_project.library.snapshot.SnapshotReader;
import com.adam.swing_project.library.snapshot.SnapshotWriter;
import com.adam.swing_project.library.snapshot.Snapshotable;

/**
 * 和Timer关联的事件记录，例如开始计时，停止计时等，后期可以制成表格形式展示
 */
public class ActionLog implements Snapshotable {

    public enum ActionLogType {
        TIMER_INITIALIZE, TIMER_RESET, TIMER_START, TIMER_PAUSE, TIMER_STOP, TIMER_TIME_UP, TIMER_TERMINATE
    }

    private ActionLogType actionLogType;
    private java.util.Date utilDate;

    private ActionLog() {}

    public ActionLog(ActionLogType actionLogType) {
        this(actionLogType, new java.util.Date());
    }

    public ActionLog(ActionLogType actionLogType, long timeMills) {
        this(actionLogType, new java.util.Date(timeMills));
    }

    public ActionLog(ActionLogType actionLogType, java.util.Date utilDate) {
        this.utilDate = utilDate;
        this.actionLogType = actionLogType;
    }

    @Override
    public byte[] writeToSnapshot() {
        SnapshotWriter writer = SnapshotWriter.writer();
        writer.writeInt(actionLogType.ordinal());
        writer.writeLong(utilDate.getTime());
        return writer.toByteArray();
    }

    @Override
    public void restoreFromSnapshot(byte[] bytes) {
        SnapshotReader reader = SnapshotReader.reader(bytes);
        this.actionLogType = ActionLogType.values()[reader.readInt()];
        this.utilDate = new java.util.Date(reader.readLong());
    }

    public ActionLogType getActionLogType() {
        return actionLogType;
    }

    public java.util.Date getUtilDate() {
        return utilDate;
    }

}
