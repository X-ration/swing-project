package com.adam.swing_project.jcompiler.internal_compiler;

import com.adam.swing_project.jcompiler.assertion.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 编译Logger，可以log File参数也可以直接log自定义字符串
 */
public abstract class CompileLogger {

    private List<CompileLoggerListener> compileLoggerListeners = new ArrayList<>();

    /**
     * 子类自定义由File类型转化为字符串log
     * @param file
     * @return
     */
    protected abstract String convertToLog(File file);

    /**
     * Log File
     * @param file 当前编译文件
     */
    public void logCompile(File file) {
        triggerListeners(convertToLog(file));
    }

    /**
     * Log String
     * @param log 自定义字符串
     */
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
