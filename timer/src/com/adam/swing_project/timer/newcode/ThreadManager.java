package com.adam.swing_project.timer.newcode;

import java.util.concurrent.*;

/**
 * 线程管理
 */
public class ThreadManager {

    private static final ThreadManager instance = new ThreadManager();
    private final AudioThread audioThread;
    private final TimerThread timerThread;
    private final ThreadPoolExecutor threadPoolExecutorForTimerThread;

    private ThreadManager(){
        audioThread = new AudioThread();
        audioThread.setName("AudioThread");
        this.threadPoolExecutorForTimerThread= new ThreadPoolExecutor(1,1,60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        timerThread = new TimerThread(threadPoolExecutorForTimerThread);
        timerThread.setName("TimerThread");
    }

    public static ThreadManager getInstance() {
        return instance;
    }

    public void initThreads() {
        audioThread.start();
        timerThread.start();
    }

    public void destroyThreads() {
        audioThread.terminate();
        timerThread.terminate();
    }

    public AudioThread getAudioThread() {
        return audioThread;
    }

    public TimerThread getTimerThread() {
        return timerThread;
    }
}
