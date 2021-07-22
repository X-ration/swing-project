package com.adam.swing_project.jcompiler.shellexecutor;

/**
 * 用于封装执行结果
 */
public class CommandOutput {

    /**
     * 命令行输入
     */
    private CommandInput sourceInput;

    /**
     * 是否成功
     */
    private boolean isSuccess;

    /**
     * 提示信息
     */
    private String msg;

    private StringBuilder stringBuilder = new StringBuilder();

    public CommandInput getSourceInput() {
        return sourceInput;
    }

    public void setSourceInput(CommandInput sourceInput) {
        this.sourceInput = sourceInput;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void appendMsg(String msg) {
        stringBuilder.append(msg);
    }

    public void joinMsg() {
        setMsg(stringBuilder.toString());
    }
}
