package com.adam.swing_project.timer.timer;

import com.adam.swing_project.library.datetime.Date;
import com.adam.swing_project.library.logger.Logger;
import com.adam.swing_project.library.logger.LoggerFactory;
import com.adam.swing_project.library.snapshot.*;

import java.io.File;
import java.util.*;

/**
 * ActionLog全局控制器，单例模式
 * 该类保存的ActionLog会不断增多，可能占用内存
 */
public class ActionLogManager //implements CustomInstantiationSnapshotable
{

    private static final ActionLogManager instance = new ActionLogManager();
    private final Logger logger = LoggerFactory.getLogger(this);
    private final Deque<ActionLog> actionLogQueue = new LinkedList<>();
    private final Collection<Date> actionLogDateCollection = new LinkedList<>();

    private ActionLogManager() {
//        SnapshotManager.getInstance().registerSnapshotable(this);
    }

    public void addActionLog(ActionLog actionLog) {
        logger.logDebug("Added action log timerId=" + actionLog.getTimerId());
        synchronized (actionLogQueue) {
            addActionLogInternal(actionLog);
        }
    }

    public Iterator<Date> getActionLogDateIterator() {
        return actionLogDateCollection.iterator();
    }

    /**
     * @param reverse true-倒序排列（由近及远） false-正序排列（由远及近）
     * @return
     */
    public List<ActionLog> getActionLogListByDate(Date date, boolean reverse) {
        Iterator<ActionLog> actionLogIterator = reverse ?
                actionLogQueue.descendingIterator() : actionLogQueue.iterator();
        List<ActionLog> result = new LinkedList<>();
        boolean foundDate = false;
        while(actionLogIterator.hasNext()) {
            ActionLog actionLog = actionLogIterator.next();
            if(actionLog.getDate().equals(date)) {
                foundDate = true;
                result.add(actionLog);
            } else if(foundDate) {
                break;
            }
        }
        return result;
    }

    /**
     * 重新映射ActionLog的timerId值，只在初始化阶段调用
     * @param timerIdMap
     */
    //todo 此方法似乎没有必要
    public void remapTimerIds(Map<Integer, Integer> timerIdMap) {
        int queueSize = actionLogQueue.size();
        while(queueSize-->0) {
            ActionLog actionLog = actionLogQueue.poll();
            int oldId = actionLog.getTimerId();
            int remapId = timerIdMap.getOrDefault(oldId, -1);
            actionLog.setTimerId(remapId);
            logger.logDebug("Remapped ActionLog id: " + oldId + "->" + remapId);
            actionLogQueue.offer(actionLog);
        }
    }

    private void addActionLogInternal(ActionLog actionLog) {
        actionLogQueue.addLast(actionLog);
        Date date = actionLog.getDate();
        if(!actionLogDateCollection.contains(date)) {
            actionLogDateCollection.add(date);
        }
    }

    public static ActionLogManager getInstance() {
        return instance;
    }

//    @Override
    public String instantiationMethodName() {
        return "getInstance";
    }

//    @Override
    public byte[] writeToSnapshot() {
        SnapshotWriter writer = SnapshotWriter.writer(new Class[]{ActionLog.class});
        writer.writeClassTable();
        writer.writeInt(actionLogQueue.size());
        Iterator<ActionLog> actionLogIterator = actionLogQueue.iterator();
        int counter = 0;
        while(actionLogIterator.hasNext()) {
            counter++;
            writer.writeSnapshotableObject(actionLogIterator.next());
        }
        logger.logDebug("wrote " + counter + " action logs");
        return writer.toByteArray();
    }

//    @Override
    public void restoreFromSnapshot(byte[] bytes) {
        SnapshotReader reader = SnapshotReader.reader(bytes);
        reader.readClassTable();
        int queueSize = reader.readInt();
        while(queueSize-->0) {
            Snapshotable object = reader.readSnapshotableObject();
            addActionLogInternal((ActionLog) object);
        }
        logger.logDebug("restored " + actionLogQueue.size() + " action logs");
    }

    public static void main(String[] args) {
        File snapshotDir = new File("C:\\Users\\Adam\\swing-timer\\snapshot-dev");
        SnapshotManager.getInstance().setSnapshotDir(snapshotDir);
        SnapshotManager.getInstance().generateSnapshot();
        List<Snapshotable> snapshotableList = SnapshotManager.getInstance().readLastSnapshot();
        snapshotableList.forEach(System.out::println);
    }
}
