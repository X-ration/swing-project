package com.adam.swing_project.jcompiler.shellexecutor;

import java.util.List;

/**
 * 可同步获取结果的命令行执行器
 */
public interface SyncShellExecutor {

    List<CommandOutput> exec(List<CommandInput> commandInputs);

}
