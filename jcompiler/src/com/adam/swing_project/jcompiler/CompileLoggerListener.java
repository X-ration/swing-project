package com.adam.swing_project.jcompiler;

/**
 * CompilerLogger监听器，用于Swing组件针对log字符串进行展示
 */
public interface CompileLoggerListener {
    void logCompile(String compileLog);
}
