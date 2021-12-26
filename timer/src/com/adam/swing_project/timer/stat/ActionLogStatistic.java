package com.adam.swing_project.timer.stat;

import com.adam.swing_project.library.datetime.Date;
import com.adam.swing_project.library.timer.action_log.ActionLog;
import com.adam.swing_project.library.timer.action_log.ActionLogManager;

import java.util.*;

public class ActionLogStatistic {

    private static final ActionLogStatistic instance = new ActionLogStatistic();

    /**
     * 获取所有有数据的日期
     * @return
     */
    public List<Date> availableDates() {
        List<Date> result = new LinkedList<>();
        Iterator<Date> actionLogDateIterator = ActionLogManager.getInstance().getActionLogDateIterator();
        while(actionLogDateIterator.hasNext()) {
            result.add(actionLogDateIterator.next());
        }
        result.sort(Comparator.reverseOrder());
        return result;
    }

    public List<ActionLog> getDetailedActionLogByDate(Date date) {
        return ActionLogManager.getInstance().getActionLogListByDate(date, false);
    }

    public ActionLogDayStatistic getDayStatistic(Date date) {
        return new ActionLogDayStatistic(date, getDetailedActionLogByDate(date));
    }

    public static ActionLogStatistic getInstance() {
        return instance;
    }
}
