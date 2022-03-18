package com.adam.swing_project.library.logger;

import com.adam.swing_project.library.assertion.Assert;

public class CombinedLogger extends Logger{
    private final Logger[] internalLoggers;

    public CombinedLogger(Object object, Logger... loggers) {
        super(object);
        Assert.notNull(loggers);
        internalLoggers = loggers;
    }

    @Override
    protected void doLog(String msg) {
        for(Logger logger: internalLoggers) {
            logger.doLog(msg);
        }
    }
}
