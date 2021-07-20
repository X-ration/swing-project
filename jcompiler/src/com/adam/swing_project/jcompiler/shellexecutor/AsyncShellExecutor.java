package com.adam.swing_project.jcompiler.shellexecutor;

import java.util.List;

/**
 * 异步获取结果的命令行执行器
 */
public interface AsyncShellExecutor {

    default <T> void submitAsync(List<CommandInput<T>> inputList) {
        submitAsync(inputList, 1);
    }

    <T> void submitAsync(List<CommandInput<T>> inputList, int batchSize);

    List<CommandOutput> getResults();

    boolean finished();

}
