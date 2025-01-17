package com.adam.swing_project.loader;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日志类
 */
public class LoaderLogger {

    private final Object object;
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static LogLevel globalLogLevel;
    private LogLevel logLevel;

    public enum LogLevel {
        DEBUG, INFO, WARNING, ERROR
    }

    private LoaderLogger(Object object) {
        this.object = object;
    }

    public static LoaderLogger createLogger(Object object) {
        return new LoaderLogger(object);
    }

    /**
     * 初期使用的方法，DEBUG日志
     * @param msg
     */
    public void logDebug(String msg) {
        log(msg, LogLevel.DEBUG);
    }
    public void logInfo(String msg) {
        log(msg, LogLevel.INFO);
    }
    public void logWarning(String msg) {
        log(msg, LogLevel.WARNING);
    }
    public void logError(String msg) {
        log(msg, LogLevel.ERROR);
    }
    public void log(String msg, LogLevel logLevel) {
        if(!levelEnabled(logLevel)) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        String logLevelString = logLevel.name();
        sb.append(logLevelString);
        for(int i=0;i<7-logLevelString.length();i++) {
            sb.append(" ");
        }
        sb.append(" ");
        sb.append(getCurrentTimeStandardFormat()).append(" ");
        String objectString = object.toString();
        String objectStringLog = objectString.substring(objectString.lastIndexOf('.') + 1);
        sb.append("[Logger of '").append(objectStringLog).append("' in thread '").append(Thread.currentThread().getName()).append("'] ")
                        .append(msg);
        System.out.println(sb);
    }

    private boolean levelEnabled(LogLevel logLevel) {
        if(this.logLevel != null && this.logLevel.compareTo(logLevel) > 0) {
            return false;
        } else if(this.logLevel == null && globalLogLevel != null && globalLogLevel.compareTo(logLevel) > 0) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 是否启用DEBUG级别
     * @return
     */
    public boolean debugEnabled() {
        return levelEnabled(LogLevel.DEBUG);
    }

    public void logException(Exception e) {
        logError("Exception occured!");
        e.printStackTrace();
    }

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    public static void setGlobalLogLevel(LogLevel globalLogLevel) {
        LoaderLogger.globalLogLevel = globalLogLevel;
    }

    private String getCurrentTimeStandardFormat() {
        Date date = new Date();
        return simpleDateFormat.format(date);
    }
}
