package com.adam.swing_project.timer.timer;

import com.adam.swing_project.library.logger.Logger;
import com.adam.swing_project.library.logger.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimerIdManager {

    private static final TimerIdManager instance = new TimerIdManager();
    private final Logger logger = LoggerFactory.getLogger(this);

    private int timerCount;
    private int maxTimerId;

    private TimerIdManager() {
    }

    /**
     * 新加入计时器，计时器总数加1
     * @return 新加计时器的id
     */
    public int reportNewTimer() {
        timerCount++;
        return ++maxTimerId;
    }

    /**
     * 销毁计时器，计时器总数减1
     */
    public void reportDestroyTimer() {
        timerCount--;
    }

    /**
     * 重新整理从快照恢复的计时器id，此方法只在初始化阶段调用
     * @return 计时器旧id到新id的映射
     */
    public Map<Integer, Integer> reCalcTimerIds(List<ExtendedTimer> timerList) {
        this.timerCount = timerList.size();
        Map<Integer, Integer> idMap = new HashMap<>();
        for(ExtendedTimer timer: timerList) {
            int oldId = timer.getTimerId(), newId = ++maxTimerId;
            timer.setTimerId(newId);
            idMap.put(oldId, newId);
            logger.logDebug("Remapped ExtendedTimer id: " + oldId + "->" + newId);
        }
        return idMap;
    }

    public static TimerIdManager getInstance() {
        return instance;
    }

    public int getTimerCount() {
        return timerCount;
    }

    public int getMaxTimerId() {
        return maxTimerId;
    }
}
