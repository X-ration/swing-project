package com.adam.swing_project.timer.thread;

import com.adam.swing_project.library.timer.newcode.NewTimerThread;

/**
 * 线程管理
 */
//todo 或许可以用shutdownhook替代该类
public class ThreadManager {

    private static final ThreadManager instance = new ThreadManager();
    private final AudioThread audioThread;
    private final NewTimerThread timerThread;

    private ThreadManager(){
        audioThread = new AudioThread();
        audioThread.setName("AudioThread");
        timerThread = new NewTimerThread();
        timerThread.setName("NewTimerThread");
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

    public NewTimerThread getTimerThread() {
        return timerThread;
    }
}
