package com.adam.swing_project.library.logger;

public class ConsoleLogger extends Logger{

    private ConsoleLogger(Object object) {
        super(object);
    }

    public static ConsoleLogger createLogger(Object object) {
        return new ConsoleLogger(object);
    }

    @Override
    protected void doLog(String msg) {
        System.out.println(msg);
    }
}
