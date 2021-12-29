package com.adam.swing_project.timer.thread;

import com.adam.swing_project.library.timer.TimerThread;

/**
 * 线程管理
 */
//todo 或许可以用shutdownhook替代该类
public class ThreadManager {

    private static final ThreadManager instance = new ThreadManager();
    private final AudioThread audioThread;

    private ThreadManager(){
        audioThread = new AudioThread();
        audioThread.setName("AudioThread");
    }

    public static ThreadManager getInstance() {
        return instance;
    }

    public void initThreads() {
        audioThread.start();
    }

    public void destroyThreads() {
        audioThread.terminate();
    }

    public AudioThread getAudioThread() {
        return audioThread;
    }

}
