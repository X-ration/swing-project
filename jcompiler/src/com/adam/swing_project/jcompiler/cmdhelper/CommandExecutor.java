package com.adam.swing_project.jcompiler.cmdhelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 命令行执行器父类
 */
public abstract class CommandExecutor {

    protected ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * 重置到初始状态，以备使用
     */
    public void reset() {
        if(this.executorService.isShutdown()) {
            this.executorService = Executors.newSingleThreadExecutor();
        }
    }

    /**
     * 清理线程池等
     */
    public void cleanup() {
        if(!this.executorService.isShutdown()) {
            this.executorService.shutdownNow();
        }
    }

}
