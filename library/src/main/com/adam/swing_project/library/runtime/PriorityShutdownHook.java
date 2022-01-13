package com.adam.swing_project.library.runtime;

public class PriorityShutdownHook{

    protected int priority;
    protected String name = "unnamed";
    protected Runnable runnable;

    /**
     * 优先级，数值大者先执行
     * @param priority
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    protected void run() {
        runnable.run();
    }
}
