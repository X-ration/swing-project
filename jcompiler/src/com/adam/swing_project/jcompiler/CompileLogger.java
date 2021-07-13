package com.adam.swing_project.jcompiler;

import com.adam.swing_project.jcompiler.assertion.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class CompileLogger {

    private List<CompileLoggerListener> compileLoggerListeners = new ArrayList<>();

    protected abstract String convertToLog(File file);

    public void logCompile(File file) {
        triggerListeners(convertToLog(file));
    }

    public void logCompile(String log) {
        triggerListeners(log);
    }

    private void triggerListeners(String log) {
        for(CompileLoggerListener listener: compileLoggerListeners) {
            listener.logCompile(log);
        }
    }

    public void addCompileLoggerListener(CompileLoggerListener listener) {
        Assert.notNull(listener);
        this.compileLoggerListeners.add(listener);
    }

}
