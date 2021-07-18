package com.adam.swing_project.jcompiler.cmdhelper;

import java.util.List;

/**
 * 异步获取结果的命令行执行器
 */
public interface AsyncCommandExecutor {

    default void submit(List<CommandInput> inputList) {
        submit(inputList, 1);
    }

    void submit(List<CommandInput> inputList, int batchSize);

    List<CommandOutput> getResults();

    boolean finished();

}
