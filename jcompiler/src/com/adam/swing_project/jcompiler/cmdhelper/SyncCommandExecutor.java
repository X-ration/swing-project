package com.adam.swing_project.jcompiler.cmdhelper;

import java.util.List;

/**
 * 可同步获取结果的命令行执行器
 */
public interface SyncCommandExecutor {

    CommandOutput exec(CommandInput commandInput);

    List<CommandOutput> exec(List<CommandInput> commandInputs);

}
