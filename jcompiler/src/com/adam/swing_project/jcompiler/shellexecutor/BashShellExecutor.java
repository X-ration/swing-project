package com.adam.swing_project.jcompiler.shellexecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BashShellExecutor extends ShellExecutor {
    public BashShellExecutor() {
        this("UTF-8", "Executing");
    }

    public BashShellExecutor(String charset, String identifierPrefix) {
        super(charset, identifierPrefix, "bash");
    }

    public static void main(String[] args) {
        BashShellExecutor bashCommandExecutor = new BashShellExecutor();
        List<CommandInput> commandInputs = Arrays.asList(
                new CommandInput("pwd", "pwd"),
                new CommandInput("ls tmp", "ls tmp"),
                new CommandInput("whoami", "whoami"),
                new CommandInput("sleep 1s", "sleep")
        );
        List<CommandInput> realInputs = new ArrayList<>(commandInputs);
        for(int i=0;i<5;i++) {
            realInputs.addAll(commandInputs);
        }
        bashCommandExecutor.submitAsync(realInputs, 5);
        int i=0;
        try {
            while (!bashCommandExecutor.finished()) {
                List<CommandOutput> commandOutputs = bashCommandExecutor.getResults();
                System.out.println("getResult" + i++ +"[" + commandOutputs.size() + "]:");
                for (CommandOutput output : commandOutputs) {
                    System.out.println(output.isSuccess() + " " + output.getMsg());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        bashCommandExecutor.reset();

        List<CommandOutput> commandOutputs = bashCommandExecutor.exec(realInputs);
        System.out.println("exec getResult[" + commandOutputs.size()+"]");
        for (CommandOutput output : commandOutputs) {
            System.out.println(output.isSuccess() + " " + output.getMsg());
        }
        bashCommandExecutor.cleanup();
    }

}
