package com.adam.swing_project.library.runtime;

import com.adam.swing_project.library.logger.ConsoleLogger;
import com.adam.swing_project.library.logger.Logger;
import com.adam.swing_project.library.logger.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Java通过Runtime.addShutdownHook可以在程序关闭的时候做清理工作，然而方法的参数是线程，程序关闭时是并发乱序执行的
 * 此类允许关闭时按照优先级执行
 */
public class ManagedShutdownHook extends Thread {

    private static ManagedShutdownHook instance;
    private final List<PriorityShutdownHook> priorityShutdownHooks = Collections.synchronizedList(new LinkedList<>());
    private final Logger logger = LoggerFactory.getLogger(this);
    private ManagedShutdownHook() {
        Runtime.getRuntime().addShutdownHook(this);
    }

    public static ManagedShutdownHook getInstance() {
        if(instance == null) {
            synchronized (ManagedShutdownHook.class) {
                if (instance == null) {
                    instance = new ManagedShutdownHook();
                }
            }
        }
        return instance;
    }

    public void registerShutdownHook(PriorityShutdownHook shutdownHook) {
        this.priorityShutdownHooks.add(shutdownHook);
    }

    @Override
    public void run() {
        priorityShutdownHooks.sort((p1,p2)->Integer.compare(p2.getPriority(),p1.getPriority()));
        for(PriorityShutdownHook shutdownHook: priorityShutdownHooks) {
            if(logger.debugEnabled()) {
                logger.logDebug("running shutdown hook '" + shutdownHook.getName() + "'");
            }
            shutdownHook.run();
            if(logger.debugEnabled()) {
                logger.logDebug("complete shutdown hook '" + shutdownHook.getName() + "'");
            }
        }
    }

    public static void main(String[] args) {
        ManagedShutdownHook managedShutdownHook = ManagedShutdownHook.getInstance();
        PriorityShutdownHook psh = new PriorityShutdownHook() {
            @Override
            public void run() {
                System.out.println(getPriority() + " run");
            }
        };
        psh.setPriority(1);
        managedShutdownHook.registerShutdownHook(psh);
        psh = new PriorityShutdownHook() {
            @Override
            public void run() {
                System.out.println(getPriority() + " run");
            }
        };
        psh.setPriority(2);
        managedShutdownHook.registerShutdownHook(psh);
        psh = new PriorityShutdownHook() {
            @Override
            public void run() {
                System.out.println(getPriority() + " run");
            }
        };
        psh.setPriority(3);
        managedShutdownHook.registerShutdownHook(psh);
    }
}
