package com.adam.swing_project.jcompiler.shellexecutor;

import com.adam.swing_project.jcompiler.assertion.Assert;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 命令行执行器父类
 */
public abstract class ShellExecutor implements  AsyncShellExecutor, SyncShellExecutor{

    protected String charset, identifierPrefix, processName;
    private int batchSize, readIndex;
    private volatile int processingIndex;
    private volatile boolean terminated = true;
    private final Object readingLock = new Object();
    private List<CommandInput> commandInputs;
    private List<CommandOutput> commandOutputs;
    private String scriptPath = "tmp/command.bat";
    private File scriptFile = null;
    private String[] scriptCommandArgs;

    public ShellExecutor(String charset, String identifierPrefix, String processName) {
        this.charset = charset;
        this.identifierPrefix = identifierPrefix;
        this.processName = (processName == null) ? "ShellExecutor" : processName;
        if(this instanceof CmdShellExecutor) {
            this.scriptPath = "tmp" + File.separator + "command.bat";
            this.scriptCommandArgs = new String[]{this.scriptPath};
        } else if(this instanceof BashShellExecutor) {
            this.scriptPath = "tmp" + File.separator + "command.sh";
            this.scriptCommandArgs = new String[]{"bash",this.scriptPath};
        } else {
            throw new ShellExecutorException("unknown type");
        }
    }

    public static ShellExecutor systemShellExecutor() {
        String osName = System.getProperty("os.name");
        if(osName.startsWith("Windows")) {
            return new CmdShellExecutor();
        } else if(osName.startsWith("Linux")) {
            return new BashShellExecutor();
        } else {
            throw new ShellExecutorException("unknown system");
        }
    }

    protected ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public <T> void submitAsync(List<CommandInput<T>> inputList, int batchSize) {
        submitInternal(inputList, batchSize, false);
    }

    @Override
    public <T> List<CommandOutput> exec(List<CommandInput<T>> commandInputs) {
        submitInternal(commandInputs, commandInputs.size(), true);
        List<CommandOutput> commandOutputs = new ArrayList<>();
        while(!finished()) {
            commandOutputs.addAll(getResultsInternal(true));
        }
        return commandOutputs;
    }

    private <T> void submitInternal(List<CommandInput<T>> inputList, int batchSize, boolean isSync) {
        Assert.notNull(inputList);
        Assert.isTrue(inputList.size()>0, "CommandInput list must not empty");
        Assert.isTrue(batchSize > 0, "batchSize must >0");

        if(inputList.size() > 0) {

            try {
                scriptFile = createFile(scriptPath);
                writeScriptToFile(scriptFile, inputList);
            } catch (IOException e) {
                e.printStackTrace();
                throw new ShellExecutorException("create temporary file failed");
            }

            if(!terminated) {
                throw new ShellExecutorException("shell executor is running, stop it first");
            }

            reset();
            this.commandInputs = inputList.stream().map(tCommandInput -> (CommandInput)tCommandInput).collect(Collectors.toList());
            this.commandOutputs = new ArrayList<>();
            this.batchSize = batchSize;

            this.terminated = false;

            if(isSync) {
                new ShellExecutorTask(this, true).run();
            } else {
                executorService.submit(new ShellExecutorTask(this, false));
            }
        }
    }

    private class ShellExecutorTask implements Runnable {
        private ShellExecutor shellExecutor;
        private boolean isSync;
        private CommandOutput lastOutput;
        private ShellExecutorTask(ShellExecutor shellExecutor, boolean isSync) {
            this.shellExecutor = shellExecutor;
            this.isSync = isSync;
        }
        @Override
        public void run() {
            Process cmdProcess = null;
            BufferedReader bufferedReader = null;
            try {
                cmdProcess = new ProcessBuilder()
                        .redirectInput(ProcessBuilder.Redirect.PIPE)
                        .redirectOutput(ProcessBuilder.Redirect.PIPE)
                        .redirectError(ProcessBuilder.Redirect.PIPE)
                        .redirectErrorStream(true)
                        .command(shellExecutor.scriptCommandArgs)
                        .start();
                System.out.println("["+Thread.currentThread().getName()+"]" + processName + " started with pid " + cmdProcess.pid());
                bufferedReader = new BufferedReader(new InputStreamReader(cmdProcess.getInputStream(), charset));
                String line;
                processingIndex = -1;
                while((line=bufferedReader.readLine())!=null) {
                    if(!isSync) {
                        synchronized (readingLock) {
                            processLine(line, true);
                        }
                    } else {
                        processLine(line, false);
                    }
                }
                if(lastOutput != null) {
                    lastOutput.joinMsg();
                }
                processingIndex++;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(bufferedReader!=null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {}
                }
                if(cmdProcess!=null) {
                    cmdProcess.destroy();
                }
                deleteFile(shellExecutor.scriptFile);
                terminated = true;
                if(!isSync) {
                    synchronized (readingLock) {
                        readingLock.notify();
                    }
                }
                System.out.println("["+Thread.currentThread().getName()+"]" + processName + " stopped.");
            }
        }

        private void processLine(String line, boolean lock) {
            System.out.println("["+Thread.currentThread().getName()+"]" + line);
            if(line.startsWith(identifierPrefix)) {
                processingIndex++;
                String requiredIdentifier = identifierPrefix + " " + commandInputs.get(processingIndex).getIdentifier();
                Assert.isTrue(line.equals(requiredIdentifier), ShellExecutorException.class, "identifier mismatched, consider change identifier prefix. line: '" + line + "' required: '" + requiredIdentifier + "'");
                if(lastOutput != null) {
                    lastOutput.joinMsg();
                }
                lastOutput = new CommandOutput();
                lastOutput.setSourceInput(commandInputs.get(processingIndex));
                lastOutput.setSuccess(true);
                commandOutputs.add(lastOutput);
                if(lock) {
                    readingLock.notify();
                }
            } else if(lastOutput != null){
                lastOutput.setSuccess(false);
                lastOutput.appendMsg(line);
                lastOutput.appendMsg(System.lineSeparator());
            } else {
                throw new ShellExecutorException("shell executor cannot resolve output, check identifier please. line: " + line);
            }
        }
    }

    private void waitIfNecessary(int readEndIndex, boolean isSync) {
        if(!isSync && !terminated) {
            synchronized (readingLock) {
                while (!terminated && processingIndex < readEndIndex) {
                    try {
                        readingLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        throw new ShellExecutorException("interrupted");
                    }
                }
            }
        }
    }

    @Override
    public List<CommandOutput> getResults() {
        return getResultsInternal(false);
    }

    private List<CommandOutput> getResultsInternal(boolean isSync) {
        Assert.notNull(commandOutputs);
        Assert.isTrue(!finished(), ShellExecutorException.class, "read already complete");
        int readEndIndex = Math.min(readIndex + batchSize, commandInputs.size());
        waitIfNecessary(readEndIndex, isSync);

        List<CommandOutput> resultList = new ArrayList<>();
        while(readIndex<readEndIndex) {
            Assert.isTrue(readIndex < commandOutputs.size(), ShellExecutorException.class, "read index exceeds limit, " + processName + " probably already stopped");
            resultList.add(commandOutputs.get(readIndex++));
        }
        return resultList;
    }

    @Override
    public boolean finished() {
        return readIndex >= commandInputs.size();
    }

    private File createFile(String filePath) throws IOException {
        File file = new File(filePath);
        if(!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        return file;
    }

    /**
     * 将命令对象转化为可写入脚本的行字符串，根据不同shell可自定义
     * @param commandInputs
     * @return
     */
    protected <T> List<String> convertCommands(List<CommandInput<T>> commandInputs) {
        List<String> converted = new ArrayList<>();
        for(CommandInput commandInput: commandInputs) {
            converted.add("echo '" + identifierPrefix + " " + commandInput.getIdentifier() + "'");
            converted.add(commandInput.getCommand());
        }
        return converted;
    }

    private <T> void writeScriptToFile(File file, List<CommandInput<T>> commandInputs) throws IOException {
        FileWriter fileWriter = new FileWriter(file);
        List<String> lines = convertCommands(commandInputs);
        for(String line: lines) {
            fileWriter.write(line);
            fileWriter.write(System.lineSeparator());
        }
        fileWriter.close();
    }

    private void deleteFile(File file) {
        file.delete();
    }

    /**
     * 重置到初始状态
     */
    public void reset() {
        readIndex = 0;
        processingIndex = 0;
        terminated = true;
        commandInputs = null;
        commandOutputs = null;
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
