package com.adam.swing_project.timer;

import com.adam.swing_project.timer.thread.ThreadManager;
import com.adam.swing_project.library.timer.TimerThread;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestTimerThread {
    public static void main(String[] args) {
        test1();
    }
    private static void test1() {
        TimerThread timerThread = ThreadManager.getInstance().getTimerThread();
        timerThread.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        TimerThread.TimerTask task1 = timerThread.new TimerTask(500, TimeUnit.MILLISECONDS, () -> System.out.println("AAAAA"));
        long currentTime = System.currentTimeMillis();
        task1.setLoopTask(true, currentTime + 5000);
        TimerThread.TimerTask task2 = timerThread.new TimerTask(1000, TimeUnit.MILLISECONDS, () -> System.out.println("BBBBB"));
        task2.setLoopTask(true, currentTime + 10000);
        timerThread.registerTask(task1);
        timerThread.registerTask(task2);
    }
    private static void test2() {
        Thread thread = new Thread(()->{
            int i=0;
            try {
                while (true) {
                    i++;
                    if(i%1000000 == 0) {
                        System.out.println(Thread.interrupted());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Terminated");
        });
        thread.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        thread.interrupt();
    }
    private static void test3() {
        List<Integer> list = Collections.synchronizedList(new LinkedList<>());
        list.add(1);
        list.add(2);
        list.add(3);
        for(Integer integer: list) {
            if(integer == 2) {
                list.remove(integer);
            }
        }
    }
}
