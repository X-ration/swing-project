package com.adam.swing_project.jcompiler.shellexecutor.cmdhelper;

public class DefaultCmdHelperLogger implements CmdHelperLogger {
    @Override
    public void logStdOut(String logMsg) {
        System.out.print(logMsg);
    }

    @Override
    public void logStdErr(String logMsg) {
        System.err.print(logMsg);
    }

    @Override
    public void logStdIn(String logMsg) {
        System.out.print(logMsg);
    }
}
