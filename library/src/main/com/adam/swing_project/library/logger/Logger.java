package com.adam.swing_project.library.logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日志类
 */
public abstract class Logger {

    private final Object object;
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static LogLevel globalLogLevel;
    private LogLevel logLevel;

    public enum LogLevel {
        DEBUG, INFO, WARNING, ERROR
    }

    protected Logger(Object object) {
        this.object = object;
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
        doLog(formatLogMsg(msg, logLevel));
    }

    protected String formatLogMsg(String msg, LogLevel logLevel) {
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
        return sb.toString();
    }

    protected abstract void doLog(String msg);

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
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        doLog(stringWriter.toString());
    }

    public void logException(Exception e, String msg) {
        logError(msg);
        logException(e);
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
        Logger logger1 = ConsoleLogger.createLogger(object1), logger2 = ConsoleLogger.createLogger(object2);
        logger1.logDebug("AAAAA");
        logger2.logDebug("BBBBB");
        logger1.logInfo("AAAAA");
    }

}
