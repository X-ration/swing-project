package com.adam.swing_project.timer.newcode;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日志类
 */
public class Logger {

    private final Object object;
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static LogLevel globalLogLevel;
    private LogLevel logLevel;

    public enum LogLevel {
        DEBUG, INFO, WARNING, ERROR
    }

    private Logger(Object object) {
        this.object = object;
    }

    public static Logger createLogger(Object object) {
        return new Logger(object);
    }

    /**
     * 初期使用的方法，DEBUG日志
     * @param msg
     */
    @Deprecated
    public void log(String msg) {
        log(msg, LogLevel.DEBUG);
    }
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
        if(this.logLevel != null && this.logLevel.compareTo(logLevel) > 0) {
            return;
        } else if(this.logLevel == null && globalLogLevel != null && globalLogLevel.compareTo(logLevel) > 0) {
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
        sb.append("[Logger of '").append(object).append("' in thread '").append(Thread.currentThread().getName()).append("'] ")
                        .append(msg);
        System.out.println(sb);
    }

    public void logException(Exception e) {
        logError("Exception occured!");
        e.printStackTrace();
    }

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    public static void setGlobalLogLevel(LogLevel globalLogLevel) {
        Logger.globalLogLevel = globalLogLevel;
    }

    private String getCurrentTimeStandardFormat() {
        Date date = new Date();
        return simpleDateFormat.format(date);
    }

    public static void main(String[] args) {
        globalLogLevel = LogLevel.INFO;
        Object object1 = new Object(), object2 = new Object();
        Logger logger1 = Logger.createLogger(object1), logger2 = Logger.createLogger(object2);
        logger1.log("AAAAA");
        logger2.log("BBBBB");
        logger1.logInfo("AAAAA");
    }

}
