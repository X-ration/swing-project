package com.adam.swing_project.library.logger;

import com.adam.swing_project.library.assertion.Assert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * LoggerFactory使用CombinedLogger，根据用户配置启用console/file logger。
 */
public class LoggerFactory {

    private static final List<Logger> loggerList = new LinkedList<>();
    private static volatile boolean initialized, initializing;

    /**
     * 在程序启动时调用，初始化logger
     * @param loggers
     */
    public static void setupLoggers(List<Logger> loggers) {
        loggerList.clear();
        loggerList.addAll(loggers);
    }

    public static void setupGlobalLevel(Logger.LogLevel logLevel) {
        Logger.setGlobalLogLevel(logLevel);
    }

    public static Logger getLogger(Object object) {
        initLoggers();
        return new CombinedLogger(object, loggerList.toArray(new Logger[0]));
    }

    private static void initLoggers() {
        if(!initialized) {
            synchronized (LoggerFactory.class) {
                if(!initialized && !initializing) {
                    initializing = true;
                    for(Logger logger: loggerList) {
                        if(logger instanceof EarlyExposed) {
                            ((EarlyExposed) logger).postConstruct();
                        }
                    }
                    initialized = true;
                    initializing = false;
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Logger logger = LoggerFactory.getLogger(new Object());
        File logFile = new File("test.log");
        FileWriter writer = new FileWriter(logFile);
        writer.write("");
        writer.close();

        for(int i=0;i<1000;i++) {
            logger.logInfo("log " + i);
        }
    }

}
