package com.adam.swing_project.jcompiler.shellexecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CmdShellExecutor extends ShellExecutor {

    public CmdShellExecutor() {
        this("GBK", "Executing");
    }

    public CmdShellExecutor(String charset, String identifierPrefix) {
        super(charset, identifierPrefix, "cmd");
    }

    @Override
    protected List<String> convertCommands(List<CommandInput> commandInputs) {
        List<String> commandStrings = new ArrayList<>();
        commandStrings.add("@echo off");  //关闭回显
        for(CommandInput commandInput: commandInputs) {
            commandStrings.add("echo " + identifierPrefix + " " + commandInput.getIdentifier());
            commandStrings.add(commandInput.getCommand());
        }
        return commandStrings;
    }

    public static void main(String[] args) {
        CmdShellExecutor cmdShellExecutor = new CmdShellExecutor();
        List<CommandInput> commandInputs = Arrays.asList(
                new CommandInput("pwd", "pwd"),
                new CommandInput("ls tmp", "ls tmp"),
                new CommandInput("whoami", "whoami"),
                new CommandInput("javac Main.java", "javac")
        );
        List<CommandInput> realInputs = new ArrayList<>(commandInputs);
        for(int i=0;i<5;i++) {
            realInputs.addAll(commandInputs);
        }
        cmdShellExecutor.submitAsync(realInputs, 5);
        int i=0;
        try {
            while (!cmdShellExecutor.finished()) {
                List<CommandOutput> commandOutputs = cmdShellExecutor.getResults();
                System.out.println("async getResult" + i++ +"[" + commandOutputs.size() + "]:");
                for (CommandOutput output : commandOutputs) {
                    System.out.println(output.isSuccess() + " " + output.getMsg());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }



        cmdShellExecutor.reset();

        List<CommandOutput> commandOutputs = cmdShellExecutor.exec(realInputs);
        System.out.println("sync getResult[" + commandOutputs.size()+"]");
        for (CommandOutput output : commandOutputs) {
            System.out.println(output.isSuccess() + " " + output.getMsg());
        }
        cmdShellExecutor.cleanup();
    }




}
