package com.adam.swing_project.jcompiler.cmdhelper;

/**
 * 通用Logger接口
 */
public interface Logger {

    /**
     * 标准输出
     * @param logMsg
     */
    void logStdOut(String logMsg);

    /**
     * 标准错误输出
     * @param logMsg
     */
    void logStdErr(String logMsg);

    /**
     * 标准输入
     * @param logMsg
     */
    void logStdIn(String logMsg);

}
