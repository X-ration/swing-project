package com.adam.swing_project.jcompiler.shellexecutor;

/**
 * 用于封装命令行命令
 */
public class CommandInput <T>{

    /**
     * 实际执行的命令
     */
    private String command;

    /**
     * 标识符，用于解析结果
     */
    private String identifier;

    /**
     * 补充信息
     */
    private T targetObject;

    public static void main(String[] args) {
        CommandInput<String> commandInput = new CommandInput<>("command", "target");
        System.out.println(commandInput);
    }

    public CommandInput(String command) {
        this(command, command);
    }

    public CommandInput(String command, String identifier) {
        this(command, identifier, null);
    }

    public CommandInput(String command, T targetObject) {
        this(command, command, targetObject);
    }

    public CommandInput(String command, String identifier, T targetObject) {
        this.command = command;
        this.identifier = identifier;
        this.targetObject = targetObject;
    }

    public String getCommand() {
        return command;
    }

    public String getIdentifier() {
        return identifier;
    }

    public T getTargetObject() {
        return targetObject;
    }
}
