package com.adam.swing_project.jcompiler.cmdhelper;

/**
 * 用于封装命令行命令
 */
public class CommandInput {

    /**
     * 实际执行的命令
     */
    private String command;

    /**
     * 标识符，用于解析结果
     */
    private String identifier;

    public CommandInput(String command) {
        this.command = command;
        this.identifier = command;
    }

    public CommandInput(String command, String identifier) {
        this.command = command;
        this.identifier = identifier;
    }

    public String getCommand() {
        return command;
    }

    public String getIdentifier() {
        return identifier;
    }
}
