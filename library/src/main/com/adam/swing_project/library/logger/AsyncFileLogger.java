package com.adam.swing_project.library.logger;

import com.adam.swing_project.library.runtime.ManagedShutdownHook;
import com.adam.swing_project.library.runtime.PriorityShutdownHook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class AsyncFileLogger extends Logger implements EarlyExposed{

    protected static class FileLog {
        String logMsg;
        File logFile;
        FileLog(String logMsg, File logFile) {
            this.logMsg = logMsg;
            this.logFile = logFile;
        }
    }

    private static class SynchronizedQueue<E> extends LinkedList<E> {
        private final Object lock = new Object();

        @Override
        public boolean add(E e) {
            synchronized (lock) {
                return super.add(e);
            }
        }

        @Override
        public E poll() {
            synchronized (lock) {
                return super.poll();
            }
        }

        @Override
        public E peek() {
            synchronized (lock) {
                return super.peek();
            }
        }

        @Override
        public boolean isEmpty() {
            synchronized (lock) {
                return super.isEmpty();
            }
        }

        @Override
        public int size() {
            synchronized (lock) {
                return super.size();
            }
        }
    }

    static class LoggerThread extends Thread {

        private final Queue<FileLog> fileLogQueue = new SynchronizedQueue<>(),
                workQueue = new LinkedList<>();
        private boolean terminateSign;
        private boolean flushSign;
        private final Object notEmptyLock = new Object(), notFullLock = new Object(), flushLock = new Object();
        private final int work_queue_threshold = 32, log_queue_threshold = 1024;
        private final Logger logger = LoggerFactory.getLogger(this);

        @Override
        public void run() {
            try {
                while (!terminateSign) {
                    synchronized (notEmptyLock) {
                        if (fileLogQueue.isEmpty()) {
                            try {
                                notEmptyLock.wait();
                            } catch (InterruptedException e) {
//                                e.printStackTrace();
                            }
                        }
                    }
                    synchronized (notFullLock) {
                        FileLog fileLog;
                        while ((fileLog = fileLogQueue.poll()) != null) {
                            workQueue.add(fileLog);
                            notFullLock.notify();
                        }
                    }
                    //workQueue 不需要一起同步，写入文件期间其他线程可继续写入fileLogQueue直到队列满阻塞等待。
                    //但应该尽量避免fileLogQueue阻塞的情况
                    if (flushSign || workQueue.size() >= work_queue_threshold) {
                        logger.logInfo("doLogFile(" + workQueue.size() + ")");
                        doLogFile(workQueue);
                        workQueue.clear();
                        if(flushSign) {
                            synchronized (flushLock) {
                                flushSign = false;
                                flushLock.notifyAll();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.logException(e, "AsyncLogThread is terminating with exception");
            } finally {
                logger.logInfo(("AsyncLogThread is terminating"));
                FileLog fileLog;
                while ((fileLog = fileLogQueue.poll()) != null) {
                    workQueue.add(fileLog);
                }
                if(!workQueue.isEmpty()) {
                    try {
                        logger.logInfo("doLogFile(" + workQueue.size() + ")");
                        if(!fileLogQueue.isEmpty()) {
                            while ((fileLog = fileLogQueue.poll()) != null) {
                                workQueue.add(fileLog);
                            }
                        }
                        doLogFile(workQueue);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    workQueue.clear();
                }
            }
        }

        void enqueueLog(FileLog fileLog) {
            synchronized (notFullLock) {
                if(fileLogQueue.size() == log_queue_threshold) {
                    try {
                        notFullLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            fileLogQueue.add(fileLog);
            synchronized (notEmptyLock) {
                notEmptyLock.notify();
            }
        }

        void terminate() {
            terminateSign = true;
            this.interrupt();
        }

        //预期：调用过flush可将投入队列的日志全部缓冲到文件 实际：只是缓冲了workQueue，而logQueue还有后续进来的没有缓冲完
        //剩余的日志会在shutdown hook中完成
        void flush() {
            synchronized (flushLock) {
                flushSign = true;
                this.interrupt();
                try {
                    flushLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void doLogFile(Queue<FileLog> workQueue) throws IOException {
            Map<File, List<String>> map = new HashMap<>();
            FileLog fileLog;
            while((fileLog = workQueue.poll()) != null) {
                List<String> list = map.get(fileLog.logFile);
                if(list == null) {
                    list = new LinkedList<>();
                    map.put(fileLog.logFile, list);
                }
                list.add(fileLog.logMsg);
            }
            for(Map.Entry<File, List<String>> entry: map.entrySet()) {
                File file = entry.getKey();
                if(!file.exists()) {
                    file.createNewFile();
                }
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(entry.getKey(), StandardCharsets.UTF_8,true));
                for(String str: entry.getValue()) {
                    bufferedWriter.write(str);
                    bufferedWriter.write(System.lineSeparator());
                }
                bufferedWriter.close();
            }
        }
    }

    private static LoggerThread LOGGER_THREAD;
    protected final File logFile;

    protected AsyncFileLogger(Object object, File logFile) {
        super(object);
        this.logFile = logFile;
    }

    @Override
    public void postConstruct() {
        if(LOGGER_THREAD == null) {
            synchronized (this) {
                if(LOGGER_THREAD == null) {
                    LOGGER_THREAD = new LoggerThread();
                    LOGGER_THREAD.setName("AsyncFileLogThread");
                    LOGGER_THREAD.start();
                    PriorityShutdownHook shutdownHook = new PriorityShutdownHook();
                    shutdownHook.setName("AsyncLogThread shutdown hook");
                    shutdownHook.setRunnable(()->{
                        LOGGER_THREAD.terminate();
                        try {
                            LOGGER_THREAD.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                    shutdownHook.setPriority(Integer.MIN_VALUE);
                    ManagedShutdownHook.getInstance().registerShutdownHook(shutdownHook);
                }
                doLog("");
                doLog("--------------------------------------------");
            }
        }
    }

    public static AsyncFileLogger createLogger(Object object, File logFile) {
        return new AsyncFileLogger(object, logFile);
    }

    @Override
    protected void doLog(String msg) {
        FileLog fileLog = new FileLog(msg, logFile);
        enqueueLog(fileLog);
    }

    protected void enqueueLog(FileLog fileLog) {
        LOGGER_THREAD.enqueueLog(fileLog);
    }

    public void flushRequired() {
        LOGGER_THREAD.flush();
    }

    public static void main(String[] args) {
        File logFile = new File("test.log");
        AsyncFileLogger logger1 = new AsyncFileLogger(new Object(), logFile),
                logger2 = new AsyncFileLogger(new Object(), logFile),
                logger3 = new AsyncFileLogger(new Object(), logFile);
        String msg = "this is a log msg.";
        for(int i=0;i<100;i++) {
            logger1.logInfo(msg + i);
            logger2.logInfo(msg + i);
            logger3.logInfo(msg + i);
        }
        System.exit(0);
    }
}
