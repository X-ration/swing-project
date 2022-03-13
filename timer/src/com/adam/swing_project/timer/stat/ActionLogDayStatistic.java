package com.adam.swing_project.timer.stat;

import com.adam.swing_project.library.assertion.Assert;
import com.adam.swing_project.library.datetime.Date;
import com.adam.swing_project.library.datetime.Time;
import com.adam.swing_project.library.logger.Logger;
import com.adam.swing_project.library.logger.LoggerFactory;
import com.adam.swing_project.library.util.DateTimeUtil;

import java.util.Collection;

public class ActionLogDayStatistic {

    private final Logger logger = LoggerFactory.getLogger(this);
    private final Date date;
    private final Time totalResetTime, totalCountedTime;

    public ActionLogDayStatistic(Date date, Time totalResetTime, Time totalCountedTime) {
        this.date = date;
        this.totalResetTime = new Time(0,0,0);
        this.totalCountedTime = new Time(0,0,0);
        this.totalResetTime.copyFrom(totalResetTime);
        this.totalCountedTime.copyFrom(totalCountedTime);
    }

    public ActionLogDayStatistic(Date date, Collection<ActionLog> actionLogs) {
        Assert.notNull(date);
        Assert.notNull(actionLogs);
        this.date = date;
        this.totalResetTime = new Time(0,0,0);
        this.totalCountedTime = new Time(0,0,0);
        for(ActionLog actionLog: actionLogs) {
            if(!date.equals(actionLog.getDate())) {
                logger.logWarning("ActionLog " + actionLog + " invalid date '" + actionLog.getDate() + "', expected '" + date + "'");
                continue;
            }
            ActionLog.ActionLogType actionLogType = actionLog.getActionLogType();
            switch (actionLogType) {
                case TIMER_STOP:
                    DateTimeUtil.timeSelfPlusTime(totalResetTime, actionLog.getResetTime());
                    DateTimeUtil.timeSelfPlusTime(totalCountedTime, DateTimeUtil.timeMinusTime(actionLog.getResetTime(), actionLog.getCountingTime()));
                    break;
                case TIMER_TIME_UP:
                    DateTimeUtil.timeSelfPlusTime(totalResetTime, actionLog.getResetTime());
                    DateTimeUtil.timeSelfPlusTime(totalCountedTime, actionLog.getResetTime());
                    break;
            }
        }
    }

    public Date getDate() {
        return date;
    }

    public Time getTotalResetTime() {
        return totalResetTime;
    }

    public Time getTotalCountedTime() {
        return totalCountedTime;
    }
}
